package com.servicedesk.dto;

import com.servicedesk.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body an admin sends to create a user directly (for example a new agent),
 * without that person having to register themselves.
 */
public class CreateUserRequest {

    @NotBlank(message = "name must not be blank")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a valid address")
    private String email;

    @NotBlank(message = "password must not be blank")
    @Size(min = 6, max = 100, message = "password must be between 6 and 100 characters")
    private String password;

    @NotNull(message = "role is required")
    private Role role;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
