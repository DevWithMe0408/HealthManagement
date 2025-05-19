package org.example.userservice.controller;

import org.example.userservice.dto.response.MessageResponse;
import org.example.userservice.entity.User;
import org.example.userservice.enums.Role;
import org.example.userservice.entity.Auth;
import org.example.userservice.exception.TokenRefreshException;
import org.example.userservice.security.JwtUtil;
import org.example.userservice.service.AuthService;
import org.example.userservice.service.RefreshTokenServiceImpl;
import org.example.userservice.service.AuthServiceImpl;
import org.example.userservice.service.UserServiceImpl;
import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.dto.request.TokenRefreshRequest;
import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.dto.response.TokenRefreshResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        // Các exception như UsernameAlreadyExistsException, EmailAlreadyExistsException
        // sẽ được ném từ service và có thể được xử lý bằng @ControllerAdvice
        // hoặc bắt tại đây nếu muốn custom response cụ thể hơn.
        authService.registerUser(registerRequest);
        return ResponseEntity.ok((new MessageResponse("User registered successfully!")));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            TokenRefreshResponse tokenResponse = authService.loginUser(loginRequest);
            return ResponseEntity.ok(tokenResponse);
        } catch (AuthenticationException e) {
            logger.error("Authentication failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Error: Invalid username or password"));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        return authService.refreshAccessToken(requestRefreshToken)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token not found or invalid"));
    }
    // Bạn có thể thêm một @ControllerAdvice để xử lý các custom exceptions
    // Ví dụ:
    // @ExceptionHandler(UsernameAlreadyExistsException.class)
    // public ResponseEntity<?> handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
    // return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
    // }
    // @ExceptionHandler(TokenRefreshException.class)
    // public ResponseEntity<?> handleTokenRefreshException(TokenRefreshException ex) {
    // return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(ex.getMessage()));
    // }
}
