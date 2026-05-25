package org.example.userservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.userservice.enums.GoalCode;

import java.math.BigDecimal;

@Data
public class UpdateGoalRequest {

    @NotNull
    private GoalCode goalCode;

    private BigDecimal targetWeightKg;

    @Min(1)
    @Max(24)
    private Integer targetDurationMonths;

    private String note;
}
