package org.example.healthdataservice.service;

import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.dto.HealthMetricsDTO;
import org.example.healthdataservice.entity.HealthIndicatorConfigs;
import org.example.healthdataservice.entity.HealthMetrics;
import org.example.healthdataservice.entity.Measurement;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.mapper.HealthMetricsMapper;
import org.example.healthdataservice.repository.HealthIndicatorConfigsRepository;
import org.example.healthdataservice.repository.HealthMetricsRepository;
import org.example.healthdataservice.repository.MeasurementRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthMetricsServiceImpl implements HealthMetricsService {
    private final HealthMetricsRepository healthMetricsRepository;
    private final MeasurementRepository measurementRepository;
    private final HealthIndicatorConfigsRepository configsRepository;
    private final HealthMetricsMapper mapper;


    @Override
    public HealthMetrics createHealthMetrics(double value, IndicatorType indicatorType, Measurement measurement) {
        HealthIndicatorConfigs config = configsRepository.getByIndicatorType(indicatorType)
                .orElseThrow(() -> new RuntimeException("Indicator config not found" + indicatorType));
        HealthMetrics healthMetrics = new HealthMetrics();
        healthMetrics.setValue(value);
        healthMetrics.setMeasurement(measurement);
        healthMetrics.setIndicatorConfig(config);

        return healthMetricsRepository.save(healthMetrics);
    }
    @Override
    public HealthMetricsDTO create(HealthMetricsDTO dto) {
        Measurement measurement = measurementRepository.findById(dto.getMeasurementId())
                .orElseThrow(() -> new RuntimeException("Measurement not found"));

        HealthIndicatorConfigs config = configsRepository.findById(dto.getIndicatorConfigId())
                .orElseThrow(() -> new RuntimeException("Config not found"));

        HealthMetrics entity = mapper.toEntity(dto, measurement, config);
        return mapper.toDto(healthMetricsRepository.save(entity));
    }

    @Override
    public HealthMetricsDTO update(Long id, HealthMetricsDTO dto) {
        HealthMetrics existing = healthMetricsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Metric not found"));

        Measurement measurement = measurementRepository.findById(dto.getMeasurementId())
                .orElseThrow(() -> new RuntimeException("Measurement not found"));

        HealthIndicatorConfigs config = configsRepository.findById(dto.getIndicatorConfigId())
                .orElseThrow(() -> new RuntimeException("Config not found"));

        existing.setValue(dto.getValue());
        existing.setMeasurement(measurement);
        existing.setIndicatorConfig(config);

        return mapper.toDto(healthMetricsRepository.save(existing));
    }

    @Override
    public void delete(Long id) {
        healthMetricsRepository.deleteById(id);
    }

    @Override
    public HealthMetricsDTO getById(Long id) {
        return healthMetricsRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Metric not found"));
    }

    @Override
    public List<HealthMetricsDTO> getAll() {
        return healthMetricsRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

}
