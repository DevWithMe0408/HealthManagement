package org.example.userservice.service;

import org.example.userservice.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(String username);
    Optional<RefreshToken> findByToken(String token);
    boolean isTokenExpired(RefreshToken token);
    void deleteByAuthId(Long authId);
    void delete(RefreshToken token);
}
