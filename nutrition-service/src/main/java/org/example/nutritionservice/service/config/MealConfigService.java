package org.example.nutritionservice.service.config;

import org.example.nutritionservice.dto.request.MealConfigUpdateRequest;
import org.example.nutritionservice.dto.response.MealConfigResponse;
import org.example.nutritionservice.dto.response.MealRatioItemResponse;

import java.util.List;

public interface MealConfigService {
    MealConfigResponse getAll();
    List<MealRatioItemResponse> update(String planType, MealConfigUpdateRequest request, String updatedBy);
}
