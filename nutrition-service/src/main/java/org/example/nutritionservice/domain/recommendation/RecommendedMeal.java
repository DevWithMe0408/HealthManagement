package org.example.nutritionservice.domain.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.entity.meallog.MealStatus;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedMeal {

    private MealTarget mealTarget;
    private Map<SlotCode, List<DishCandidate>> candidatesPerSlot;
    private List<MealCombination> combinations;
    private MealStatus status;
    private String mealLogId;
}
