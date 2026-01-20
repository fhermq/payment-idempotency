package com.fhermq.payment.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fhermq.payment.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler for the Payment API.
 * 
 * This class demonstrates production-grade error handling best practices:
 * 
 * 1. Centralized Exception Handling
 *    - All exceptions are handled in one place
 *    - Consistent error response format across the API
 *    - Reduces code duplication
 * 
 * 2. Different Exception Types
 *    - ApiException and subclasses (PaymentException, IdempotencyException, ValidationException)
 *    - Spring validation exceptions (MethodArgumentNotValidException)
 *    - JSON processing errors (JsonProcessingException)
 *    - Generic exceptions (uncaught errors)
 * 
 * 3. Appropriate Logging
 *    - Client errors (4xx): logged at WARN level
 *    - Server errors (5xx): logged at ERROR level with stack trace
 *    - Sensitive information is NOT logged
 * 
 * 4. Error Response Quality
 *    - Meaningful error codes for client-side handling
 *    - User-friendly error messages
 *    - Correlation IDs for tracing
 *    - Optional detailed information for debugging
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    private final Environment environment;
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC = "correlation_id";
    
    @Autowired
    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * Handles ApiException and its subclasses.
     * 
     * This is the main exception handler for API errors. It converts
     * ApiException to a structured ErrorResponse with appropriate HTTP status.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request) {
        
        // Get error code and HTTP status
        ErrorCode errorCode = ex.getErrorCode();
        int httpStatus = errorCode.getHttpStatus();
        
        // Log appropriately based on error type
        if (errorCode.isClientError()) {
            log.warn("Client error [{}]: {} - {}",
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                ex.getDetailMessage());
        } else {
            log.error("Server error [{}]: {} - {}",
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                ex.getDetailMessage(),
                ex.getCause());
        }
        
        // Build error response
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(httpStatus)
            .errorCode(errorCode.getCode())
            .message(errorCode.getDefaultMessage())
            .detail(ex.getDetailMessage())
            .correlationId(getCorrelationId(request, ex.getCorrelationId()))
            .path(request.getRequestURI())
            .build();
        
        // Add trace ID in non-production environments
        if (isDevEnvironment()) {
            errorResponse.setTraceId(generateTraceId(ex));
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.valueOf(httpStatus));
    }
    
    /**
     * Handles PaymentException specifically.
     * Can be used to add payment-specific logic if needed.
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(
            PaymentException ex,
            HttpServletRequest request) {
        return handleApiException(ex, request);
    }
    
    /**
     * Handles IdempotencyException specifically.
     * Can be used to add idempotency-specific logic if needed.
     */
    @ExceptionHandler(IdempotencyException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyException(
            IdempotencyException ex,
            HttpServletRequest request) {
        return handleApiException(ex, request);
    }
    
    /**
     * Handles validation errors from Spring's @Valid annotation.
     * 
     * Extracts field-level validation errors and returns them in the response.
     * This allows clients to provide better user feedback about what's wrong.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        // Extract field-level errors
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        log.warn("Validation error: {} fields invalid", fieldErrors.size());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .errorCode(ErrorCode.VALIDATION_INVALID_BODY.getCode())
            .message("Validation failed")
            .detail("Please check the field errors for details")
            .fieldErrors(fieldErrors)
            .correlationId(getCorrelationId(request, null))
            .path(request.getRequestURI())
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles missing required request headers.
     * 
     * Useful for detecting missing headers like Idempotency-Key.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(
            MissingRequestHeaderException ex,
            HttpServletRequest request) {
        
        String headerName = ex.getHeaderName();
        log.warn("Missing required header: {}", headerName);
        
        // Determine if it's an idempotency key
        ErrorCode errorCode = "Idempotency-Key".equals(headerName)
            ? ErrorCode.IDEMPOTENCY_KEY_MISSING
            : ErrorCode.VALIDATION_MISSING_HEADER;
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .errorCode(errorCode.getCode())
            .message(errorCode.getDefaultMessage())
            .detail("Missing header: " + headerName)
            .correlationId(getCorrelationId(request, null))
            .path(request.getRequestURI())
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles JSON processing errors.
     * 
     * Occurs when request body cannot be parsed as JSON or
     * response cannot be serialized to JSON.
     */
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ErrorResponse> handleJsonProcessingException(
            JsonProcessingException ex,
            HttpServletRequest request) {
        
        log.error("JSON processing error", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .errorCode(ErrorCode.SYSTEM_JSON_PROCESSING.getCode())
            .message("Invalid JSON format")
            .detail(ex.getOriginalMessage())
            .correlationId(getCorrelationId(request, null))
            .path(request.getRequestURI())
            .build();
        
        if (isDevEnvironment()) {
            errorResponse.setTraceId(generateTraceId(ex));
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles type mismatch errors.
     * 
     * Occurs when request parameter cannot be converted to expected type
     * (e.g., passing string when number expected).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        
        log.warn("Type mismatch for parameter '{}': expected {}, got {}",
            ex.getName(),
            ex.getRequiredType().getSimpleName(),
            ex.getValue());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .errorCode(ErrorCode.VALIDATION_INVALID_REQUEST.getCode())
            .message("Invalid request parameter type")
            .detail(String.format(
                "Parameter '%s' should be of type %s",
                ex.getName(),
                ex.getRequiredType().getSimpleName()))
            .correlationId(getCorrelationId(request, null))
            .path(request.getRequestURI())
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles 404 Not Found errors.
     * 
     * Triggered when requested endpoint doesn't exist.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {
        
        log.warn("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .errorCode("NOT_FOUND")
            .message("Endpoint not found")
            .detail(ex.getHttpMethod() + " " + ex.getRequestURL() + " not supported")
            .correlationId(getCorrelationId(request, null))
            .path(request.getRequestURI())
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Generic exception handler for any uncaught exceptions.
     * 
     * This is the last resort handler. All exceptions not handled by
     * specific handlers will be caught here.
     * 
     * IMPORTANT: Never expose sensitive information in error responses.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Unexpected error occurred", ex);
        
        // In production, don't expose the actual exception message
        String detailMessage = isDevEnvironment()
            ? ex.getMessage()
            : "An unexpected error occurred. Please try again later.";
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .errorCode(ErrorCode.SYSTEM_INTERNAL_ERROR.getCode())
            .message("Internal server error")
            .detail(detailMessage)
            .correlationId(getCorrelationId(request, null))
            .path(request.getRequestURI())
            .build();
        
        // Include trace ID for debugging in non-production
        if (isDevEnvironment()) {
            errorResponse.setTraceId(generateTraceId(ex));
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Gets the correlation ID from multiple sources.
     * Priority: provided ID > MDC > request header > generate new
     */
    private String getCorrelationId(HttpServletRequest request, String providedId) {
        if (providedId != null) {
            return providedId;
        }
        
        // Try to get from MDC
        String mdcId = MDC.get(CORRELATION_ID_MDC);
        if (mdcId != null) {
            return mdcId;
        }
        
        // Try to get from request header
        String headerId = request.getHeader(CORRELATION_ID_HEADER);
        if (headerId != null) {
            return headerId;
        }
        
        // None found, return null (ErrorResponse will handle it)
        return null;
    }
    
    /**
     * Generates a unique trace ID for debugging.
     * This ID can be used to correlate logs and errors across the system.
     */
    private String generateTraceId(Exception ex) {
        return String.format("TRACE-%d-%s",
            System.currentTimeMillis(),
            Integer.toHexString(ex.hashCode()).toUpperCase());
    }
    
    /**
     * Determines if running in development environment.
     * Used to decide what information to expose in error responses.
     */
    private boolean isDevEnvironment() {
        String[] profiles = environment.getActiveProfiles();
        for (String profile : profiles) {
            if ("dev".equalsIgnoreCase(profile) || "local".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        // If no profile is set, check if it's Spring Boot development
        return environment.containsProperty("spring.devtools.restart.enabled");
    }
}
