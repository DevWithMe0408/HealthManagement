package org.example.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGoalResponse {
    private String id;
    private String goalCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private BigDecimal targetWeightKg;
    private Integer targetDurationMonths;
    private String note;
}
