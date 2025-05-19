package org.example.userservice.service;

import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.dto.response.TokenRefreshResponse;
import org.example.userservice.entity.Auth;

import java.util.Optional;

public interface AuthService {
     boolean existsByUsername(String username);
     boolean existsByEmail(String email);
     Auth save(Auth user);
     void registerUser(RegisterRequest request);
     TokenRefreshResponse loginUser(LoginRequest request);
     Optional<TokenRefreshResponse> refreshAccessToken(String refreshTokenString);

}
