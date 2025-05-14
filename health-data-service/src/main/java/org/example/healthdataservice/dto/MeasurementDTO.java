package org.example.healthdataservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementDTO {
    private Long id;

    @NotNull(message = "userId không được null")
    @Positive(message = "userId phải lớn hơn 0")
    private Long userId;

    @NotNull(message = "measurementTime không được bỏ trống")
    private LocalDateTime measurementTime;

    @Min(value = 1900)
    @Max(value = 2100)
    private Integer year;

    @Min(1)
    @Max(12)
    private Integer month;

    @Min(1)
    @Max(53)
    private Integer weekNumber;

    private LocalDateTime updatedAt;
}
