package com.servicedesk.controller;

import com.servicedesk.dto.UserResponse;
import com.servicedesk.entity.Role;
import com.servicedesk.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Small read-only endpoint the frontend uses to fill the "assign to" dropdown.
 * Only agents and admins can see the list of agents.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public List<UserResponse> getAgents() {
        return userRepository.findByRole(Role.AGENT)
                .stream()
                .map(user -> new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole()))
                .toList();
    }
}
