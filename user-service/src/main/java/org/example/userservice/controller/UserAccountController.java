package org.example.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.request.UserRequestDTO;
import org.example.web.dto.response.DataResponse;
import org.example.userservice.dto.response.UserAccountDetailsResponse;
import org.example.userservice.service.UserAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    @PutMapping("/update-account-details")
    public ResponseEntity<DataResponse<UserAccountDetailsResponse>> updateAccountDetails(
            @RequestHeader("userId") String userIdFromGateway,
            @Valid @RequestBody UserRequestDTO request
    ) {
        UserAccountDetailsResponse updatedUserDetails = userAccountService.updateUserAccount(userIdFromGateway, request);
        return ResponseEntity.ok(DataResponse.success(updatedUserDetails));
    }
}
