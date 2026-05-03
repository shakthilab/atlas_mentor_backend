package com.lab.atlasmentor.exception;

/**
 * Exception thrown when a user attempts to access or modify data outside their branch scope.
 */
public class BranchSecurityException extends RuntimeException {
    
    public BranchSecurityException(String message) {
        super(message);
    }
    
    public BranchSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
