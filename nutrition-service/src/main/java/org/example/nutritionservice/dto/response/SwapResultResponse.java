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
    private ServingSuggestionResponse suggestion;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServingSuggestionResponse {
        private String message;
        private String targetSlotIdInMeal;
        private BigDecimal suggestedServingMultiplier;
        private BigDecimal suggestedNewScore;
    }
}
