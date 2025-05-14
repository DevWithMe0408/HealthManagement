package org.example.healthdataservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitHealthMetricRequest {
    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "indicatorType is required")
    private String indicatorType;

    @DecimalMin(value = "0.0", inclusive = false, message = "value must be positive")
    private double value;
}
