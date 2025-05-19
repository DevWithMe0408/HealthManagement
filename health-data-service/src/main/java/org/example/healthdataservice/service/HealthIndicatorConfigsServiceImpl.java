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

        defaultConfigs.add(createDefaultConfig(userId,IndicatorType.HEIGHT, "Chiều cao", unitRepository.findById(4L), MeasurementFrequency.YEARLY));
        defaultConfigs.add(createDefaultConfig(userId,IndicatorType.WEIGHT, "Cân nặng", unitRepository.findById(3L), MeasurementFrequency.WEEKLY));
        defaultConfigs.add(createDefaultConfig(userId,IndicatorType.BMI, "Chỉ số khối cơ thể", null, MeasurementFrequency.WEEKLY));
        defaultConfigs.add(createDefaultConfig(userId,IndicatorType.BMR, "Tỷ lệ trao đổi chất cơ bản", unitRepository.findById(5L), MeasurementFrequency.WEEKLY));
        defaultConfigs.add(createDefaultConfig(userId,IndicatorType.TDEE, "Tổng năng lượng tiêu thụ hàng ngày", unitRepository.findById(5L), MeasurementFrequency.WEEKLY));
        defaultConfigs.add(createDefaultConfig(userId,IndicatorType.PBF, "Tỷ lệ mỡ cơ thể", unitRepository.findById(6L), MeasurementFrequency.WEEKLY));
        defaultConfigs.add(createDefaultConfig(userId,IndicatorType.WHR, "Tỷ lệ vòng eo/vòng hông", null, MeasurementFrequency.WEEKLY));
        defaultConfigs.add(createDefaultConfig(userId,IndicatorType.WAIST, "Vòng eo", unitRepository.findById(4L), MeasurementFrequency.MONTHLY));
        defaultConfigs.add(createDefaultConfig(userId,IndicatorType.HIP, "Vòng hông", unitRepository.findById(4L), MeasurementFrequency.MONTHLY));
        defaultConfigs.add(createDefaultConfig(userId,IndicatorType.NECK, "Vòng cổ", unitRepository.findById(4L), MeasurementFrequency.MONTHLY));
        defaultConfigs.add(createDefaultConfig(userId,IndicatorType.BUST, "Vòng ngực", unitRepository.findById(4L), MeasurementFrequency.MONTHLY));
        defaultConfigs.add(createDefaultConfig(userId,IndicatorType.ACTIVITYFACTOR, "Hệ số hoạt động", null, MeasurementFrequency.MONTHLY));
        // Thêm các chỉ số mặc định khác ở đây

        if (!defaultConfigs.isEmpty()) {
            repository.saveAll(defaultConfigs);
            log.info("Save {} default health indicator configs for userId: {}", defaultConfigs.size(), userId);
        } else {
            log.info("No default health indicator configs defined to create for userId: {}", userId);
        }
    }

    private HealthIndicatorConfigs createDefaultConfig(Long userId, IndicatorType type, String displayName, Unit unit, MeasurementFrequency frequency) {
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
