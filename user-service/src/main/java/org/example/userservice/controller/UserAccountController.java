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
        try {
            UserAccountDetailsResponse updatedUserDetails = userAccountService.updateUserAccount(userIdFromGateway, request);
            return ResponseEntity.ok(updatedUserDetails);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
