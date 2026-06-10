package org.example.nutritionservice.service.meallog;

import org.example.nutritionservice.dto.request.ConfirmMealRequest;
import org.example.nutritionservice.dto.response.DishSuggestionResponse;
import org.example.nutritionservice.dto.response.MealCombinationResponse;
import org.example.nutritionservice.entity.catalog.FoodGroup;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.entity.meallog.MealLog;
import org.example.nutritionservice.entity.meallog.MealStatus;
import org.example.nutritionservice.entity.meallog.MealType;
import org.example.nutritionservice.repository.catalog.DishRepository;
import org.example.nutritionservice.repository.meallog.MealLogDishRepository;
import org.example.nutritionservice.repository.meallog.MealLogRepository;
import org.example.web.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MealLogServiceTest {

    private final MealLogRepository mealLogRepository = mock(MealLogRepository.class);
    private final MealLogDishRepository mealLogDishRepository = mock(MealLogDishRepository.class);
    private final DishRepository dishRepository = mock(DishRepository.class);
    private final MealLogService mealLogService = new MealLogService(
            mealLogRepository,
            mealLogDishRepository,
            dishRepository
    );

    @Test
    void replaceDishRowsWhenConfirmingExistingMeal() {
        MealLog existing = MealLog.builder()
                .id("meal-1")
                .status(MealStatus.FOLLOWED)
                .build();
        when(mealLogRepository.findByUserIdAndMealDateAndMealType(
                "user-1",
                LocalDate.of(2026, 5, 22),
                MealType.TRUA
        )).thenReturn(Optional.of(existing));
        when(mealLogRepository.save(any(MealLog.class))).thenReturn(existing);

        MealLog saved = mealLogService.confirmMeal("user-1", request());

        assertEquals("GIAM", existing.getGoalCode());
        assertEquals(MealStatus.FOLLOWED, existing.getStatus());
        assertEquals(existing, saved);
        verify(mealLogDishRepository).deleteByMealLogId("meal-1");
        verify(mealLogDishRepository).saveAll(any());
    }

    @Test
    void updateStatusStoresCustomNoteOnlyForCustomStatus() {
        MealLog existing = MealLog.builder()
                .id("meal-1")
                .userId("user-1")
                .status(MealStatus.SUGGESTED)
                .build();
        when(mealLogRepository.findById("meal-1")).thenReturn(Optional.of(existing));
        when(mealLogRepository.save(existing)).thenReturn(existing);

        MealLog custom = mealLogService.updateStatus("user-1", "meal-1", MealStatus.CUSTOM, "pho bo");
        assertEquals(MealStatus.CUSTOM, custom.getStatus());
        assertEquals("pho bo", custom.getCustomNote());

        MealLog followed = mealLogService.updateStatus("user-1", "meal-1", MealStatus.FOLLOWED, "ignored");
        assertEquals(MealStatus.FOLLOWED, followed.getStatus());
        assertNull(followed.getCustomNote());
    }

    @Test
    void updateStatusRejectsMealFromAnotherUser() {
        MealLog existing = MealLog.builder()
                .id("meal-1")
                .userId("user-2")
                .status(MealStatus.SUGGESTED)
                .build();
        when(mealLogRepository.findById("meal-1")).thenReturn(Optional.of(existing));

        assertThrows(BusinessException.class,
                () -> mealLogService.updateStatus("user-1", "meal-1", MealStatus.SKIPPED, null));
        verify(mealLogRepository, never()).save(any());
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
