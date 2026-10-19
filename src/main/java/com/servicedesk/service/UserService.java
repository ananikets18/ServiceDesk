package com.servicedesk.service;

import com.servicedesk.dto.CreateUserRequest;
import com.servicedesk.dto.UserResponse;
import com.servicedesk.entity.Role;
import com.servicedesk.entity.User;
import com.servicedesk.exception.DuplicateResourceException;
import com.servicedesk.exception.ForbiddenException;
import com.servicedesk.exception.ResourceNotFoundException;
import com.servicedesk.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User related operations: looking users up (used when creating and assigning
 * tickets) and the admin-only management of accounts.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("An account with email " + email + " already exists");
        }

        User user = new User(
                request.getName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                request.getRole());
        return toResponse(userRepository.save(user));
    }

    /**
     * Turns an account on or off. Admins cannot lock themselves out.
     */
    @Transactional
    public UserResponse setActive(Long id, boolean active, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new ForbiddenException("You cannot deactivate your own account");
        }

        User user = getById(id);
        user.setActive(active);
        return toResponse(userRepository.save(user));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive());
    }
}
