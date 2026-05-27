package org.example.nutritionservice.dto.response;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.entity.meallog.MealType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealSuggestionResponse {

    @NotNull
    private MealType mealType;

    @NotNull
    private BigDecimal mealKcalTarget;

    @NotNull
    private BigDecimal kcalTarget;

    @NotNull
    private BigDecimal proteinTarget;

    @NotNull
    private BigDecimal fatTarget;

    @NotNull
    private BigDecimal carbTarget;

    @Valid
    private MealCombinationResponse topCombination;

    private Map<String, List<@Valid DishOptionResponse>> slotAlternatives;
}
