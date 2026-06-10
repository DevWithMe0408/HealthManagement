package org.example.nutritionservice.domain.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.entity.meallog.MealType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserContext {
    /**
     * input của 1 lần dề xuất
     */

    private String userId;
    private BigDecimal tdee;
    private String goalCode;
    private String planType;
    private Map<MealType, PerMealConfig> perMealConfigs;
    private LocalDateTime requestTime;
    private boolean forceCompute;
    private String planDay;
}
