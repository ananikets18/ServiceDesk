package com.servicedesk.dto;

import com.servicedesk.entity.Status;
import jakarta.validation.constraints.NotNull;

/**
 * Body for changing a ticket's status.
 */
public class StatusUpdateRequest {

    @NotNull(message = "status is required")
    private Status status;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
