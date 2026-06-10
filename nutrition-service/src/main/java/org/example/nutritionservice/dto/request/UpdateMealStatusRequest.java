package org.example.nutritionservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.nutritionservice.entity.meallog.MealStatus;

@Data
public class UpdateMealStatusRequest {

    @NotNull
    private MealStatus status;

    @Size(max = 500)
    private String customNote;
}
