package org.example.nutritionservice.service.dashboard;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.response.CatalogStatsResponse;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.repository.catalog.DishRepository;
import org.example.nutritionservice.repository.catalog.IngredientRepository;
import org.example.nutritionservice.repository.meallog.MealLogRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private final DishRepository dishRepository;
    private final IngredientRepository ingredientRepository;
    private final MealLogRepository mealLogRepository;

    public CatalogStatsResponse getCatalogStats() {
        Map<String, Long> dishCountBySlot = new LinkedHashMap<>();
        for (SlotCode slotCode : SlotCode.values()) {
            dishCountBySlot.put(slotCode.name(), 0L);
        }

        for (Object[] row : dishRepository.countActiveBySlot()) {
            SlotCode slotCode = (SlotCode) row[0];
            long count = ((Number) row[1]).longValue();
            dishCountBySlot.put(slotCode.name(), count);
        }

        return CatalogStatsResponse.builder()
                .dishTotal(dishRepository.count())
                .dishActive(dishRepository.countByIsActiveTrue())
                .ingredientTotal(ingredientRepository.count())
                .ingredientWithMacro(ingredientRepository.countByKcalPer100gIsNotNull())
                .mealLogTotal(mealLogRepository.count())
                .dishCountBySlot(dishCountBySlot)
                .build();
    }
}
