package org.example.nutritionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealConfigResponse {
    private List<MealRatioItemResponse> plan3Meals;
    private List<MealRatioItemResponse> plan5Meals;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
