package org.example.nutritionservice.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class DishSuggestionResponse {

    private String slotKey;

    @NotBlank
    private String dishId;

    private String dishName;

    @NotNull
    private SlotCode slotCode;

    @NotNull
    private FoodGroup foodGroupCode;

    @NotNull
    @Positive
    private BigDecimal servingMultiplier;

    @NotNull
    @Positive
    private BigDecimal actualGrams;

    @NotNull
    @Positive
    private BigDecimal dishKcal;

    private String unit;
    private Integer baseServingG;
    private boolean favorite;
}
