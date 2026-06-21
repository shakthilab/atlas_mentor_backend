package com.lab.atlasmentor.exception;

/**
 * Thrown by service layer for intentional, user-safe error conditions.
 * The message is safe to expose in API responses.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
