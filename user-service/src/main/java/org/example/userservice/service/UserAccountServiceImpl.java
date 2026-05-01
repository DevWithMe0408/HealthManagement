package org.example.userservice.service;

import org.example.userservice.dto.request.UserRequestDTO;
import org.example.userservice.dto.response.UserAccountDetailsResponse;
import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.example.userservice.repository.AuthRepository;
import org.example.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class UserAccountServiceImpl implements UserAccountService {
    private static final Logger log = LoggerFactory.getLogger(UserAccountServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthRepository authRepository;

    @Override
    @Transactional
    public UserAccountDetailsResponse updateUserAccount(String userId, UserRequestDTO updateRequest) {
        log.info("Processing account update for userId: {} via UserAccountService", userId);

        User userUpdates = new User();
        userUpdates.setName(updateRequest.getName());
        userUpdates.setPhone(updateRequest.getPhone());
        userUpdates.setBirthDate(updateRequest.getBirthDate());
        if (updateRequest.getGender() != null) {
            try {
                userUpdates.setGender(updateRequest.getGender());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid gender value in update request: {}", updateRequest.getGender());
            }
        } else {
            userUpdates.setGender(null);
        }

        User updatedUser = userService.updateUserProfile(userId, userUpdates);
        log.info("User profile updated by underlying userService for userId: {}", userId);

        Auth auth = updatedUser.getAuth();
        if (auth == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        List<String> roles = Collections.singletonList(auth.getRole().name());

        return UserAccountDetailsResponse.builder()
                .userId(updatedUser.getId())
                .userName(auth.getUsername())
                .email(auth.getEmail())
                .roles(roles)
                .name(updatedUser.getName())
                .phoneNumber(updatedUser.getPhone())
                .birthDate(updatedUser.getBirthDate())
                .gender(updatedUser.getGender() != null ? updatedUser.getGender() : null)
                .build();
    }
}
