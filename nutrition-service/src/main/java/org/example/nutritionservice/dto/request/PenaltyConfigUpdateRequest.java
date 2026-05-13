package org.example.nutritionservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PenaltyConfigUpdateRequest {

    @NotNull
    private LayerPenalty layer1;

    @NotNull
    private LayerPenalty layer2;

    @NotNull
    private SlotFactors slotFactors;

    @NotNull
    private OtherParams others;

    @Data
    public static class LayerPenalty {
        @NotNull
        @Min(0)
        private Integer sameDay;

        @NotNull
        @Min(0)
        private Integer oneDayBefore;

        @NotNull
        @Min(0)
        private Integer twoDayBefore;
    }

    @Data
    public static class SlotFactors {
        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private BigDecimal main;

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private BigDecimal veg;

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private BigDecimal carb;

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private BigDecimal combo;
    }

    @Data
    public static class OtherParams {
        @NotNull
        @Min(10)
        private Integer penaltyCap;

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private BigDecimal favoriteDiscount;

        @NotNull
        @Min(1)
        private Integer lookbackDays;
    }
}
