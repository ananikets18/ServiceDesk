package com.servicedesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for editing a ticket's title and description. Status and assignment
 * have their own endpoints, so they are not part of this request.
 */
public class TicketUpdateRequest {

    @NotBlank(message = "title must not be blank")
    @Size(max = 150, message = "title must be at most 150 characters")
    private String title;

    @NotBlank(message = "description must not be blank")
    @Size(max = 2000, message = "description must be at most 2000 characters")
    private String description;

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
}
