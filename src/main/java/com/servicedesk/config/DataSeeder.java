package com.servicedesk.config;

import com.servicedesk.entity.Role;
import com.servicedesk.entity.User;
import com.servicedesk.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adds a few demo users the first time the app starts so the API can be tried
 * out straight away. Passwords are hashed with the same encoder used by login.
 *
 * Demo accounts (password: password123)
 * - riya@servicedesk.local   (REQUESTER)
 * - arjun@servicedesk.local  (AGENT)
 * - admin@servicedesk.local  (ADMIN)
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        seedUser("Riya Sharma", "riya@servicedesk.local", Role.REQUESTER, hash);
        seedUser("Arjun Mehta", "arjun@servicedesk.local", Role.AGENT, hash);
        seedUser("Admin", "admin@servicedesk.local", Role.ADMIN, hash);
    }

    private void seedUser(String name, String email, Role role, String hash) {
        userRepository.findByEmail(email).ifPresentOrElse(existing -> {
            // Earlier versions of the project seeded a placeholder hash, which
            // could never match a real password. Repair it once so the demo
            // accounts can actually be used.
            if ("seeded".equals(existing.getPasswordHash())) {
                existing.setPasswordHash(hash);
                userRepository.save(existing);
            }
        }, () -> userRepository.save(new User(name, email, hash, role)));
    }
}
