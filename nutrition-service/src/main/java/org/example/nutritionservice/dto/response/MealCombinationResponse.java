package org.example.nutritionservice.dto.response;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class MealCombinationResponse {

    @NotNull
    private BigDecimal totalKcal;

    @NotNull
    private BigDecimal totalProtein;

    @NotNull
    private BigDecimal totalFat;

    @NotNull
    private BigDecimal totalCarb;

    @NotNull
    private BigDecimal macroScore;

    @NotNull
    private BigDecimal penalty;

    @NotNull
    private BigDecimal finalScore;

    @NotEmpty
    private List<@Valid DishSuggestionResponse> dishes;
}
