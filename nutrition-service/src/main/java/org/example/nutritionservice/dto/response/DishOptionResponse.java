package org.example.nutritionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.entity.catalog.FoodGroup;
import org.example.nutritionservice.entity.catalog.SlotCode;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishOptionResponse {

    private String dishId;
    private String dishName;
    private SlotCode slotCode;
    private FoodGroup foodGroupCode;
    private BigDecimal expectedScore;
    private BigDecimal expectedServing;
    private BigDecimal expectedActualGrams;
    private String unit;
    private Integer baseServingG;
    private boolean favorite;
}
