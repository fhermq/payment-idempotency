package com.fhermq.payment.exception;

/**
 * Base exception class for API-related errors.
 * Provides standardized error information that can be serialized to JSON.
 * 
 * This exception should be extended for specific error scenarios or used
 * directly when the error code already captures the necessary information.
 */
public class ApiException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final String detailMessage;
    private final String correlationId;
    private final Throwable cause;
    
    public ApiException(ErrorCode errorCode) {
        this(errorCode, null, null, null);
    }
    
    public ApiException(ErrorCode errorCode, String detailMessage) {
        this(errorCode, detailMessage, null, null);
    }
    
    public ApiException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        this(errorCode, detailMessage, null, cause);
    }
    
    public ApiException(ErrorCode errorCode, String detailMessage, String correlationId, Throwable cause) {
        super(errorCode.getDefaultMessage() + (detailMessage != null ? ": " + detailMessage : ""), cause);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
        this.correlationId = correlationId;
        this.cause = cause;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public String getDetailMessage() {
        return detailMessage;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public int getHttpStatus() {
        return errorCode.getHttpStatus();
    }
    
    public String getErrorCodeString() {
        return errorCode.getCode();
    }
}
