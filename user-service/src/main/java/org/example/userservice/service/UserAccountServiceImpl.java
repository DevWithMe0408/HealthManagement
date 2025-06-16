package org.example.userservice.service;

import org.example.userservice.dto.request.UserRequestDTO;
import org.example.userservice.dto.response.UserAccountDetailsResponse;
import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;
import org.example.userservice.enums.Gender;
import org.example.userservice.exception.ResourceNotFoundException;
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
    private UserRepository userRepository; // Vẫn cần để fetch user ban đầu nếu cần

    @Autowired
    private UserService userService; // Inject interface của UserServiceImpl

    @Autowired
    private AuthRepository authRepository; // Để build response

    @Override
    @Transactional
    public UserAccountDetailsResponse updateUserAccount(Long userId, UserRequestDTO updateRequest) {
        log.info("Processing account update for userId: {} via UserAccountService", userId);

        // Tạo một User entity từ UpdateUserAccountDetailsRequest để truyền vào phương thức hiện có
        // Hoặc UserServiceImpl.updateUserProfile nhận trực tiếp DTO và tự map
        User userUpdates = new User();
        // Không set ID ở đây, vì phương thức updateUserProfile sẽ dùng ID truyền vào để tìm existingUser
        userUpdates.setName(updateRequest.getName());
        userUpdates.setPhone(updateRequest.getPhone()); // Đảm bảo tên trường khớp
        userUpdates.setBirthDate(updateRequest.getBirthDate());
        if (updateRequest.getGender() != null ) {
            try {
                userUpdates.setGender(updateRequest.getGender());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid gender value in update request: {}", updateRequest.getGender());
                // Có thể bỏ qua hoặc ném lỗi tùy theo yêu cầu nghiệp vụ
            }
        } else {
            userUpdates.setGender(null);
        }
        // Không set age ở đây, để UserServiceImpl.updateUserProfile tự tính

        // Gọi phương thức updateUserProfile từ UserServiceImpl
        // Phương thức này đã chứa logic kiểm tra thay đổi và gửi event RabbitMQ
        User updatedUser = userService.updateUserProfile(userId, userUpdates);
        log.info("User profile updated by underlying userService for userId: {}", userId);

        // Lấy thông tin Auth để xây dựng response đầy đủ
        Auth auth = authRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Auth record not found for user id: " + userId));
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
