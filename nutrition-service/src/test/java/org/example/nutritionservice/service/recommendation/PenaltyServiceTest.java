package org.example.nutritionservice.service.recommendation;

import org.example.nutritionservice.domain.recommendation.DishCandidate;
import org.example.nutritionservice.domain.recommendation.HistoryEntry;
import org.example.nutritionservice.domain.recommendation.LoadedConfigs;
import org.example.nutritionservice.entity.catalog.Dish;
import org.example.nutritionservice.entity.catalog.FoodGroup;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.entity.config.SlotConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PenaltyServiceTest {

    private final PenaltyService penaltyService = new PenaltyService();

    @Test
    void applyLayerOnePenaltyForSameDayMainDish() {
        Dish dish = Dish.builder()
                .id("dish-1")
                .slotCode(SlotCode.CHINH)
                .foodGroupCode(FoodGroup.GIA_CAM)
                .build();
        DishCandidate candidate = DishCandidate.builder().dish(dish).build();
        LocalDate targetDate = LocalDate.of(2026, 5, 22);
        HistoryEntry historyEntry = HistoryEntry.builder()
                .mealDate(targetDate)
                .dishId("dish-1")
                .slotCode(SlotCode.CHINH)
                .foodGroupCode(FoodGroup.GIA_CAM)
                .build();

        SlotConfig mainSlot = new SlotConfig();
        mainSlot.setSlotCode("CHINH");
        mainSlot.setSlotFactor(new BigDecimal("1.0"));
        LoadedConfigs configs = LoadedConfigs.builder()
                .slotConfigs(Map.of(SlotCode.CHINH, mainSlot))
                .penaltyConfigs(Map.of(
                        1, Map.of(0, 12, 1, 6, 2, 3),
                        2, Map.of(0, 6, 1, 3, 2, 1)
                ))
                .systemConfigs(Map.of(
                        "penalty.cap", "40",
                        "penalty.fav_discount", "0.5"
                ))
                .build();

        BigDecimal penalty = penaltyService.computePenalty(
                List.of(candidate),
                List.of(historyEntry),
                Set.of(),
                configs,
                targetDate
        );

        assertEquals(new BigDecimal("12.00"), penalty);
    }
}
