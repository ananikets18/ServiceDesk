package com.servicedesk.service;

import com.servicedesk.dto.CreateUserRequest;
import com.servicedesk.dto.UserResponse;
import com.servicedesk.entity.Role;
import com.servicedesk.entity.User;
import com.servicedesk.exception.DuplicateResourceException;
import com.servicedesk.exception.ForbiddenException;
import com.servicedesk.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the admin-only user management rules.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest createRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Neha Kulkarni");
        request.setEmail("  Neha@ServiceDesk.Local ");
        request.setPassword("secret123");
        request.setRole(Role.AGENT);
        return request;
    }

    @Test
    @DisplayName("createUser hashes the password and normalises the email")
    void createUserHashesPassword() {
        when(userRepository.findByEmail("neha@servicedesk.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        UserResponse response = userService.createUser(createRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(captor.getValue().getEmail()).isEqualTo("neha@servicedesk.local");
        assertThat(response.getRole()).isEqualTo(Role.AGENT);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("createUser rejects an email that is already taken")
    void createUserRejectsDuplicateEmail() {
        User existing = new User("Someone", "neha@servicedesk.local", "hash", Role.AGENT);
        when(userRepository.findByEmail("neha@servicedesk.local")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.createUser(createRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("setActive deactivates another account")
    void setActiveDeactivatesAnotherUser() {
        User agent = new User("Arjun Mehta", "arjun@servicedesk.local", "hash", Role.AGENT);
        agent.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(agent));
        when(userRepository.save(agent)).thenReturn(agent);

        UserResponse response = userService.setActive(2L, false, 4L);

        assertThat(response.isActive()).isFalse();
        verify(userRepository).save(agent);
    }

    @Test
    @DisplayName("setActive stops an admin from deactivating themselves")
    void setActiveBlocksSelf() {
        assertThatThrownBy(() -> userService.setActive(4L, false, 4L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("your own account");

        verify(userRepository, never()).save(any(User.class));
    }
}
