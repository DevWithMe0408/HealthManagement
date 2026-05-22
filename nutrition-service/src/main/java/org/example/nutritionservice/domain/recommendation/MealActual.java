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
public class MealActual {

    private BigDecimal proteinG;
    private BigDecimal fatG;
    private BigDecimal carbG;
    private BigDecimal kcal;
}
