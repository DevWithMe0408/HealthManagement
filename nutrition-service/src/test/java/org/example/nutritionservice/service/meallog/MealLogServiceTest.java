package org.example.nutritionservice.service.meallog;

import org.example.nutritionservice.dto.request.ConfirmMealRequest;
import org.example.nutritionservice.dto.response.DishSuggestionResponse;
import org.example.nutritionservice.dto.response.MealCombinationResponse;
import org.example.nutritionservice.entity.catalog.FoodGroup;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.entity.meallog.MealLog;
import org.example.nutritionservice.entity.meallog.MealType;
import org.example.nutritionservice.repository.meallog.MealLogDishRepository;
import org.example.nutritionservice.repository.meallog.MealLogRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MealLogServiceTest {

    private final MealLogRepository mealLogRepository = mock(MealLogRepository.class);
    private final MealLogDishRepository mealLogDishRepository = mock(MealLogDishRepository.class);
    private final MealLogService mealLogService = new MealLogService(mealLogRepository, mealLogDishRepository);

    @Test
    void replaceDishRowsWhenConfirmingExistingMeal() {
        MealLog existing = MealLog.builder().id("meal-1").build();
        when(mealLogRepository.findByUserIdAndMealDateAndMealType(
                "user-1",
                LocalDate.of(2026, 5, 22),
                MealType.TRUA
        )).thenReturn(Optional.of(existing));
        when(mealLogRepository.save(any(MealLog.class))).thenReturn(existing);

        MealLog saved = mealLogService.confirmMeal("user-1", request());

        assertEquals("GIAM", existing.getGoalCode());
        assertEquals(existing, saved);
        verify(mealLogDishRepository).deleteByMealLogId("meal-1");
        verify(mealLogDishRepository).saveAll(any());
    }

    private ConfirmMealRequest request() {
        return ConfirmMealRequest.builder()
                .mealDate(LocalDate.of(2026, 5, 22))
                .mealType(MealType.TRUA)
                .planType("3_BUA")
                .goalCode("GIAM")
                .mealKcalTarget(new BigDecimal("640.00"))
                .selectedCombination(MealCombinationResponse.builder()
                        .totalKcal(new BigDecimal("635.00"))
                        .totalProtein(new BigDecimal("40.00"))
                        .totalFat(new BigDecimal("20.00"))
                        .totalCarb(new BigDecimal("70.00"))
                        .macroScore(new BigDecimal("90.00"))
                        .penalty(BigDecimal.ZERO)
                        .finalScore(new BigDecimal("90.00"))
                        .dishes(List.of(DishSuggestionResponse.builder()
                                .dishId("dish-1")
                                .slotCode(SlotCode.CHINH)
                                .foodGroupCode(FoodGroup.GIA_CAM)
                                .servingMultiplier(BigDecimal.ONE)
                                .actualGrams(new BigDecimal("100.00"))
                                .dishKcal(new BigDecimal("215.00"))
                                .build()))
                        .build())
                .build();
    }
}
