package com.lab.atlasmentor.exception;

/**
 * Exception thrown when a user attempts to perform an operation without proper authorization
 * based on their role and hierarchy permissions.
 */
public class UnauthorizedAccessException extends RuntimeException {
    
    public UnauthorizedAccessException(String message) {
        super(message);
    }
    
    public UnauthorizedAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
