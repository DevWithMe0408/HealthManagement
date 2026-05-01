package org.example.nutritionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalConfigResponse {
    private String goalCode;
    private BigDecimal calMultiplier;
    private BigDecimal proteinRatio;
    private BigDecimal fatRatio;
    private BigDecimal carbRatio;
    private BigDecimal slotMainRatio;
    private BigDecimal slotVegRatio;
    private BigDecimal slotCarbRatio;
    private BigDecimal weightP;
    private BigDecimal weightF;
    private BigDecimal weightC;
    private BigDecimal weightKcal;
    private String description;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
