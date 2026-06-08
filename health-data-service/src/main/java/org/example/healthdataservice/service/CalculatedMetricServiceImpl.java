package org.example.healthdataservice.service;

import org.example.healthdataservice.client.Model1PbfClient;
import org.example.healthdataservice.dto.ml.PbfPredictRequest;
import org.example.healthdataservice.entity.BaseMetricValue;
import org.example.healthdataservice.entity.CalculatedMetricSnapshot;
import org.example.healthdataservice.entity.Unit;
import org.example.healthdataservice.entity.UserForHealthData;
import org.example.healthdataservice.entity.enums.Gender;
import org.example.healthdataservice.entity.enums.IndicatorCategory;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.repository.CalculatedMetricSnapshotRepository;
import org.example.healthdataservice.repository.UnitRepository;
import org.example.healthdataservice.util.HealthCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CalculatedMetricServiceImpl implements CalculatedMetricService {

    private static final Logger log = LoggerFactory.getLogger(CalculatedMetricServiceImpl.class);
    private static final String PBF_METHOD_FORMULA = "FORMULA";
    private static final String PBF_METHOD_MODEL_1 = "MODEL_1";

    private final CalculatedMetricSnapshotRepository snapshotRepository;
    private final BaseMetricService baseMetricService;
    private final UnitRepository unitRepository;
    private final HealthCalculator healthCalculator;
    private final UserProfileMirrorService userProfileMirrorService;
    private final Model1PbfClient model1PbfClient;

    @Autowired
    public CalculatedMetricServiceImpl(CalculatedMetricSnapshotRepository snapshotRepository, BaseMetricService baseMetricService,
                                       UnitRepository unitRepository, HealthCalculator healthCalculator,
                                       UserProfileMirrorService userProfileMirrorService, Model1PbfClient model1PbfClient) {
        this.snapshotRepository = snapshotRepository;
        this.baseMetricService = baseMetricService;
        this.unitRepository = unitRepository;
        this.healthCalculator = healthCalculator;
        this.userProfileMirrorService = userProfileMirrorService;
        this.model1PbfClient = model1PbfClient;
    }

    @Override
    @Transactional
    public void saveUserProvidedCalculatedMetric(String userId, IndicatorType type, Double value, Unit unit, LocalDateTime providedAt) {
        if (!type.isCalculatedMetric()) {// Kiểm tra xem có phải là loại có thể tính toán không
            log.warn("Attempted to save user-provided metric for non-calculated type: {}", type);
            return;
        }
        if (value == null) {
            log.debug("User-provided value for {} us null for userId {}. Skipping save.", type, userId);
            return;
        }

        CalculatedMetricSnapshot snapshot = new CalculatedMetricSnapshot();
        snapshot.setUserId(userId);
        snapshot.setIndicatorType(type);
        snapshot.setValue(value);
        snapshot.setUnit(unit);
        snapshot.setCalculatedAt(providedAt);
        snapshot.setSourceCategory(IndicatorCategory.USER_PROVIDED_CALCULATED);
        snapshotRepository.save(snapshot);
        log.info("Saved user-provided calculated metric {} (value: {}) for userId {}.", type, value, userId);
    }

    @Override
    @Transactional
    public void recalculateAndSaveDerivedMetrics(String userId, Set<IndicatorType> changedBaseMetrics) {
        if (changedBaseMetrics == null || changedBaseMetrics.isEmpty()) {
            log.info("No base metrics changed for userId {}. Skipping recalculation of derived metrics.", userId);
            return;
        }
        log.info("Recalculating derived metrics for userId {} due to changes in: {}", userId, changedBaseMetrics);

        // Lấy các giá trị cơ bản mới nhất
        // Xác định các IndicatorType cơ bản cần thiết cho tất cả các phép tính
        Set<IndicatorType> requiredBaseTypes = Stream.of(IndicatorType.HEIGHT, IndicatorType.WEIGHT, IndicatorType.ABDOMEN,
                IndicatorType.HIP, IndicatorType.NECK, IndicatorType.ACTIVITY_FACTOR
        ).collect(Collectors.toSet());

        Map<IndicatorType, BaseMetricValue> latestBaseValues = baseMetricService.getLatestBaseMetrics(userId, requiredBaseTypes);

        Optional<UserForHealthData> userProfileOpt = userProfileMirrorService.getUserProfile(userId);

        if (userProfileOpt.isEmpty()) {
            log.warn("User profile data not found in Health-Data-Service for userId {}. Cannot calculate metrics requiring age/gender.", userId);
            return;
        }
        UserForHealthData userProfile = userProfileOpt.get();
        LocalDate birthDate = userProfile.getBirthDate();
        Gender genderEnum = userProfile.getGender();

        Double age = null;
        if (birthDate != null ) {
            age = (double) Period.between(birthDate, LocalDate.now()).getYears();
        }
        String genderString = (genderEnum != null) ? genderEnum.name() : null;
        LocalDateTime now = LocalDateTime.now();

        // Helper để lấy giá trị từ map, trả về null nếu không có
        Double height = latestBaseValues.get(IndicatorType.HEIGHT) != null ? latestBaseValues.get(IndicatorType.HEIGHT).getValue() : null;
        Double weight = latestBaseValues.get(IndicatorType.WEIGHT) != null ? latestBaseValues.get(IndicatorType.WEIGHT).getValue() : null;
        Double abdomen = latestBaseValues.get(IndicatorType.ABDOMEN) != null ? latestBaseValues.get(IndicatorType.ABDOMEN).getValue() : null;
        Double hip = latestBaseValues.get(IndicatorType.HIP) != null ? latestBaseValues.get(IndicatorType.HIP).getValue() : null;
        Double neck = latestBaseValues.get(IndicatorType.NECK) != null ? latestBaseValues.get(IndicatorType.NECK).getValue() : null;
        Double activityFactor = latestBaseValues.get(IndicatorType.ACTIVITY_FACTOR) != null ? latestBaseValues.get(IndicatorType.ACTIVITY_FACTOR).getValue() : null;

        // Tính toán BMI
        if (affects(IndicatorType.BMI, changedBaseMetrics) || isFullRecalculation(changedBaseMetrics)) {
            Double bmi = healthCalculator.calculateBMI(height, weight);
            saveSystemCalculatedMetric(userId, IndicatorType.BMI, bmi, now, null);
        }
        // Tính toán BMR
        // BMR bị ảnh hưởng bởi height, weight, age, gender.
        // Nếu profile (age/gender) thay đổi (trigger recalculateAll) HOẶC height/weight thay đổi.
        if (affects(IndicatorType.BMR, changedBaseMetrics) || isFullRecalculation(changedBaseMetrics)) {// Thêm điều kiện nếu age/gender thay đổi
            Double bmr = healthCalculator.calculateBMR(genderString, weight, height, age);
            saveSystemCalculatedMetric(userId, IndicatorType.BMR, bmr, now, null);
        }

        // Tính toán TDEE
        if (affects(IndicatorType.TDEE, changedBaseMetrics) || isFullRecalculation(changedBaseMetrics)) {
            // TDEE cần BMR, nên tính BMR trước hoặc lấy BMR đã tính
            Double bmrForTdee = healthCalculator.calculateBMR(genderString, weight, height, age);
            Double tdee = healthCalculator.calculateTDEE(activityFactor, bmrForTdee);
            saveSystemCalculatedMetric(userId, IndicatorType.TDEE, tdee, now, null);
        }

        // Tính toán PBF
        if (affects(IndicatorType.PBF, changedBaseMetrics) || isFullRecalculation(changedBaseMetrics)) {
            Double pbf = healthCalculator.calculatePBF(genderString, abdomen, hip, neck, height, age);
            saveSystemCalculatedMetric(userId, IndicatorType.PBF, pbf, now, PBF_METHOD_FORMULA);
        }

        // Tính toán WHR
        if (affects(IndicatorType.WHR, changedBaseMetrics) || isFullRecalculation(changedBaseMetrics)) {
            Double whr = healthCalculator.calculateWHR(abdomen, hip);
            saveSystemCalculatedMetric(userId, IndicatorType.WHR, whr, now, null);
        }

    }

    @Override
    @Transactional
    public void recalculateAllDerivedMetricsForUser(String userId) {
        log.info("Recalculating all derived metrics for userId {}",userId);
        Set<IndicatorType> allRelevantBaseMetrics = Stream.of(
                IndicatorType.HEIGHT, IndicatorType.WEIGHT,IndicatorType.ABDOMEN,
                IndicatorType.HIP,IndicatorType.NECK,IndicatorType.ACTIVITY_FACTOR
        ).collect(Collectors.toSet());
        recalculateAndSaveDerivedMetrics(userId, allRelevantBaseMetrics);
    }

    @Override
    @Transactional
    public void predictAndSaveModel1Pbf(String userId, LocalDateTime now) {
        Optional<UserForHealthData> userProfileOpt = userProfileMirrorService.getUserProfile(userId);
        if (userProfileOpt.isEmpty()) {
            log.info("Skipping Model 1 PBF for userId {} because profile is missing.", userId);
            return;
        }

        UserForHealthData profile = userProfileOpt.get();
        if (profile.getGender() == null || profile.getBirthDate() == null) {
            log.info("Skipping Model 1 PBF for userId {} because gender or birthDate is missing.", userId);
            return;
        }

        double age = Period.between(profile.getBirthDate(), LocalDate.now()).getYears();
        if (age <= 0) {
            log.info("Skipping Model 1 PBF for userId {} because age is invalid: {}", userId, age);
            return;
        }

        Set<IndicatorType> requiredBaseTypes = Stream.of(
                IndicatorType.WEIGHT,
                IndicatorType.HEIGHT,
                IndicatorType.NECK,
                IndicatorType.BUST,
                IndicatorType.ABDOMEN,
                IndicatorType.HIP,
                IndicatorType.THIGH
        ).collect(Collectors.toSet());
        Map<IndicatorType, BaseMetricValue> latestBaseValues = baseMetricService.getLatestBaseMetrics(userId, requiredBaseTypes);

        Double weight = latestBaseValue(latestBaseValues, IndicatorType.WEIGHT);
        Double height = latestBaseValue(latestBaseValues, IndicatorType.HEIGHT);
        Double neck = latestBaseValue(latestBaseValues, IndicatorType.NECK);
        Double chest = latestBaseValue(latestBaseValues, IndicatorType.BUST);
        Double abdomen = latestBaseValue(latestBaseValues, IndicatorType.ABDOMEN);
        Double hip = latestBaseValue(latestBaseValues, IndicatorType.HIP);
        Double thigh = latestBaseValue(latestBaseValues, IndicatorType.THIGH);

        if (Stream.of(weight, height, neck, chest, abdomen, hip, thigh).anyMatch(value -> value == null || value <= 0)) {
            log.info("Skipping Model 1 PBF for userId {} because one or more required body measurements are missing or invalid.", userId);
            return;
        }

        PbfPredictRequest request = PbfPredictRequest.builder()
                .sexM(profile.getGender() == Gender.MALE ? 1 : 0)
                .age(age)
                .weight(weight)
                .height(height)
                .neck(neck)
                .chest(chest)
                .abdomen(abdomen)
                .hip(hip)
                .thigh(thigh)
                .build();

        Double pbfModel = model1PbfClient.predictPbf(request);
        if (pbfModel == null) {
            log.info("Skipping Model 1 PBF save for userId {} because prediction response is empty.", userId);
            return;
        }

        saveSystemCalculatedMetric(userId, IndicatorType.PBF, pbfModel, now, PBF_METHOD_MODEL_1);
    }

    private Double latestBaseValue(Map<IndicatorType, BaseMetricValue> latestBaseValues, IndicatorType type) {
        BaseMetricValue metric = latestBaseValues.get(type);
        return metric != null ? metric.getValue() : null;
    }

    private void saveSystemCalculatedMetric(String userId, IndicatorType type, Double value, LocalDateTime calculatedAt, String method) {
        if (value == null) {
            log.debug("Calculated value for {} is null for userId {}. Skipping save.", type, userId);
            return;
        }
        Unit unit = null;
        if (type.getDefaultUnitCode() != null) {
            unit = unitRepository.findByCode(type.getDefaultUnitCode()).orElseGet(() -> {
                log.warn("Default unit '{}' for calculated metric {} not found. Saving without unit. ", type.getDefaultUnitCode(), type);
                return null;
            });
        }
        CalculatedMetricSnapshot snapshot = new CalculatedMetricSnapshot();
        snapshot.setUserId(userId);
        snapshot.setIndicatorType(type);
        snapshot.setValue(value);
        snapshot.setUnit(unit);
        snapshot.setCalculatedAt(calculatedAt);
        snapshot.setSourceCategory(IndicatorCategory.CALCULATED); // Đánh dấu nguồn là hệ thống tính
        snapshot.setMethod(method);
        snapshotRepository.save(snapshot);
        log.info("Saved system-calculated metric {} (value: {}) for userId {}.", type, value, userId);

    }

    // Helper method để xác định xem một chỉ số tính toán có bị ảnh hưởng bởi các thay đổi cơ bản không
    private boolean affects(IndicatorType calculatedType, Set<IndicatorType> changedBaseTypes) {
        return switch (calculatedType) {
            case BMI ->
                    changedBaseTypes.contains(IndicatorType.HEIGHT) || changedBaseTypes.contains(IndicatorType.WEIGHT);
            case BMR ->
                    changedBaseTypes.contains(IndicatorType.HEIGHT) || changedBaseTypes.contains(IndicatorType.WEIGHT); // Giả sử age/gender lấy từ nguồn khác
            case TDEE ->
                    changedBaseTypes.contains(IndicatorType.HEIGHT) || changedBaseTypes.contains(IndicatorType.WEIGHT)
                            || changedBaseTypes.contains(IndicatorType.ACTIVITY_FACTOR);
            case PBF ->
                    changedBaseTypes.contains(IndicatorType.HEIGHT) || changedBaseTypes.contains(IndicatorType.ABDOMEN)
                            || changedBaseTypes.contains(IndicatorType.HIP) || changedBaseTypes.contains(IndicatorType.NECK);
            case WHR -> changedBaseTypes.contains(IndicatorType.ABDOMEN) || changedBaseTypes.contains(IndicatorType.HIP);
            default -> false;
        };
    }


    //Helper để kiểm tra xem có phải là recalculateAll không
    private boolean isFullRecalculation(Set<IndicatorType> changedBaseMetrics) {
        // Nếu set này chứa tất cả các base metric có thể có, coi như là full recalculate
        return changedBaseMetrics.contains(IndicatorType.HEIGHT) &&
                changedBaseMetrics.contains(IndicatorType.WEIGHT) &&
                changedBaseMetrics.contains(IndicatorType.ABDOMEN) &&
                changedBaseMetrics.contains(IndicatorType.HIP) &&
                changedBaseMetrics.contains(IndicatorType.NECK) &&
                changedBaseMetrics.contains(IndicatorType.BUST) &&
                changedBaseMetrics.contains(IndicatorType.ACTIVITY_FACTOR);
    }
    @Override
    public Optional<CalculatedMetricSnapshot> getLatestSnapshot(String userId, IndicatorType type) {
        if (!type.isCalculatedMetric() && type.getCategory() != IndicatorCategory.USER_PROVIDED_CALCULATED) {
            log.warn("Attempted to get snapshot for non-calculated/non-user-provided-calculated type: {} for userId {}", type, userId);
        }
        // Ưu tiên lấy bản ghi do người dùng cung cấp nếu nó mới hơn, hoặc chỉ lấy bản ghi mới nhất bất kể nguồn
        return snapshotRepository.findTopByUserIdAndIndicatorTypeOrderByCalculatedAtDesc(userId, type);
    }

    @Override
    public Optional<CalculatedMetricSnapshot> getLatestSnapshotByMethod(String userId, IndicatorType type, String method) {
        if (!type.isCalculatedMetric() && type.getCategory() != IndicatorCategory.USER_PROVIDED_CALCULATED) {
            log.warn("Attempted to get snapshot by method for non-calculated/non-user-provided-calculated type: {} for userId {}", type, userId);
        }
        return snapshotRepository.findTopByUserIdAndIndicatorTypeAndMethodOrderByCalculatedAtDesc(userId, type, method);
    }
}
