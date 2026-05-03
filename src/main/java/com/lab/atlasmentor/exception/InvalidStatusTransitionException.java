package com.lab.atlasmentor.exception;

/**
 * Exception thrown when a task status transition violates the defined workflow rules.
 */
public class InvalidStatusTransitionException extends RuntimeException {
    
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
    
    public InvalidStatusTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
