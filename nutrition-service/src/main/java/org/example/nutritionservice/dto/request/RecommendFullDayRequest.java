package org.example.nutritionservice.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import org.example.nutritionservice.domain.recommendation.MealKind;
import org.example.nutritionservice.entity.meallog.MealType;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendFullDayRequest {

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

    @NotEmpty
    private Map<MealType, @Valid PerMealConfigRequest> perMealConfig;

    @Builder.Default
    private boolean forceCompute = false;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PerMealConfigRequest {

        @NotNull
        private MealKind mealKind;

        @JsonProperty("nMain")
        private Integer nMain;

        @JsonProperty("nRau")
        private Integer nRau;

        @JsonProperty("nCarb")
        private Integer nCarb;
    }
}
