package com.servicedesk.exception;

/**
 * Thrown when something already exists, for example registering with an email
 * that is already taken. The handler turns this into a 409 response.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
