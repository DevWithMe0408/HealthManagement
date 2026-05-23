package org.example.nutritionservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.dto.response.MealCombinationResponse;
import org.example.nutritionservice.entity.meallog.MealType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmMealRequest {

    @NotNull
    private LocalDate mealDate;

    @NotNull
    private MealType mealType;

    @NotNull
    @Pattern(regexp = "3_BUA|5_BUA")
    private String planType;

    @NotNull
    @Pattern(regexp = "GIAM|DUY_TRI|TANG")
    private String goalCode;

    @NotNull
    @Positive
    private BigDecimal mealKcalTarget;

    @NotNull
    @Valid
    private MealCombinationResponse selectedCombination;
}
