package org.example.healthdataservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.entity.enums.MeasurementFrequency;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthIndicatorConfigsDTO {
    private Long id;
    private IndicatorType indicatorType;
    private String displayName;
    private String unit;
    private MeasurementFrequency measurementFrequency;
    private boolean isActive;
}
