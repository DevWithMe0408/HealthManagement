package org.example.healthdataservice.service;

import org.example.healthdataservice.dto.HealthMetricsDTO;
import org.example.healthdataservice.entity.HealthMetrics;
import org.example.healthdataservice.entity.Measurement;
import org.example.healthdataservice.entity.enums.IndicatorType;

import java.util.List;

public interface HealthMetricsService {
    HealthMetrics createHealthMetrics(double value, IndicatorType indicatorType, Measurement measurement);

    HealthMetricsDTO create(HealthMetricsDTO dto);
    HealthMetricsDTO update(Long id, HealthMetricsDTO dto);
    void delete(Long id);
    HealthMetricsDTO getById(Long id);
    List<HealthMetricsDTO> getAll();
}
