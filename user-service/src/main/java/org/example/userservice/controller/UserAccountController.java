package org.example.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.request.UserRequestDTO;
import org.example.userservice.dto.response.UserAccountDetailsResponse;
import org.example.userservice.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserAccountController {
    @Autowired
    private final UserAccountService userAccountService;

    @PutMapping("/update-account-details")
    public ResponseEntity<?> updateAccountDetails(
            @RequestHeader("userId") String userIdFromGateway,
            @Valid @RequestBody UserRequestDTO request
    ) {
        Long userId;
        try {
            userId = Long.parseLong(userIdFromGateway);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid User ID format in header.");
        }

        try {
            UserAccountDetailsResponse updatedUserDetails = userAccountService.updateUserAccount(userId, request);
            return ResponseEntity.ok(updatedUserDetails);
        } catch (RuntimeException e) { // Bắt các exception cụ thể hơn nếu có (ví dụ: UserNotFoundException)
            // Log lỗi ở service layer
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
        // Các lỗi validation từ @Valid sẽ được Spring xử lý tự động và trả về 400 BAD REQUEST
        // Bạn có thể custom xử lý lỗi validation bằng @ControllerAdvice nếu muốn.
    }

}
