package org.example.userservice.controller;

import org.example.userservice.entity.User;
import org.example.userservice.enums.Role;
import org.example.userservice.entity.Auth;
import org.example.userservice.security.JwtUtil;
import org.example.userservice.service.RefreshTokenService;
import org.example.userservice.service.AuthService;
import org.example.userservice.service.UserService;
import org.example.userservice.service.dto.request.LoginRequest;
import org.example.userservice.service.dto.request.TokenRefreshRequest;
import org.example.userservice.service.dto.request.RegisterRequest;
import org.example.userservice.service.dto.response.TokenRefreshResponse;
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

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        if (authService.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body("Username is already taken");
        }

        if (authService.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body("Email is already in use");
        }
        // Tạo auth (chưa có user)
        Auth auth = new Auth(
                registerRequest.getUsername(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getEmail()
        );
        auth.setRole(Role.ROLE_USER);

        // Lưu auth trước để có ID
        Auth savedAuth = authService.save(auth);

        // Tạo user và gán auth đã có ID
        User user = userService.createDefaultUser();
        user.setAuth(savedAuth);
        savedAuth.setUser(user);

        // Lưu user
        userService.save(user);

        // Cập nhật lại auth để gán ngược `user_id`
        authService.save(savedAuth);

        return ResponseEntity.ok("User registered successfully");
    }

    // kiem tra username va password, neu dung tao token tra ve
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            log.info("Login request received for username: {}",loginRequest.getUsername());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            log.info("Authentication successful for user: {}",loginRequest.getUsername());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtil.generateToken(authentication);
            String refreshToken = refreshTokenService.createRefreshToken(loginRequest.getUsername()).getToken();

            return ResponseEntity.ok(new TokenRefreshResponse(refreshToken, jwt));
        } catch (AuthenticationException e) {
            log.error("Authentication failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequest request) {
        String requestToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestToken)
                .map(refreshToken -> {
                    if (refreshTokenService.isTokenExpired(refreshToken)) {
                        refreshTokenService.deleteByUserId(refreshToken.getAuth().getId());
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Refresh token expired. Please login again.");
                    }
                    Authentication authentication = new UsernamePasswordAuthenticationToken(refreshToken.getAuth().getUsername(), null, List.of(new SimpleGrantedAuthority(refreshToken.getAuth().getRole().name())));
                    String token = jwtUtil.generateToken(authentication);
                    return ResponseEntity.ok(new TokenRefreshResponse(token, requestToken));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid refresh token"));
    }
}
