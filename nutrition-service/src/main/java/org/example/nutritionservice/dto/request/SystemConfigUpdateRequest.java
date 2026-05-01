package org.example.nutritionservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SystemConfigUpdateRequest {

    @NotNull
    private FilterConfig filter;

    @NotEmpty
    private List<SlotConstraint> constraints;

    @NotNull
    private DisplayConfig display;

    @Data
    public static class FilterConfig {
        @NotNull
        @DecimalMin("0.05")
        @DecimalMax("0.30")
        private BigDecimal kcalTolerance;

        @NotNull
        @DecimalMin("0.25")
        @DecimalMax("1.0")
        private BigDecimal servingMin;

        @NotNull
        @DecimalMin("1.0")
        @DecimalMax("3.0")
        private BigDecimal servingMax;

        @NotEmpty
        private List<BigDecimal> servingSteps;

        @NotEmpty
        private List<BigDecimal> comboServingSteps;
    }

    @Data
    public static class SlotConstraint {
        @NotBlank
        private String slotCode;

        @NotNull
        @Min(1)
        private Integer minG;

        @NotNull
        @Min(1)
        private Integer maxG;
    }

    @Data
    public static class DisplayConfig {
        @NotNull
        @Min(5)
        @Max(20)
        private Integer topK;

        @NotNull
        @Min(5)
        @Max(50)
        private Integer roundStepG;
    }
}
