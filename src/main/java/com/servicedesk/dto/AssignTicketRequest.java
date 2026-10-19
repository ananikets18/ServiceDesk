package com.servicedesk.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body for assigning a ticket to an agent.
 */
public class AssignTicketRequest {

    @NotNull(message = "assigneeId is required")
    private Long assigneeId;

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }
}
