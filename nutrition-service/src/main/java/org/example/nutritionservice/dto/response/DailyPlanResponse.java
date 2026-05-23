package org.example.nutritionservice.dto.response;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyPlanResponse {

    private LocalDate planDate;
    private String goalCode;
    private String planType;
    private WarningResponse warning;
    private List<@Valid MealSuggestionResponse> meals;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WarningResponse {
        private String level;
        private String code;
        private String message;
        private boolean requireConfirm;
    }
}
