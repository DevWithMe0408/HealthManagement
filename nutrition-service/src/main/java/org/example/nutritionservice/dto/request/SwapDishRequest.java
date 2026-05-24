package org.example.nutritionservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.dto.response.DailyPlanResponse;
import org.example.nutritionservice.entity.meallog.MealType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwapDishRequest {

    @NotNull
    @Valid
    private DailyPlanResponse currentPlan;

    @NotNull
    private MealType mealType;

    @NotBlank
    private String swappedSlot;

    @NotBlank
    private String newDishId;

    @Valid
    private List<PinnedDish> pinnedDishes;
}
