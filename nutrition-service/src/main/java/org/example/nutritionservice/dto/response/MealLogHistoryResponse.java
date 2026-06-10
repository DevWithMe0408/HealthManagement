package org.example.nutritionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.entity.meallog.MealStatus;
import org.example.nutritionservice.entity.meallog.MealType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealLogHistoryResponse {

    private String id;
    private LocalDate mealDate;
    private MealType mealType;
    private String planType;
    private String goalCode;
    private BigDecimal mealKcalTarget;
    private BigDecimal totalKcalActual;
    private BigDecimal totalProtein;
    private BigDecimal totalFat;
    private BigDecimal totalCarb;
    private BigDecimal finalScore;
    private MealStatus status;
    private String customNote;
    private List<DishSuggestionResponse> dishes;
}
