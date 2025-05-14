package org.example.userservice.service.dto.response;

import lombok.Data;

@Data
public class TokenRefreshResponse {
    private String refreshToken;
    private String accessToken;
    private String tokenType = "Bearer";

    public TokenRefreshResponse(String refreshToken, String accessToken) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
    }
}
