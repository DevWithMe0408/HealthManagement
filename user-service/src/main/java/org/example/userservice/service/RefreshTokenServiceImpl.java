package org.example.userservice.service;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.example.userservice.entity.RefreshToken;
import org.example.userservice.entity.Auth;
import org.example.userservice.repository.AuthRepository;
import org.example.userservice.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshExpirationDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthRepository authRepository;


    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, AuthRepository authRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authRepository = authRepository;
    }
    @Override
    @Transactional
    public RefreshToken createRefreshToken (String username) {
        Auth authUser = authRepository.findByUsername(username)
                .orElseThrow(()-> new RuntimeException("Error: User not found for refresh token generation."));

        Optional<RefreshToken> existingTokenOptional = refreshTokenRepository.findByAuth(authUser);

        RefreshToken refreshTokenToSave;
        if (existingTokenOptional.isPresent()) {
            // If a token already exists for this user, update it
            refreshTokenToSave = existingTokenOptional.get();
            refreshTokenToSave.setToken(UUID.randomUUID().toString());
            refreshTokenToSave.setExpiryDate(Instant.now().plusMillis(refreshExpirationDurationMs));
        } else {
            // If no token exists, create a new one
            refreshTokenToSave = new RefreshToken();
            refreshTokenToSave.setAuth(authUser);
            refreshTokenToSave.setToken(UUID.randomUUID().toString());
            refreshTokenToSave.setExpiryDate(Instant.now().plusMillis(refreshExpirationDurationMs));
        }

        return refreshTokenRepository.save(refreshTokenToSave);

    }
    @Override
    public Optional<RefreshToken> findByToken(String tokenValue) {
        return refreshTokenRepository.findByToken(tokenValue);
    }
    @Override
    public boolean isTokenExpired(RefreshToken token) {
        if (token == null || token.getExpiryDate() == null) {
            return true; // Coi như hết hạn nếu token hoặc expiryDate null
        }
        return token.getExpiryDate().isBefore(Instant.now());
    }
    @Override
    @Transactional
    public void delete(RefreshToken refreshToken) {
        if (refreshToken != null) {
            refreshTokenRepository.delete(refreshToken);
        }
    }
    @Override
    @Transactional
    public void deleteByAuthId(Long authId) {
        Auth auth = authRepository.findById(authId)
                .orElseThrow(() -> new RuntimeException("Auth not found with id: " + authId + " for deleting refresh token."));
        refreshTokenRepository.findByAuth(auth).ifPresent(refreshTokenRepository::delete);
    }
}
