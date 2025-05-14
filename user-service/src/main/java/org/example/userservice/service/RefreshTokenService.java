package org.example.userservice.service;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.example.userservice.entity.RefreshToken;
import org.example.userservice.entity.Auth;
import org.example.userservice.repository.AuthRepository;
import org.example.userservice.repository.RefreshTokenRepository;
import org.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class RefreshTokenService {

    @Value("${jwt.secret}")
    private String secretKey;
    @Value("${jwt.refresh-token-expiration}")
    private Long refreshExpirationDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthRepository authRepository;


    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, AuthRepository authRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authRepository = authRepository;
    }
    @Transactional
    public RefreshToken createRefreshToken (String username) {
        Auth user = authRepository.findByUsername(username).orElseThrow();
        // Xoá token cũ nếu đã tồn tại
        refreshTokenRepository.deleteByAuth(user);
        refreshTokenRepository.flush();
        // Tạo token mới

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getUsername());
        claims.put("roles", user.getRole().name());
        claims.put("userId", user.getUser().getId());
        claims.put("created", new Date());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationDurationMs);

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                .compact();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setAuth(user);
        refreshToken.setToken(token);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpirationDurationMs));
        return refreshTokenRepository.save(refreshToken);
    }
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    public boolean isTokenExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }
    public void deleteByUserId(Long userId) {
        authRepository.findById(userId).ifPresent(refreshTokenRepository::deleteByAuth);
    }
}
