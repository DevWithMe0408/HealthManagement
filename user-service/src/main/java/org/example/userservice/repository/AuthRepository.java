package org.example.userservice.repository;

import org.example.userservice.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<Auth, String> {

    Optional<Auth> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
