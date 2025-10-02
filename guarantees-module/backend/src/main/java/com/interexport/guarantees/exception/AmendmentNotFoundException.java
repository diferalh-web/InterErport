package com.interexport.guarantees.exception;

/**
 * Exception thrown when an amendment is not found
 */
public class AmendmentNotFoundException extends RuntimeException {
    
    public AmendmentNotFoundException(String message) {
        super(message);
    }
    
    public AmendmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}





