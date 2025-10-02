package com.interexport.guarantees.exception;

/**
 * Exception thrown when a parameter (bank, account, commission parameter) is not found
 */
public class ParameterNotFoundException extends RuntimeException {
    
    public ParameterNotFoundException(String message) {
        super(message);
    }
    
    public ParameterNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}





