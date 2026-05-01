package org.example.nutritionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigResponse {

    private FilterConfig filter;
    private List<SlotConstraint> constraints;
    private DisplayConfig display;
    private LocalDateTime updatedAt;
    private String updatedBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterConfig {
        private BigDecimal kcalTolerance;
        private BigDecimal servingMin;
        private BigDecimal servingMax;
        private List<BigDecimal> servingSteps;
        private List<BigDecimal> comboServingSteps;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotConstraint {
        private String slotCode;
        private Integer minG;
        private Integer maxG;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisplayConfig {
        private Integer topK;
        private Integer roundStepG;
    }
}
