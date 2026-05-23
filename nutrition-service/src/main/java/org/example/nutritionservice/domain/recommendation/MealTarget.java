package org.example.nutritionservice.domain.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.entity.meallog.MealType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealTarget {
    /**
     * kcal/macro/slot target cho 1 bữa
     */

    private LocalDate mealDate;
    private MealType mealType;
    private BigDecimal mealKcal;
    private MacroTarget macroTarget;
    private Map<SlotCode, BigDecimal> slotKcalTargets;
    private PerMealConfig perMealConfig;
}
