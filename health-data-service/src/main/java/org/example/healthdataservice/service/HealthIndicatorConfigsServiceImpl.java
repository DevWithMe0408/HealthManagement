package org.example.healthdataservice.service;

import org.example.healthdataservice.entity.HealthIndicatorConfigs;
import org.example.healthdataservice.entity.Unit;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.entity.enums.MeasurementFrequency;
import org.example.healthdataservice.repository.HealthIndicatorConfigsRepository;
import org.example.healthdataservice.repository.UnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class HealthIndicatorConfigsServiceImpl implements HealthIndicatorConfigsService {

    private static final Logger log = LoggerFactory.getLogger(HealthIndicatorConfigsServiceImpl.class);
    private final HealthIndicatorConfigsRepository repository;
    private final UnitRepository unitRepository;

    public HealthIndicatorConfigsServiceImpl(HealthIndicatorConfigsRepository repository, UnitRepository unitRepository) {
        this.repository = repository;
        this.unitRepository = unitRepository;
    }

    @Override
    public List<HealthIndicatorConfigs> getAll() {
        return repository.findAll();
    }

    // Bạn có thể cần điều chỉnh phương thức này nếu nó dành cho một user cụ thể
    @Override
    public Optional<HealthIndicatorConfigs> getByType(IndicatorType indicatorType) {
        // Nếu config là global thì OK, nếu theo user thì cần thêm userId
        // return repository.findByIndicatorTypeAndUserId(indicatorType, userId);
        return repository.findByIndicatorType(indicatorType); // Giả sử tìm kiếm global hoặc chưa cần userId ở đây
    }

    @Override
    public HealthIndicatorConfigs save(HealthIndicatorConfigs config) {
        return repository.save(config);
    }

    @Override
    @Transactional
    public void createDefaultHealthIndicatorConfigsForUser(Long userId) {
        log.info("Default health indicator configs already exist for userid: {}", userId);

        // Kiểm tra xem user đã có config chưa để tránh tạo trùng (Idempotency)
        if (repository.existsByUserId(userId)) {
            log.warn("Default health indicator configs already exist for userid: {}. Skipping creating.", userId);
            return;
        }

        List<HealthIndicatorConfigs> defaultConfigs = new ArrayList<>();

        for (IndicatorType type : IndicatorType.values()) {
            Unit unit = null;
            if (type.getDefaultUnitCode() != null) {
                unit = unitRepository.findByCode(type.getDefaultUnitCode())
                        .orElseGet(() -> {
                            log.warn("Unit with code '{}' for IndicatorType '{}' not found. Config will be created without unit.",
                                    type.getDefaultUnitCode(), type);
                            return null;
                        });
            }
            MeasurementFrequency frequency = type.getDefaultFrequency();
            defaultConfigs.add(createDefaultConfigEntity(userId,type,type.getDefaultDisplayName(),unit,frequency));

        }


        if (!defaultConfigs.isEmpty()) {
            repository.saveAll(defaultConfigs);
            log.info("Save {} default health indicator configs for userId: {}", defaultConfigs.size(), userId);
        } else {
            log.info("No default health indicator configs defined to create for userId: {}", userId);
        }
    }

    private HealthIndicatorConfigs createDefaultConfigEntity(Long userId, IndicatorType type, String displayName, Unit unit, MeasurementFrequency frequency) {
        HealthIndicatorConfigs config = new HealthIndicatorConfigs();
        config.setUserId(userId);
        config.setIndicatorType(type);
        config.setDisplayName(displayName);
        config.setUnit(unit);
        config.setMeasurementFrequency(frequency);
        config.setActive(true);
        return config;
    }
}
