package com.servicedesk.config;

import com.servicedesk.entity.Role;
import com.servicedesk.entity.User;
import com.servicedesk.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Adds a few demo users the first time the app starts. We need at least one
 * requester and one agent to try the ticket endpoints, and registration/login
 * do not exist yet. If any user is already present, this does nothing.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        userRepository.save(new User("Riya Sharma", "riya@servicedesk.local", "seeded", Role.REQUESTER));
        userRepository.save(new User("Arjun Mehta", "arjun@servicedesk.local", "seeded", Role.AGENT));
        userRepository.save(new User("Admin", "admin@servicedesk.local", "seeded", Role.ADMIN));
    }
}
