package com.fhermq.payment.exception;

import java.util.Map;

/**
 * Exception for validation errors.
 * Can carry detailed field-level validation errors.
 */
public class ValidationException extends ApiException {
    
    private final Map<String, String> fieldErrors;
    
    public ValidationException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }
    
    public ValidationException(ErrorCode errorCode, String detailMessage) {
        this(errorCode, detailMessage, null);
    }
    
    public ValidationException(ErrorCode errorCode, String detailMessage, Map<String, String> fieldErrors) {
        super(errorCode, detailMessage);
        this.fieldErrors = fieldErrors;
    }
    
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
