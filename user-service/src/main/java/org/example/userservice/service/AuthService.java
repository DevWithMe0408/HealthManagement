package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import org.example.userservice.entity.Auth;
import org.example.userservice.repository.AuthRepository;
import org.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    @Autowired
    private AuthRepository authRepository;

    private BCryptPasswordEncoder passwordEncoder;

    public Optional<Auth> getUserByUsername(String username) {
        return authRepository.findByUsername(username);
    }

    /**
     * Check if a user exists by username
     * @param username the username to check
     * @return true if user exists, false otherwise
     */
    public boolean existsByUsername(String username) {
        return authRepository.existsByUsername(username);
    }

    /**
     * Check if a user exists by email
     * @param email the email to check
     * @return true if user exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        return authRepository.existsByEmail(email);
    }

    /**
     * Save a new user
     * @param user the user to save
     * @return the saved user
     */
    public Auth save(Auth user) {
        return authRepository.save(user);
    }
}
