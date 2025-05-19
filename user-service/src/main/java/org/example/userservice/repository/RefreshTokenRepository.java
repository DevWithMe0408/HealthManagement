package org.example.userservice.repository;

import org.example.userservice.entity.RefreshToken;
import org.example.userservice.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByAuth(Auth auth);

    void deleteByAuth(Auth auth);

    void delete(RefreshToken refreshToken);

}
