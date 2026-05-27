package org.example.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.request.ChangePasswordRequest;
import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.dto.request.TokenRefreshRequest;
import org.example.web.dto.response.DataResponse;
import org.example.userservice.dto.response.TokenRefreshResponse;
import org.example.userservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<DataResponse<Void>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.registerUser(registerRequest);
        return ResponseEntity.ok(DataResponse.success());
    }

    @PostMapping("/login")
    public ResponseEntity<DataResponse<TokenRefreshResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        TokenRefreshResponse tokenResponse = authService.loginUser(loginRequest);
        return ResponseEntity.ok(DataResponse.success(tokenResponse));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<DataResponse<TokenRefreshResponse>> refreshToken(@RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse tokenResponse = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(DataResponse.success(tokenResponse));
    }

    @PutMapping("/change-password")
    public ResponseEntity<DataResponse<Void>> changePassword(
            @RequestHeader("userId") String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ResponseEntity.ok(DataResponse.success());
    }
}
