package org.example.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.request.UpdateGoalRequest;
import org.example.userservice.dto.response.UserGoalResponse;
import org.example.userservice.service.UserGoalService;
import org.example.web.dto.response.DataResponse;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-goals")
@RequiredArgsConstructor
public class UserGoalController {

    private final UserGoalService service;

    @GetMapping("/current")
    public ResponseEntity<DataResponse<UserGoalResponse>> getCurrent(
            @RequestHeader("userId") String userId) {
        UserGoalResponse data = service.getCurrent(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOAL_NO_ACTIVE));
        return ResponseEntity.ok(DataResponse.success(data));
    }

    @PutMapping("/current")
    public ResponseEntity<DataResponse<UserGoalResponse>> updateCurrent(
            @RequestHeader("userId") String userId,
            @Valid @RequestBody UpdateGoalRequest req) {
        return ResponseEntity.ok(DataResponse.success(service.updateCurrent(userId, req)));
    }

    @GetMapping("/history")
    public ResponseEntity<DataResponse<List<UserGoalResponse>>> getHistory(
            @RequestHeader("userId") String userId) {
        return ResponseEntity.ok(DataResponse.success(service.getHistory(userId)));
    }
}
