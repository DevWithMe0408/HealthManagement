package org.example.userservice.service;

import org.example.userservice.common.util.UuidV7Generator;
import org.example.userservice.entity.Auth;
import org.example.userservice.entity.RefreshToken;
import org.example.userservice.repository.AuthRepository;
import org.example.userservice.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

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
    public RefreshToken createRefreshToken(Auth authUser) {
        Optional<RefreshToken> existingTokenOptional = refreshTokenRepository.findByAuth(authUser);

        RefreshToken refreshTokenToSave;
        if (existingTokenOptional.isPresent()) {
            refreshTokenToSave = existingTokenOptional.get();
            refreshTokenToSave.setToken(UUID.randomUUID().toString());
            refreshTokenToSave.setExpiryDate(Instant.now().plusMillis(refreshExpirationDurationMs));
        } else {
            refreshTokenToSave = new RefreshToken();
            refreshTokenToSave.setId(UuidV7Generator.generate());
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
            return true;
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
    public void deleteByAuthId(String authId) {
        Auth auth = authRepository.findById(authId)
                .orElseThrow(() -> new RuntimeException("Auth not found with id: " + authId + " for deleting refresh token."));
        refreshTokenRepository.findByAuth(auth).ifPresent(refreshTokenRepository::delete);
    }
}
