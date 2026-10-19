package com.servicedesk.dto;

import com.servicedesk.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What the client sends when creating a ticket. The requester is no longer
 * part of the body - it is taken from the logged-in user.
 */
public class TicketRequest {

    @NotBlank(message = "title must not be blank")
    @Size(max = 150, message = "title must be at most 150 characters")
    private String title;

    @NotBlank(message = "description must not be blank")
    @Size(max = 2000, message = "description must be at most 2000 characters")
    private String description;

    @NotNull(message = "priority is required")
    private Priority priority;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }
}
