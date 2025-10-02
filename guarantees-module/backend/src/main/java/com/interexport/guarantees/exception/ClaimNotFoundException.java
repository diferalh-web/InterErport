package com.interexport.guarantees.exception;

/**
 * Exception thrown when a claim is not found
 */
public class ClaimNotFoundException extends RuntimeException {
    
    public ClaimNotFoundException(String message) {
        super(message);
    }
    
    public ClaimNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}





