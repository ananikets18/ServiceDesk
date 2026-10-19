package com.servicedesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for adding a comment. The author comes from the logged-in user.
 */
public class CommentRequest {

    @NotBlank(message = "message must not be blank")
    @Size(max = 2000, message = "message must be at most 2000 characters")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
