package org.example.nutritionservice.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.request.GoalConfigUpdateRequest;
import org.example.nutritionservice.dto.request.MealConfigUpdateRequest;
import org.example.nutritionservice.dto.request.PenaltyConfigUpdateRequest;
import org.example.nutritionservice.dto.request.ScoringConfigUpdateRequest;
import org.example.nutritionservice.dto.request.SystemConfigUpdateRequest;
import org.example.nutritionservice.dto.response.GoalConfigResponse;
import org.example.nutritionservice.dto.response.MealConfigResponse;
import org.example.nutritionservice.dto.response.MealRatioItemResponse;
import org.example.nutritionservice.dto.response.PenaltyConfigResponse;
import org.example.nutritionservice.dto.response.ScoringConfigResponse;
import org.example.nutritionservice.dto.response.SystemConfigResponse;
import org.example.nutritionservice.service.config.GoalConfigService;
import org.example.nutritionservice.service.config.MealConfigService;
import org.example.nutritionservice.service.config.PenaltyConfigService;
import org.example.nutritionservice.service.config.ScoringConfigService;
import org.example.nutritionservice.service.config.SystemConfigService;
import org.example.web.dto.response.DataResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/configs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminConfigController {

    private final GoalConfigService goalConfigService;
    private final MealConfigService mealConfigService;
    private final PenaltyConfigService penaltyConfigService;
    private final ScoringConfigService scoringConfigService;
    private final SystemConfigService systemConfigService;

    // ============================================================
    // ===== GOAL CONFIG (3 endpoints)
    // ============================================================

    @GetMapping("/goals")
    public DataResponse<List<GoalConfigResponse>> getAllGoalConfigs() {
        return DataResponse.success(goalConfigService.getAll());
    }

    @GetMapping("/goals/{goalCode}")
    public DataResponse<GoalConfigResponse> getGoalConfig(@PathVariable String goalCode) {
        return DataResponse.success(goalConfigService.getByCode(goalCode));
    }

    @PutMapping("/goals/{goalCode}")
    public DataResponse<GoalConfigResponse> updateGoalConfig(
            @PathVariable String goalCode,
            @RequestBody @Valid GoalConfigUpdateRequest request,
            Authentication auth) {
        String updatedBy = auth != null ? auth.getName() : null;
        return DataResponse.success(goalConfigService.update(goalCode, request, updatedBy));
    }

    // ============================================================
    // ===== MEAL CONFIG (2 endpoints)
    // ============================================================

    @GetMapping("/meals")
    public DataResponse<MealConfigResponse> getMealConfigs() {
        return DataResponse.success(mealConfigService.getAll());
    }

    @PutMapping("/meals/{planType}")
    public DataResponse<List<MealRatioItemResponse>> updateMealConfig(
            @PathVariable String planType,
            @RequestBody @Valid MealConfigUpdateRequest request,
            Authentication auth) {
        String updatedBy = auth != null ? auth.getName() : null;
        return DataResponse.success(mealConfigService.update(planType, request, updatedBy));
    }

    // ============================================================
    // ===== PENALTY CONFIG (2 endpoints)
    // ============================================================

    @GetMapping("/penalties")
    public DataResponse<PenaltyConfigResponse> getPenaltyConfig() {
        return DataResponse.success(penaltyConfigService.get());
    }

    @PutMapping("/penalties")
    public DataResponse<PenaltyConfigResponse> updatePenaltyConfig(
            @RequestBody @Valid PenaltyConfigUpdateRequest request,
            Authentication auth) {
        String updatedBy = auth != null ? auth.getName() : null;
        return DataResponse.success(penaltyConfigService.update(request, updatedBy));
    }

    // ============================================================
    // ===== SCORING CONFIG (2 endpoints)
    // ============================================================

    @GetMapping("/scoring")
    public DataResponse<ScoringConfigResponse> getScoringConfig() {
        return DataResponse.success(scoringConfigService.get());
    }

    @PutMapping("/scoring")
    public DataResponse<ScoringConfigResponse> updateScoringConfig(
            @RequestBody @Valid ScoringConfigUpdateRequest request,
            Authentication auth) {
        String updatedBy = auth != null ? auth.getName() : null;
        return DataResponse.success(scoringConfigService.update(request, updatedBy));
    }

    // ============================================================
    // ===== SYSTEM CONFIG (2 endpoints)
    // ============================================================

    @GetMapping("/system")
    public DataResponse<SystemConfigResponse> getSystemConfig() {
        return DataResponse.success(systemConfigService.get());
    }

    @PutMapping("/system")
    public DataResponse<SystemConfigResponse> updateSystemConfig(
            @RequestBody @Valid SystemConfigUpdateRequest request,
            Authentication auth) {
        String updatedBy = auth != null ? auth.getName() : null;
        return DataResponse.success(systemConfigService.update(request, updatedBy));
    }
}
