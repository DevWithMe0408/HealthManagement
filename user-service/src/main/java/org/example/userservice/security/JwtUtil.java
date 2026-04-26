package org.example.userservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.example.userservice.entity.User;
import org.example.userservice.repository.AuthRepository;
import org.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.expiration}")
    private long expirationTime;

    private Key getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        String userId = null;
        if (userDetails instanceof CustomUserDetails cud && cud.getId() != null) {
            userId = cud.getId();
        } else {
            // Fallback: lookup via DB. Phase 3 will remove this branch entirely.
            String authId = authRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Auth not found for username: " + userDetails.getUsername()))
                    .getId();
            User user = userRepository.findByAuth_Id(authId)
                    .orElseThrow(() -> new RuntimeException("User not found for auth id: " + authId));
            userId = user.getId();
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userDetails.getUsername());
        claims.put("created", now);
        claims.put("roles", userDetails.getAuthorities().iterator().next().getAuthority());
        claims.put("userId", userId);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
}
