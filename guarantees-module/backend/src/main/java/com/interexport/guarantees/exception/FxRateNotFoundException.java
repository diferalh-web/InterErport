package com.interexport.guarantees.exception;

/**
 * Exception thrown when an exchange rate is not found.
 */
public class FxRateNotFoundException extends RuntimeException {
    
    public FxRateNotFoundException(String message) {
        super(message);
    }
    
    public FxRateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}





