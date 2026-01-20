package com.fhermq.payment.exception;

/**
 * Exception for payment processing errors.
 * Extends ApiException for payment-specific scenarios.
 */
public class PaymentException extends ApiException {
    
    public PaymentException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public PaymentException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
    
    public PaymentException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(errorCode, detailMessage, cause);
    }
    
    public PaymentException(ErrorCode errorCode, String detailMessage, String correlationId, Throwable cause) {
        super(errorCode, detailMessage, correlationId, cause);
    }
}
