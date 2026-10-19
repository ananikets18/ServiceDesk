package com.servicedesk.controller;

import com.servicedesk.dto.CreateUserRequest;
import com.servicedesk.dto.UserResponse;
import com.servicedesk.entity.Role;
import com.servicedesk.repository.UserRepository;
import com.servicedesk.security.UserPrincipal;
import com.servicedesk.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * User endpoints. Agents and admins can read the agent list to fill the
 * "assign to" dropdown; everything else here is for admins only, who manage
 * the people in the system.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public List<UserResponse> getAgents() {
        return userRepository.findByRole(Role.AGENT)
                .stream()
                .filter(user -> user.isActive())
                .map(user -> new UserResponse(user.getId(), user.getName(), user.getEmail(),
                        user.getRole(), user.isActive()))
                .toList();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getUsers() {
        return userService.listUsers();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse deactivateUser(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        return userService.setActive(id, false, currentUser.getId());
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse activateUser(@PathVariable Long id,
                                     @AuthenticationPrincipal UserPrincipal currentUser) {
        return userService.setActive(id, true, currentUser.getId());
    }
}
