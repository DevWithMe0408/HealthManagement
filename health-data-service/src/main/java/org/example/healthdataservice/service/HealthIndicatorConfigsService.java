package org.example.healthdataservice.service;

import org.example.healthdataservice.entity.HealthIndicatorConfigs;
import org.example.healthdataservice.entity.enums.IndicatorType;

import java.util.List;
import java.util.Optional;

public interface HealthIndicatorConfigsService {
    List<HealthIndicatorConfigs> getAll();
    Optional<HealthIndicatorConfigs> getByType(IndicatorType indicatorType);
    HealthIndicatorConfigs save(HealthIndicatorConfigs configs);
}
