package com.fhermq.payment.exception;

/**
 * Exception for idempotency-related errors.
 * Extends ApiException for idempotency-specific scenarios.
 */
public class IdempotencyException extends ApiException {
    
    public IdempotencyException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public IdempotencyException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
    
    public IdempotencyException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(errorCode, detailMessage, cause);
    }
    
    public IdempotencyException(ErrorCode errorCode, String detailMessage, String correlationId, Throwable cause) {
        super(errorCode, detailMessage, correlationId, cause);
    }
}
