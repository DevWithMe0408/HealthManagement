package org.example.healthdataservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetricsDTO {
    private Long id;

    @NotNull(message = "Giá trị chỉ số không được để trống")
    @Min(value = 0, message = "Giá trị chỉ số phải lớn hơn hoặc bằng 0")
    private Double value;

    @NotNull(message = "ID của measurement không được để trống")
    private Long measurementId;

    @NotNull(message = "ID của indicatorConfig không được để trống")
    private Long indicatorConfigId;
}
