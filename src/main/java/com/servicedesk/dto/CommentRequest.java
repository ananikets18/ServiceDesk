package com.servicedesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for adding a comment to a ticket.
 * Like the ticket request, authorId is temporary until authentication exists.
 */
public class CommentRequest {

    @NotNull(message = "authorId is required")
    private Long authorId;

    @NotBlank(message = "message must not be blank")
    @Size(max = 2000, message = "message must be at most 2000 characters")
    private String message;

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
