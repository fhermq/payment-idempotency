package com.fhermq.payment.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standardized error response DTO for REST API.
 * 
 * This DTO follows REST API best practices for error responses:
 * - Consistent format across all error scenarios
 * - Includes error code for machine-readable error identification
 * - Includes HTTP status code in response
 * - Includes timestamp for request tracking
 * - Includes correlation ID for tracing
 * - Optional fields for different error types (fieldErrors, details)
 * 
 * Example JSON response:
 * {
 *   "timestamp": "2026-01-19T10:30:00",
 *   "status": 400,
 *   "errorCode": "PAYMENT_001",
 *   "message": "Payment is already being processed",
 *   "detail": "Order ID: ORD-12345",
 *   "correlationId": "req-123-456",
 *   "path": "/api/payments"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * ISO 8601 timestamp of when the error occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * HTTP status code (e.g., 400, 404, 500)
     */
    private int status;
    
    /**
     * Machine-readable error code (e.g., "PAYMENT_001")
     * Used for client-side error handling and internationalization
     */
    private String errorCode;
    
    /**
     * Human-readable error message
     * Should be suitable for displaying to end users
     */
    private String message;
    
    /**
     * Additional error details
     * Can include information about what went wrong
     */
    private String detail;
    
    /**
     * Correlation ID for request tracing
     * Helps track a request through the system
     */
    private String correlationId;
    
    /**
     * The API path where the error occurred
     */
    private String path;
    
    /**
     * Field-level validation errors
     * Key: field name, Value: error message
     * Only present for validation errors
     */
    private Map<String, String> fieldErrors;
    
    /**
     * Additional error details as a map
     * Used for structured error information
     */
    private Map<String, Object> details;
    
    /**
     * Error trace ID for debugging
     * Can be used to look up detailed logs
     * Should only be included in development/staging environments
     */
    private String traceId;
}
