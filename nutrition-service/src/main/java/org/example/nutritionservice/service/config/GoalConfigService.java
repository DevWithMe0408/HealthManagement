package org.example.nutritionservice.service.config;

import org.example.nutritionservice.dto.request.GoalConfigUpdateRequest;
import org.example.nutritionservice.dto.response.GoalConfigResponse;

import java.util.List;

public interface GoalConfigService {
    List<GoalConfigResponse> getAll();
    GoalConfigResponse getByCode(String goalCode);
    GoalConfigResponse update(String goalCode, GoalConfigUpdateRequest request, String updatedBy);
}
