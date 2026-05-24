package org.example.nutritionservice.service.recommendation;

import org.example.nutritionservice.domain.recommendation.MealKind;
import org.example.nutritionservice.dto.request.RecommendFullDayRequest;
import org.example.nutritionservice.dto.response.DailyPlanResponse;
import org.example.nutritionservice.entity.meallog.MealType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationApiServiceTest {

    private final RecommendationApiService recommendationApiService = new RecommendationApiService(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
    );

    @Test
    void returnWarningOnlyWhenObeseUserRequestsGainWithoutConfirmation() {
        RecommendFullDayRequest request = RecommendFullDayRequest.builder()
                .tdee(new BigDecimal("2000"))
                .goalCode("TANG")
                .planType("3_BUA")
                .constitution("BEO_PHI")
                .constitutionConfirmed(false)
                .perMealConfig(Map.of(
                        MealType.SANG, combo(),
                        MealType.TRUA, multiDish(),
                        MealType.TOI, multiDish()
                ))
                .build();

        DailyPlanResponse response = recommendationApiService.recommendFullDay("user-1", request);

        assertEquals("OBESE_BUT_GAIN_WEIGHT", response.getWarning().getCode());
        assertTrue(response.getWarning().isRequireConfirm());
        assertTrue(response.getMeals().isEmpty());
    }

    private RecommendFullDayRequest.PerMealConfigRequest combo() {
        return RecommendFullDayRequest.PerMealConfigRequest.builder()
                .mealKind(MealKind.COMBO)
                .build();
    }

    private RecommendFullDayRequest.PerMealConfigRequest multiDish() {
        return RecommendFullDayRequest.PerMealConfigRequest.builder()
                .mealKind(MealKind.NHIEU_MON)
                .nMain(1)
                .nRau(1)
                .nCarb(1)
                .build();
    }
}
