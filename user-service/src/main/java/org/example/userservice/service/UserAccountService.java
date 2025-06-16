package org.example.userservice.service;

import org.example.userservice.dto.request.UserRequestDTO;
import org.example.userservice.dto.response.UserAccountDetailsResponse;

public interface UserAccountService {
    UserAccountDetailsResponse updateUserAccount(Long userId, UserRequestDTO request);
}
