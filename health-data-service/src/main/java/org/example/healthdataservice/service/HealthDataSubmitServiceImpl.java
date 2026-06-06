package org.example.healthdataservice.service;

import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.dto.request.SubmitHealthDataRequest;
import org.example.healthdataservice.entity.*;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class HealthDataSubmitServiceImpl implements HealthDataSubmitService {

    private static final Logger log = LoggerFactory.getLogger(HealthDataSubmitServiceImpl.class);

    private final BaseMetricService baseMetricService;
    private final UnitRepository unitRepository;
    private final CalculatedMetricService calculatedMetricService;

    @Autowired
    public HealthDataSubmitServiceImpl(BaseMetricService baseMetricService,
                                       UnitRepository unitRepository,
                                       CalculatedMetricService calculatedMetricService) {
        this.baseMetricService = baseMetricService;
        this.unitRepository = unitRepository;
        this.calculatedMetricService = calculatedMetricService;
    }

    @Override
    @Transactional
    public void processSubmittedHealthData(SubmitHealthDataRequest request) {

        String userId = request.getUserId();
        LocalDateTime now = LocalDateTime.now(); // Thời điểm ghi nhận
        Set<IndicatorType> changedBaseMetrics = new HashSet<>();

        // Contract moi dung baseMetrics[], fallback field phang de khong pha client cu.
        if (request.getBaseMetrics() != null && !request.getBaseMetrics().isEmpty()) {
            saveBaseMetricsFromList(request, now, changedBaseMetrics);
        } else {
            saveBaseMetricsFromFlatFields(request, now, changedBaseMetrics);
        }

        // 2. Xử lý các chỉ số tính toán do người dùng cung cấp (nếu có)
        if (request.getBMINew() != null) {
            calculatedMetricService.saveUserProvidedCalculatedMetric(userId, IndicatorType.BMI, request.getBMINew(), getUnitForType(IndicatorType.BMI), now);
        }
        if (request.getBMRNew() != null) {
            calculatedMetricService.saveUserProvidedCalculatedMetric(userId, IndicatorType.BMR, request.getBMRNew(), getUnitForType(IndicatorType.BMR), now);
        }
        if (request.getTDEENew() != null) {
            calculatedMetricService.saveUserProvidedCalculatedMetric(userId, IndicatorType.TDEE, request.getTDEENew(), getUnitForType(IndicatorType.TDEE), now);
        }
        if (request.getPBFNew() != null) {
            calculatedMetricService.saveUserProvidedCalculatedMetric(userId, IndicatorType.PBF, request.getPBFNew(), getUnitForType(IndicatorType.PBF), now);
        }
        if (request.getWHRNew() != null) {
            calculatedMetricService.saveUserProvidedCalculatedMetric(userId, IndicatorType.WHR, request.getWHRNew(), getUnitForType(IndicatorType.WHR), now);
        }

        // 3. Nếu có bất kỳ chỉ số cơ bản nào thay đổi, kích hoạt tính toán lại
        if (!changedBaseMetrics.isEmpty()) {
            log.info("Base metrics changed for userId: {}. Types: {}. Triggering recalculation of derived metrics.",
                    userId, changedBaseMetrics);
             calculatedMetricService.recalculateAndSaveDerivedMetrics(userId, changedBaseMetrics);
             try {
                 calculatedMetricService.predictAndSaveModel1Pbf(userId, now);
             } catch (Exception e) {
                 log.warn("Model 1 PBF skipped for userId {}: {}", userId, e.getMessage());
                 throw e;
             }
        } else {
            log.info("No base metrics changed for userId: {}. Skipping recalculation.", userId);
        }
        log.info("Finished processing submitted health data for userId: {}", userId);

    }

    private void saveBaseMetricFromRequest(String userId, IndicatorType type, Double value, LocalDateTime recordedAt, Set<IndicatorType> changedMetricsCollector) {
        if (value != null) { // Chỉ xử lý nếu người dùng cung cấp giá trị
            Unit unit = null;
            if (type.getDefaultUnitCode() != null) {
                // Lấy Unit từ repository dựa trên defaultUnitCode của IndicatorType
                unit = unitRepository.findByCode(type.getDefaultUnitCode())
                        .orElseGet(() -> {
                            log.warn("Unit with code {} not found for IndicatorType '{}'. Metric witll be saved without unit.",
                                    type.getDefaultUnitCode(), type);
                            return null;
                        });
            }
            Optional<BaseMetricValue> saveMetric = baseMetricService.saveBaseMetricIfChanged(userId, type, value, unit, recordedAt);
            saveMetric.ifPresent(bmv -> changedMetricsCollector.add(bmv.getIndicatorType()));
        }
    }

    private void saveBaseMetricsFromList(SubmitHealthDataRequest request,
                                         LocalDateTime recordedAt,
                                         Set<IndicatorType> changedMetricsCollector) {
        for (SubmitHealthDataRequest.BaseMetricInput metric : request.getBaseMetrics()) {
            if (metric == null || metric.getType() == null) {
                throw new IllegalArgumentException("baseMetrics.type khong duoc de trong");
            }
            if (!metric.getType().isBaseMetric()) {
                throw new IllegalArgumentException("Chi so " + metric.getType() + " khong phai base metric");
            }
            saveBaseMetricFromRequest(
                    request.getUserId(),
                    metric.getType(),
                    metric.getValue(),
                    recordedAt,
                    changedMetricsCollector
            );
        }
    }

    private void saveBaseMetricsFromFlatFields(SubmitHealthDataRequest request,
                                               LocalDateTime recordedAt,
                                               Set<IndicatorType> changedMetricsCollector) {
        saveBaseMetricFromRequest(request.getUserId(), IndicatorType.HEIGHT, request.getHeight(), recordedAt, changedMetricsCollector);
        saveBaseMetricFromRequest(request.getUserId(), IndicatorType.WEIGHT, request.getWeight(), recordedAt, changedMetricsCollector);
        saveBaseMetricFromRequest(request.getUserId(), IndicatorType.ABDOMEN, request.getAbdomen(), recordedAt, changedMetricsCollector);
        saveBaseMetricFromRequest(request.getUserId(), IndicatorType.HIP, request.getHip(), recordedAt, changedMetricsCollector);
        saveBaseMetricFromRequest(request.getUserId(), IndicatorType.NECK, request.getNeck(), recordedAt, changedMetricsCollector);
        saveBaseMetricFromRequest(request.getUserId(), IndicatorType.BUST, request.getBust(), recordedAt, changedMetricsCollector);
        saveBaseMetricFromRequest(request.getUserId(), IndicatorType.THIGH, request.getThigh(), recordedAt, changedMetricsCollector);
        saveBaseMetricFromRequest(request.getUserId(), IndicatorType.ACTIVITY_FACTOR, request.getActivityFactor(), recordedAt, changedMetricsCollector);
    }

    // Helper method để lấy Unit
    private Unit getUnitForType(IndicatorType type) {
        if (type.getDefaultUnitCode() != null) {
            return unitRepository.findByCode(type.getDefaultUnitCode())
                    .orElseGet(() -> {
                        log.warn("Unit with code '{}' not found for IndicatorType '{}'.",
                                type.getDefaultUnitCode(), type);
                        return null;
                    });
        }
        return null;
    }


}
