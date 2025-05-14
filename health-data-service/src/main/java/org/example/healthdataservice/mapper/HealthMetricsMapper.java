package org.example.healthdataservice.mapper;

import org.example.healthdataservice.dto.HealthMetricsDTO;
import org.example.healthdataservice.entity.HealthIndicatorConfigs;
import org.example.healthdataservice.entity.HealthMetrics;
import org.example.healthdataservice.entity.Measurement;
import org.springframework.stereotype.Component;

@Component
public class HealthMetricsMapper {

    public HealthMetricsDTO toDto(HealthMetrics entity) {
        HealthMetricsDTO dto = new HealthMetricsDTO();
        dto.setId(entity.getId());
        dto.setValue(entity.getValue());
        dto.setMeasurementId(entity.getMeasurement().getId());
        dto.setIndicatorConfigId(entity.getIndicatorConfig().getId());
        return dto;
    }

    public HealthMetrics toEntity(HealthMetricsDTO dto, Measurement measurement, HealthIndicatorConfigs config) {
        HealthMetrics entity = new HealthMetrics();
        entity.setId(dto.getId());
        entity.setValue(dto.getValue());
        entity.setMeasurement(measurement);
        entity.setIndicatorConfig(config);
        return entity;
    }
}
