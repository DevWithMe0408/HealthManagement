package org.example.nutritionservice.domain.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.entity.catalog.Dish;
import org.example.nutritionservice.entity.catalog.FoodGroup;
import org.example.nutritionservice.entity.catalog.SlotCode;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishCandidate {

    private Dish dish;
    private BigDecimal baseKcal;
    private BigDecimal baseProteinG;
    private BigDecimal baseFatG;
    private BigDecimal baseCarbG;

    public String getDishId() {
        return dish.getId();
    }

    public String getDishName() {
        return dish.getName();
    }

    public SlotCode getSlotCode() {
        return dish.getSlotCode();
    }

    public FoodGroup getFoodGroupCode() {
        return dish.getFoodGroupCode();
    }

    public BigDecimal getBaseServingG() {
        return BigDecimal.valueOf(dish.getBaseServingG());
    }
}
