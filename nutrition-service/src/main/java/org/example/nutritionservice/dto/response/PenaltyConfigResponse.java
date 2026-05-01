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
public class PenaltyConfigResponse {

    private LayerPenalty layer1;
    private LayerPenalty layer2;
    private SlotFactors slotFactors;
    private OtherParams others;
    private LocalDateTime updatedAt;
    private String updatedBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LayerPenalty {
        private Integer sameDay;
        private Integer oneDayBefore;
        private Integer twoDayBefore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotFactors {
        private BigDecimal main;
        private BigDecimal veg;
        private BigDecimal carb;
        private BigDecimal combo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherParams {
        private Integer penaltyCap;
        private BigDecimal favoriteDiscount;
        private Integer lookbackDays;
    }
}
