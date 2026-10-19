package com.servicedesk.service;

import com.servicedesk.entity.User;
import com.servicedesk.exception.ResourceNotFoundException;
import com.servicedesk.repository.UserRepository;
import org.springframework.stereotype.Service;

/**
 * User related operations. For now it only needs to look users up, which is
 * enough for creating tickets and assigning them. Login comes later.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }
}
