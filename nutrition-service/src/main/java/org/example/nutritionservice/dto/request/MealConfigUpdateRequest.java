package org.example.nutritionservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MealConfigUpdateRequest {

    @NotEmpty
    private List<MealItem> meals;

    @Data
    public static class MealItem {
        @NotBlank
        private String mealCode;

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("1.00")
        private BigDecimal ratio;
    }
}
