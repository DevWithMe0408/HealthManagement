package org.example.healthdataservice.service;

import org.example.healthdataservice.entity.BaseMetricValue;
import org.example.healthdataservice.entity.Unit;
import org.example.healthdataservice.entity.enums.IndicatorCategory;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.repository.BaseMetricValueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class BaseMetricServiceImpl implements BaseMetricService {

    private static final Logger log = LoggerFactory.getLogger(BaseMetricServiceImpl.class);
    private static final double EPSILON = 1e-6;

    private final BaseMetricValueRepository baseMetricValueRepository;

    @Autowired
    public BaseMetricServiceImpl(BaseMetricValueRepository baseMetricValueRepository) {
        this.baseMetricValueRepository = baseMetricValueRepository;
    }

    @Override
    @Transactional
    public Optional<BaseMetricValue> saveBaseMetricIfChanged(Long userId, IndicatorType type, Double newValue,
                                                             Unit unit, LocalDateTime recordedAt) {

        if (type.getCategory() != IndicatorCategory.BASE) {
            log.warn("Attempted to save non-base metric type {} as a base metric. Skipping.", type);
            return Optional.empty();
        }
        if (newValue == null) {
            log.debug("New value for {} is null for userId {}. Skipping save.", type, userId);
            return Optional.empty();
        }

        Optional<BaseMetricValue> latestRecordOpt = baseMetricValueRepository
                .findTopByUserIdAndIndicatorTypeOrderByRecordedAtDesc(userId, type);

        // Kiểm tra xem giá trị mới có khác với giá trị gần nhất không
        boolean shouldSave = true;
        if (latestRecordOpt.isPresent()) {
            if (areDoubleEffectivelyEqual(newValue, latestRecordOpt.get().getValue()) &&
            unitsAreEqual(unit, latestRecordOpt.get().getUnit())) {
                shouldSave = false;
                log.info("Base metric {} for userId {} has not changed (value: {}, unit: {}). Skipping save.",
                        type, userId, newValue, unit != null ? unit.getCode() : "null");
            }
        }
        if (shouldSave) {
            BaseMetricValue newMetric = new BaseMetricValue();
            newMetric.setUserId(userId);
            newMetric.setIndicatorType(type);
            newMetric.setValue(newValue);
            newMetric.setUnit(unit);
            newMetric.setRecordedAt(recordedAt);
            BaseMetricValue saveMetric = baseMetricValueRepository.save(newMetric);
            log.info("New base metric {} (value: {}, unit: {}) saved for userId {}.",
                    type, newValue, unit != null ? unit.getCode() : "null", userId);

            return Optional.of(saveMetric);
        }
        return Optional.empty();
    }

    @Override
    public Optional<BaseMetricValue> getLatestBaseMetric(Long userId, IndicatorType type) {
        if (type.getCategory() != IndicatorCategory.BASE) {
            log.warn("Attempted to get non-base metric type {} as a base metric.", type);
            return Optional.empty();
        }
        return baseMetricValueRepository.findTopByUserIdAndIndicatorTypeOrderByRecordedAtDesc(userId, type);
    }
    @Override
    public Map<IndicatorType, BaseMetricValue> getLatestBaseMetrics(Long userId, Set<IndicatorType> types) {
        Map<IndicatorType, BaseMetricValue> latestMetricsMap = new HashMap<>();
        for (IndicatorType type : types) {
            if(type.getCategory() == IndicatorCategory.BASE) {
                getLatestBaseMetric(userId, type).ifPresent(metric -> latestMetricsMap.put(type, metric));
            }
        }
        return latestMetricsMap;
    }

    private boolean areDoubleEffectivelyEqual(Double d1, Double d2) {
        if (d1 == null && d2 == null) return true;
        if (d1 == null || d2 == null) return false; // Nếu một trong hai null mà cái kia không thì khác nhau
        return Math.abs(d1 - d2) < EPSILON;
    }
    private boolean unitsAreEqual(Unit u1, Unit u2) {
        if (u1 == null && u2 == null) return true;
        if (u1 == null || u2 == null) return false; // Nếu một trong hai null mà cái kia không thì khác nhau
        return u1.getId().equals(u2.getId()); // So sánh qua ID của Unit
    }
}
