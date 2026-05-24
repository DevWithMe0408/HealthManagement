package org.example.nutritionservice.domain.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.entity.catalog.FoodGroup;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotAlternative {

    private DishCandidate candidate;
    private BigDecimal expectedScore;
    private BigDecimal expectedServing;
    private BigDecimal expectedActualGrams;

    public FoodGroup getFoodGroupCode() {
        return candidate.getFoodGroupCode();
    }

    public String getDishId() {
        return candidate.getDishId();
    }
}
