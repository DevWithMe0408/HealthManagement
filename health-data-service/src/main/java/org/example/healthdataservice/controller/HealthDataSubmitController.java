package org.example.healthdataservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.dto.request.SubmitHealthDataRequest;
import org.example.healthdataservice.service.HealthDataSubmitServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/health-data")
@RequiredArgsConstructor
public class HealthDataSubmitController {

    private final HealthDataSubmitServiceImpl healthDataSubmitServiceImpl;


    @PostMapping("/submit")
    public ResponseEntity<String> submitHealthData(@Valid @RequestBody SubmitHealthDataRequest request) {
        try {
            healthDataSubmitServiceImpl.submitBasicMetrics(request);
            return ResponseEntity.ok("Đã lưu dữ liệu và tính toán chỉ số thành công.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

}
