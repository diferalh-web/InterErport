package com.interexport.guarantees.exception;

/**
 * Exception thrown when an operation is attempted on a guarantee in an invalid state.
 */
public class InvalidGuaranteeStateException extends RuntimeException {
    
    public InvalidGuaranteeStateException(String message) {
        super(message);
    }
    
    public InvalidGuaranteeStateException(String message, Throwable cause) {
        super(message, cause);
    }
}





