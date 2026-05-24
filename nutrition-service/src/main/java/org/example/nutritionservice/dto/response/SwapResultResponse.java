package org.example.nutritionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwapResultResponse {

    private MealSuggestionResponse updatedMeal;
    private BigDecimal newFinalScore;
    private BigDecimal originalFinalScore;
    private boolean scoreDropTriggered;
    private SwapSuggestion suggestion;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SwapSuggestion {
        private String message;
        private String targetSlotKey;
        private String suggestedDishId;
        private BigDecimal suggestedScore;
    }
}
