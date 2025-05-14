package org.example.healthdataservice.service;

import org.example.healthdataservice.entity.HealthIndicatorConfigs;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.repository.HealthIndicatorConfigsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HealthIndicatorConfigsServiceImpl implements HealthIndicatorConfigsService {
    private final HealthIndicatorConfigsRepository repository;

    public HealthIndicatorConfigsServiceImpl(HealthIndicatorConfigsRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<HealthIndicatorConfigs> getAll() {
        return repository.findAll();
    }

    @Override
    public Optional<HealthIndicatorConfigs> getByType(IndicatorType indicatorType) {
        return repository.findByIndicatorType(indicatorType);
    }

    @Override
    public HealthIndicatorConfigs save(HealthIndicatorConfigs config) {
        return repository.save(config);
    }
}
