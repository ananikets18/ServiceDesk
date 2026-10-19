package com.servicedesk.service;

import com.servicedesk.dto.AuthResponse;
import com.servicedesk.dto.LoginRequest;
import com.servicedesk.dto.RegisterRequest;
import com.servicedesk.entity.User;
import com.servicedesk.exception.DuplicateResourceException;
import com.servicedesk.repository.UserRepository;
import com.servicedesk.security.JwtService;
import com.servicedesk.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login. Passwords are hashed before they reach the database,
 * and a successful login returns a JWT the client can use afterwards.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("An account with email " + email + " already exists");
        }

        User user = new User(
                request.getName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                request.getRole());
        User saved = userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(saved);
        return toAuthResponse(saved, jwtService.generateToken(principal));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // This throws BadCredentialsException if the email or password is wrong.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User disappeared after login"));
        UserPrincipal principal = new UserPrincipal(user);
        return toAuthResponse(user, jwtService.generateToken(principal));
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
