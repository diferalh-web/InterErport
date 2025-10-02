package com.interexport.guarantees.exception;

/**
 * Exception thrown when a guarantee is not found.
 */
public class GuaranteeNotFoundException extends RuntimeException {

    public GuaranteeNotFoundException(String message) {
        super(message);
    }

    public GuaranteeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}