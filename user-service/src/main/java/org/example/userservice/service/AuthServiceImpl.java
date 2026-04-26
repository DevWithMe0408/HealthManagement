package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import org.example.events.UserCreatedEvent;
import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.dto.response.TokenRefreshResponse;
import org.example.userservice.entity.Auth;
import org.example.userservice.entity.RefreshToken;
import org.example.userservice.entity.User;
import org.example.userservice.enums.Role;
import org.example.userservice.exception.TokenRefreshException;
import org.example.userservice.exception.UsernameAlreadyExistsException;
import org.example.userservice.repository.AuthRepository;
import org.example.userservice.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.user-events}")
    private String userEventsExchangeName;

    @Value("${app.rabbitmq.routing-key.user-created}")
    private String userCreateRoutingKey;

    @Override
    public boolean existsByUsername(String username) {
        return authRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return authRepository.existsByEmail(email);
    }

    @Override
    public Auth save(Auth user) {
        return authRepository.save(user);
    }

    @Override
    @Transactional
    public void registerUser(RegisterRequest request) {
        if (existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username is already taken");
        }
        if (existsByEmail(request.getEmail())) {
            throw new UsernameAlreadyExistsException("Email is already in use");
        }
        // 1. Create Auth (UUID v7 generated inside the constructor)
        Auth auth = new Auth(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getEmail()
        );
        auth.setRole(Role.ROLE_USER);
        Auth savedAuth = authRepository.save(auth);

        // 2. Create User holding FK to Auth (User is the owning side now)
        User savedUser = userService.createAndAssociateUser(savedAuth);

        // 3. Publish event
        UserCreatedEvent event = new UserCreatedEvent(
                savedUser.getId(),
                savedAuth.getUsername(),
                savedAuth.getEmail()
        );
        try {
            log.info("Sending UserCreatedEvent for userId: {}, username: {}", event.getUserId(), event.getUsername());
            rabbitTemplate.convertAndSend(userEventsExchangeName, userCreateRoutingKey, event);
            log.info("UserCreatedEvent sent successfully to exchange '{}' with routing key '{}'", userEventsExchangeName, userCreateRoutingKey);
        } catch (Exception e) {
            log.error("Failed to send UserCreatedEvent for userId: {}. Error: {}", event.getUserId(), e.getMessage(), e);
        }
        log.info("User {} registered successfully. Auth ID: {}, User ID: {}", savedAuth.getUsername(), savedAuth.getId(), savedUser.getId());
    }

    @Override
    public TokenRefreshResponse loginUser(LoginRequest request) {
        log.info("Authenticating user: {}", request.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        log.info("Authentication successful for user: {}", request.getUsername());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtil.generateToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(request.getUsername());

        return new TokenRefreshResponse(jwt, refreshToken.getToken());
    }

    @Override
    public Optional<TokenRefreshResponse> refreshAccessToken(String refreshTokenString) {
        return refreshTokenService.findByToken(refreshTokenString)
                .map(refreshToken -> {
                    if (refreshTokenService.isTokenExpired(refreshToken)) {
                        refreshTokenService.delete(refreshToken);
                        throw new TokenRefreshException(refreshTokenString, "Refresh token was expired. Please make a new signin request");
                    }
                    Auth auth = refreshToken.getAuth();
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            auth.getUsername(),
                            null,
                            List.of(new SimpleGrantedAuthority(auth.getRole().name()))
                    );
                    String newJwt = jwtUtil.generateToken(authentication);
                    return new TokenRefreshResponse(newJwt, refreshToken.getToken());
                });
    }
}
