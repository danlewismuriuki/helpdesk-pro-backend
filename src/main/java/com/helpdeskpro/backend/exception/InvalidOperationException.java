package com.helpdeskpro.backend.exception;

/**
 * Exception thrown when a business rule is violated
 * Examples:
 * - Trying to assign ticket to non-agent user
 * - Invalid status transition
 * - Unauthorized operation
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }

    public InvalidOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}