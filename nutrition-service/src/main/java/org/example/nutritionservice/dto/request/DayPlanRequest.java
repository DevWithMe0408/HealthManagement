package org.example.nutritionservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.entity.meallog.MealType;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayPlanRequest {

    @NotNull
    @DecimalMin("1000")
    @DecimalMax("5000")
    private BigDecimal tdee;

    @NotNull
    @Pattern(regexp = "GIAM|DUY_TRI|TANG")
    private String goalCode;

    @NotNull
    @Pattern(regexp = "3_BUA|5_BUA")
    private String planType;

    @NotNull
    @Pattern(regexp = "GAY|CAN_DOI|THUA_CAN|BEO_PHI")
    private String constitution;

    @Builder.Default
    private boolean constitutionConfirmed = false;

    @Valid
    @NotEmpty
    private Map<MealType, RecommendFullDayRequest.PerMealConfigRequest> perMealConfig;

    @Pattern(regexp = "TODAY|TOMORROW")
    private String planDay;

    @Builder.Default
    private boolean forceRegenerate = false;

    @Builder.Default
    private boolean forceCompute = false;
}
