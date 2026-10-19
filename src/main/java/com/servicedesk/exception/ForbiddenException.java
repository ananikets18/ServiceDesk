package com.servicedesk.exception;

/**
 * Thrown when a logged-in user tries to do something their role or ownership
 * does not allow. The handler turns this into a 403 response.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
