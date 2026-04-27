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
import org.example.userservice.exception.BusinessException;
import org.example.userservice.exception.ErrorCode;
import org.example.userservice.repository.AuthRepository;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.security.CustomUserDetails;
import org.example.userservice.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private UserRepository userRepository;

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
            throw new BusinessException(ErrorCode.USERNAME_TAKEN);
        }
        if (existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_TAKEN);
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
            throw new BusinessException(ErrorCode.EVENT_PUBLISH_FAILED);
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
        Auth auth = ((org.example.userservice.security.CustomUserDetails) authentication.getPrincipal()).getAuth();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(auth);

        return new TokenRefreshResponse(jwt, refreshToken.getToken());
    }

    @Override
    public TokenRefreshResponse refreshAccessToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenString)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (refreshTokenService.isTokenExpired(refreshToken)) {
            refreshTokenService.delete(refreshToken);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        Auth auth = refreshToken.getAuth();
        User user = userRepository.findByAuth_Id(auth.getId()).orElse(null);
        CustomUserDetails userDetails = new CustomUserDetails(auth, user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        String newJwt = jwtUtil.generateToken(authentication);
        return new TokenRefreshResponse(newJwt, refreshToken.getToken());
    }
}
