package com.servicedesk.entity;

import java.util.List;

/**
 * The lifecycle of a ticket: Open -> In Progress -> Resolved -> Closed.
 *
 * Not every jump makes sense, so the allowed moves are defined here instead of
 * being spread around the service. A ticket can move forward one step at a
 * time, or be sent back one step if it needs more work. Closed is final.
 */
public enum Status {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    /**
     * Returns the statuses this one is allowed to move to.
     */
    public List<Status> allowedNextStatuses() {
        return switch (this) {
            case OPEN -> List.of(IN_PROGRESS);
            case IN_PROGRESS -> List.of(RESOLVED, OPEN);
            case RESOLVED -> List.of(CLOSED, IN_PROGRESS);
            case CLOSED -> List.of();
        };
    }

    public boolean canTransitionTo(Status next) {
        return next != null && allowedNextStatuses().contains(next);
    }
}
