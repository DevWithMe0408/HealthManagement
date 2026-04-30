package org.example.userservice.service;

import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.dto.response.TokenRefreshResponse;
import org.example.userservice.entity.Auth;

public interface AuthService {
     boolean existsByUsername(String username);
     boolean existsByEmail(String email);
     Auth save(Auth user);

     /**
      * Register a new user
      * @param request
      */
     void registerUser(RegisterRequest request);
     /**
      * Login a user
      * @param request
      */
     TokenRefreshResponse loginUser(LoginRequest request);
     /**
      * Refresh access token using refresh token
      * @param refreshTokenString
      */
     TokenRefreshResponse refreshAccessToken(String refreshTokenString);

}
