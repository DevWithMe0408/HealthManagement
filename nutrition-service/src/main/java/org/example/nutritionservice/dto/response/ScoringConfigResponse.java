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
public class ScoringConfigResponse {

    private BigDecimal threshold;
    private SurplusFactors surplusFactors;
    private ReoptimizeParams reoptimize;
    private LocalDateTime updatedAt;
    private String updatedBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SurplusFactors {
        private BigDecimal protein;
        private BigDecimal fat;
        private BigDecimal carb;
        private BigDecimal kcal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReoptimizeParams {
        private Integer scoreThreshold;
        private Integer scoreDrop;
    }
}
