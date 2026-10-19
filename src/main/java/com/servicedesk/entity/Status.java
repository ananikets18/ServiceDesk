package com.servicedesk.entity;

/**
 * The lifecycle of a ticket: Open -> In Progress -> Resolved -> Closed.
 */
public enum Status {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
