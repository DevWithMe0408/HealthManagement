package org.example.healthdataservice.service;

import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.dto.request.SubmitHealthDataRequest;
import org.example.healthdataservice.entity.*;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.entity.enums.MeasurementFrequency;
import org.example.healthdataservice.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HealthDataSubmitServiceImpl implements HealthDataSubmitService {

    private final MeasurementRepository measurementRepository;
    private final HealthIndicatorConfigsRepository healthIndicatorConfigsRepository;
    private final HealthMetricsRepository metricsRepository;

    @Override
    public void submitBasicMetrics(SubmitHealthDataRequest request) {

        List<HealthIndicatorConfigs> configs = healthIndicatorConfigsRepository.findByUserIdAndIsActiveTrue(request.getUserId());
        List<IndicatorType> toUpdate = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for(HealthIndicatorConfigs config : configs) {
            if(isDueToMeasure(config,request.getUserId(), now)) {
                toUpdate.add(config.getIndicatorType());
            }
        }
        Measurement targetMeasurement;

        if(!toUpdate.isEmpty()){
            // // Có ít nhất một chỉ số đến hạn → tạo Measurement mới
            targetMeasurement = new Measurement();
            targetMeasurement.setUserId(request.getUserId());
            targetMeasurement.setMeasurement_time(now);
            targetMeasurement.setYear(now.getYear());
            targetMeasurement.setMonth(now.getMonthValue());
            targetMeasurement.setWeek_number(now.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()));
            targetMeasurement.setUpdatedAt(now);
            measurementRepository.save(targetMeasurement);
        } else {
            // Không có chỉ số nào đến hạn → lấy Measurement mới nhất
            targetMeasurement = measurementRepository.findLastByUserId(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin đo lường cho người dùng: " + request.getUserId()));
        }

        for(HealthIndicatorConfigs config : configs) {
            IndicatorType type = config.getIndicatorType();
            Double value = extractIndicatorValue(type, request);

            if(value == null) {
                value = calculateIfPossible(type, request); // fallback nếu không có giá trị
            }

            if(value != null) {
                saveOrUpdateMetric(targetMeasurement, config, value);
            }

        }

    }
    private Double calculateIfPossible(IndicatorType type, SubmitHealthDataRequest request) {
        return switch (type) {
            case BMI -> calculateBMI(request.getHeight(), request.getWeight());
            case BMR -> calculateBMR(request.getGender(), request.getWeight(), request.getHeight(), request.getAge());
            case TDEE -> {
                Double bmr = calculateBMR(request.getGender(), request.getWeight(), request.getHeight(), request.getAge());
                yield (bmr != null && request.getActivityFactor() != null) ? bmr * request.getActivityFactor() : null;
            }
            case PBF -> calculatePBF(request.getGender(), request.getWaist(), request.getHip(), request.getNeck(), request.getHeight());
            case WHR -> calculateWHR(request.getWaist(), request.getHip());
            default -> null;
        };
    }
    private void saveOrUpdateMetric(Measurement measurement, HealthIndicatorConfigs config, Double value) {
        Optional<HealthMetrics> existing = metricsRepository
                .findByMeasurementAndIndicatorConfig(measurement.getId(),config.getId());

        if(existing.isPresent()) {
            HealthMetrics metrics = existing.get();
            metrics.setValue(value);
            metricsRepository.save(metrics);
        } else {
            HealthMetrics metrics = new HealthMetrics();
            metrics.setMeasurement(measurement);
            metrics.setIndicatorConfig(config);
            metrics.setValue(value);
            metricsRepository.save(metrics);
        }
    }
    // Trich xuất giá trị chỉ số từ request
    private Double extractIndicatorValue(IndicatorType type, SubmitHealthDataRequest request) {
        return switch (type) {
            case HEIGHT -> request.getHeight();
            case WEIGHT -> request.getWeight();
            case WAIST -> request.getWaist();
            case HIP -> request.getHip();
            case NECK -> request.getNeck();
            case BUST -> request.getBust();
            case ACTIVITYFACTOR -> request.getActivityFactor();
            case BMI -> request.getBMINew();
            case BMR -> request.getBMRNew();
            case TDEE -> request.getTDEENew();
            case PBF -> request.getPBFNew();
            case WHR -> request.getWHRNew();
        };
    }

    // tính toán chỉ số BMI
    private Double calculateBMI(Double height, Double weight) {
        if(height == null || weight == null || height == 0) {
            return null;
        }
        return weight/ Math.pow(height / 100.0, 2);
    }

    // tính toán chỉ số BHR theo Mifflin-St Jeor
    private Double calculateBMR(String gender, Double weight, Double height, Double age) {
        if (gender == null || weight == null || height == null || age == null) {
            return null;
        }
        return gender.equalsIgnoreCase("male") ?
                10 * weight + 6.25 * height - 5 * age + 5 :
                10 * weight + 6.25 * height - 5 * age - 161;
    }
    // tính toán chỉ số TDEE
    private Double calculateTDEE(Double activityFactor, String gender, Double weight, Double height, Double age) {
        Double bmr = calculateBMR(gender, weight, height, age);
        if (bmr == null || activityFactor == null) {
            return null;
        }
        return bmr * activityFactor;
    }
    // tính toán chỉ số PBF theo phương pháp Jackson & Pollock
    private Double calculatePBF(String gender, Double waist, Double hip, Double neck, Double height) {
        if (gender == null || waist == null || height == null || neck == null) {
            return null;
        }
        if (gender.equalsIgnoreCase("male")) {
            return 495 / (1.0324 - 0.19077 * Math.log10(waist - neck) + 0.15456 * Math.log10(height)) - 450;
        } else if (hip != null) {
            return 495 / (1.29579 - 0.35004 * Math.log10(waist + hip - neck) + 0.22100 * Math.log10(height)) - 450;
        }
        return null;
    }
    // tính toán chỉ số WHR
    private Double calculateWHR(Double waist, Double hip) {
        if (waist == null || hip == null || hip == 0) {
            return null;
        }
        return waist / hip;
    }
    // Hàm kiểm tra tuần suất
    private boolean isDueToMeasure(HealthIndicatorConfigs config, Long userId, LocalDateTime now){
        Optional<HealthMetrics> lastestMetric = metricsRepository.findLatestByIndicatorTypeAndUserId(config.getIndicatorType(), userId);
        if (lastestMetric.isEmpty()) return true; // nếu chưa có chỉ số nào thì cho phép đo

        LocalDateTime lastTime = lastestMetric.get().getMeasurement().getMeasurement_time();

        return switch (config.getMeasurementFrequency()) {
            case DAILY -> !lastTime.toLocalDate().isEqual(now.toLocalDate());
            case WEEKLY -> lastTime.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
                    != now.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
            case MONTHLY -> lastTime.getMonthValue() != now.getMonthValue() || lastTime.getYear() != now.getYear();
            default -> false;
        };
    }
}
