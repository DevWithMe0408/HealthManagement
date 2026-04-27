package org.example.userservice.service;

import org.example.userservice.entity.Auth;
import org.example.userservice.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Auth auth);
    Optional<RefreshToken> findByToken(String token);
    boolean isTokenExpired(RefreshToken token);
    void deleteByAuthId(String authId);
    void delete(RefreshToken token);
}
