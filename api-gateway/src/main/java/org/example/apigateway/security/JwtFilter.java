package org.example.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtFilter extends AbstractGatewayFilterFactory<JwtFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    @Value("${jwt.secret}")
    private String secretKey;

    public JwtFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().toString();
            logger.debug("Processing request for path: {}", path);

            // Skip authentication for public endpoints
            if (isPublicEndpoint(path)) {
                logger.debug("Skipping authentication for public endpoint: {}", path);
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Missing or invalid authorization header for path: {}", path);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization header");
            }

            String token = authHeader.substring(7);
            try {
                // Create SecretKey from the secret string
                byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
                SecretKey key = Keys.hmacShaKeyFor(keyBytes); // từ thư viện jjwt


                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                logger.debug("Successfully validated token for user: {}", claims.getSubject());

                // Add user information to headers for downstream services
                return chain.filter(exchange.mutate()
                        .request(exchange.getRequest().mutate()
                                .header("userId", String.valueOf(claims.get("userId")))
                                .header("username", claims.getSubject())
                                .header("userRoles", claims.get("roles", String.class))
                                .build())
                        .build());
            } catch (Exception e) {
                logger.error("Token validation failed for path: {}", path, e);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
            }
        };
    }

    private boolean isPublicEndpoint(String path) {
        // Add more specific checks for public endpoints
        return path.contains("/api/auth/login") ||
//                path.contains("/api/user") ||
                path.contains("/api/auth/register") ||
                path.contains("/api/auth/refresh") ||
                path.contains("/api/public/") ||
                path.contains("/actuator/") ||
                path.contains("/v3/api-docs") ||
                path.contains("/swagger-ui");
    }

    public static class Config {
        // Configuration can be extended later if needed
    }
}