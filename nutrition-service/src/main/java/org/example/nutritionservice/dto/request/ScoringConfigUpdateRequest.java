package org.example.nutritionservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoringConfigUpdateRequest {

    @NotNull
    @DecimalMin("0.05")
    @DecimalMax("0.50")
    private BigDecimal threshold;

    @NotNull
    private SurplusFactors surplusFactors;

    @NotNull
    private ReoptimizeParams reoptimize;

    @Data
    public static class SurplusFactors {
        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private BigDecimal protein;

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private BigDecimal fat;

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private BigDecimal carb;

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private BigDecimal kcal;
    }

    @Data
    public static class ReoptimizeParams {
        @NotNull
        @Min(0)
        @Max(100)
        private Integer scoreThreshold;

        @NotNull
        @Min(0)
        @Max(100)
        private Integer scoreDrop;
    }
}
