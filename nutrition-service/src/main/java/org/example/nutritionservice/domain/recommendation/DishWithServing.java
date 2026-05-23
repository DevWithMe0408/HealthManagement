package org.example.nutritionservice.domain.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishWithServing {
    /**
     * Dishcandidate + Serving multiplier
     */

    private DishCandidate candidate;
    private BigDecimal servingMultiplier;
    private BigDecimal actualGrams;
    private BigDecimal kcal;
    private BigDecimal proteinG;
    private BigDecimal fatG;
    private BigDecimal carbG;
}
