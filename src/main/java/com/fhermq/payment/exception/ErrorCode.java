package com.fhermq.payment.exception;

/**
 * Standardized error codes for the Payment API.
 * Follows REST API best practices for error classification.
 * 
 * Error codes are prefixed by category:
 * - PAYMENT_* : Payment processing errors
 * - VALIDATION_* : Input validation errors
 * - IDEMPOTENCY_* : Idempotency-related errors
 * - SYSTEM_* : System/internal errors
 */
public enum ErrorCode {
    
    // Payment Processing Errors (4xx)
    PAYMENT_ALREADY_PROCESSING("PAYMENT_001", "Payment is already being processed", 409),
    PAYMENT_FAILED("PAYMENT_002", "Payment processing failed", 400),
    PAYMENT_NOT_FOUND("PAYMENT_003", "Payment not found", 404),
    PAYMENT_AMOUNT_INVALID("PAYMENT_004", "Invalid payment amount", 400),
    
    // Validation Errors (4xx)
    VALIDATION_INVALID_REQUEST("VALIDATION_001", "Invalid request format", 400),
    VALIDATION_MISSING_HEADER("VALIDATION_002", "Required header missing", 400),
    VALIDATION_INVALID_HEADER("VALIDATION_003", "Invalid header value", 400),
    VALIDATION_INVALID_BODY("VALIDATION_004", "Invalid request body", 400),
    
    // Idempotency Errors (4xx)
    IDEMPOTENCY_KEY_INVALID("IDEMPOTENCY_001", "Invalid idempotency key format", 400),
    IDEMPOTENCY_KEY_MISSING("IDEMPOTENCY_002", "Idempotency-Key header is required", 400),
    
    // System/Internal Errors (5xx)
    SYSTEM_JSON_PROCESSING("SYSTEM_001", "JSON processing error", 500),
    SYSTEM_DATABASE_ERROR("SYSTEM_002", "Database error", 500),
    SYSTEM_INTERNAL_ERROR("SYSTEM_003", "Internal server error", 500),
    SYSTEM_SERIALIZATION_ERROR("SYSTEM_004", "Serialization error", 500);
    
    private final String code;
    private final String defaultMessage;
    private final int httpStatus;
    
    ErrorCode(String code, String defaultMessage, int httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDefaultMessage() {
        return defaultMessage;
    }
    
    public int getHttpStatus() {
        return httpStatus;
    }
    
    /**
     * Determines if this error code represents a client error (4xx)
     */
    public boolean isClientError() {
        return httpStatus >= 400 && httpStatus < 500;
    }
    
    /**
     * Determines if this error code represents a server error (5xx)
     */
    public boolean isServerError() {
        return httpStatus >= 500;
    }
}
