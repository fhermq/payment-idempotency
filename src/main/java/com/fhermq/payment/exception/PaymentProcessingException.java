package com.fhermq.payment.exception;

/**
 * Deprecated: Use PaymentException with ErrorCode instead.
 * 
 * This class is kept for backward compatibility.
 * New code should use PaymentException with appropriate ErrorCode values.
 * 
 * @deprecated Use {@link PaymentException} instead
 */
@Deprecated(since = "1.1.0", forRemoval = true)
public class PaymentProcessingException extends RuntimeException {
    public PaymentProcessingException(String message) {
        super(message);
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}