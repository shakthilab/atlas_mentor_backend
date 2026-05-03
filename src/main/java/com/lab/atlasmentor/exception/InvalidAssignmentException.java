package com.lab.atlasmentor.exception;

/**
 * Exception thrown when a task assignment violates role-based rules
 * or hierarchy constraints.
 */
public class InvalidAssignmentException extends RuntimeException {
    
    public InvalidAssignmentException(String message) {
        super(message);
    }
    
    public InvalidAssignmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
