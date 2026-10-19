package com.servicedesk.exception;

/**
 * Thrown when we look up something (a ticket, a user, ...) that does not exist.
 * The global handler turns this into a 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
