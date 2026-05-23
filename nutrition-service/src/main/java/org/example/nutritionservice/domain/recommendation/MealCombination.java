package org.example.nutritionservice.domain.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealCombination {
    /**
     * tổ hợp dishes + servings + scores
     */

    private List<DishWithServing> dishes;
    private MealActual actual;
    private BigDecimal macroScore;
    private BigDecimal penalty;
    private BigDecimal finalScore;
}
