package org.example.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.request.PreferenceUpdateRequest;
import org.example.userservice.dto.response.PreferenceResponse;
import org.example.userservice.service.UserPreferenceService;
import org.example.web.dto.response.DataResponse;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService service;

    @GetMapping
    public ResponseEntity<DataResponse<List<PreferenceResponse>>> getAll(
            @RequestHeader("userId") String userId) {
        return ResponseEntity.ok(DataResponse.success(service.getAll(userId)));
    }

    @GetMapping("/{prefKey}")
    public ResponseEntity<DataResponse<PreferenceResponse>> getOne(
            @RequestHeader("userId") String userId,
            @PathVariable String prefKey) {
        PreferenceResponse data = service.getOne(userId, prefKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));
        return ResponseEntity.ok(DataResponse.success(data));
    }

    @PutMapping("/{prefKey}")
    public ResponseEntity<DataResponse<PreferenceResponse>> upsert(
            @RequestHeader("userId") String userId,
            @PathVariable String prefKey,
            @Valid @RequestBody PreferenceUpdateRequest req) {
        return ResponseEntity.ok(DataResponse.success(service.upsert(userId, prefKey, req)));
    }

    @DeleteMapping("/{prefKey}")
    public ResponseEntity<DataResponse<Void>> delete(
            @RequestHeader("userId") String userId,
            @PathVariable String prefKey) {
        service.delete(userId, prefKey);
        return ResponseEntity.ok(DataResponse.success());
    }
}
