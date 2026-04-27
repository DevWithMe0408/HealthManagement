package org.example.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptHashGeneratorTest {
    @Test
    public void generateAdminPasswordHash() {
        String rawPassword = "Admin@2026";
        String hash = new BCryptPasswordEncoder().encode(rawPassword);
        System.out.println("=".repeat(60));
        System.out.println("Password raw: " + rawPassword);
        System.out.println("BCrypt hash:  " + hash);
        System.out.println("=".repeat(60));
    }
}