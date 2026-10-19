package com.servicedesk.service;

import com.servicedesk.dto.AuthResponse;
import com.servicedesk.dto.LoginRequest;
import com.servicedesk.dto.RegisterRequest;
import com.servicedesk.entity.Role;
import com.servicedesk.entity.User;
import com.servicedesk.exception.DuplicateResourceException;
import com.servicedesk.repository.UserRepository;
import com.servicedesk.security.JwtService;
import com.servicedesk.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for registration and login. Everything around AuthService is
 * mocked, so we only check its own decisions.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail(email);
        request.setPassword("secret123");
        request.setRole(Role.REQUESTER);
        return request;
    }

    @Test
    @DisplayName("register hashes the password instead of storing it")
    void registerHashesPassword() {
        when(userRepository.findByEmail("new@servicedesk.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("test-token");

        AuthResponse response = authService.register(registerRequest("new@servicedesk.local"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("secret123");
        assertThat(response.getToken()).isEqualTo("test-token");
        assertThat(response.getUserId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("register trims and lowercases the email")
    void registerNormalisesEmail() {
        when(userRepository.findByEmail("new@servicedesk.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("token");

        authService.register(registerRequest("  New@ServiceDesk.Local  "));

        verify(userRepository).findByEmail("new@servicedesk.local");
    }

    @Test
    @DisplayName("register rejects an email that is already taken")
    void registerRejectsDuplicateEmail() {
        User existing = new User("Existing", "taken@servicedesk.local", "hash", Role.REQUESTER);
        when(userRepository.findByEmail("taken@servicedesk.local")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.register(registerRequest("taken@servicedesk.local")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login returns a token for valid credentials")
    void loginReturnsToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("riya@servicedesk.local");
        request.setPassword("password123");

        User user = new User("Riya Sharma", "riya@servicedesk.local", "hash", Role.REQUESTER);
        user.setId(1L);

        when(userRepository.findByEmail("riya@servicedesk.local")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("login-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("login-token");
        assertThat(response.getRole()).isEqualTo(Role.REQUESTER);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login propagates bad credentials")
    void loginRejectsWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("riya@servicedesk.local");
        request.setPassword("wrong");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(any());
    }
}
