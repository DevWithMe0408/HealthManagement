package org.example.healthdataservice.service;

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

    private final CalculatedMetricSnapshotRepository snapshotRepository;
    private final BaseMetricService baseMetricService;
    private final UnitRepository unitRepository;
    private final HealthCalculator healthCalculator;
    private final UserProfileMirrorService userProfileMirrorService;

    @Autowired
    public CalculatedMetricServiceImpl(CalculatedMetricSnapshotRepository snapshotRepository, BaseMetricService baseMetricService,
                                       UnitRepository unitRepository, HealthCalculator healthCalculator, UserProfileMirrorService userProfileMirrorService) {
        this.snapshotRepository = snapshotRepository;
        this.baseMetricService = baseMetricService;
        this.unitRepository = unitRepository;
        this.healthCalculator = healthCalculator;
        this.userProfileMirrorService = userProfileMirrorService;
    }

    @Override
    @Transactional
    public void saveUserProvidedCalculatedMetric(Long userId, IndicatorType type, Double value, Unit unit, LocalDateTime providedAt) {
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
    public void recalculateAndSaveDerivedMetrics(Long userId, Set<IndicatorType> changedBaseMetrics) {
        if (changedBaseMetrics == null || changedBaseMetrics.isEmpty()) {
            log.info("No base metrics changed for userId {}. Skipping recalculation of derived metrics.", userId);
            return;
        }
        log.info("Recalculating derived metrics for userId {} due to changes in: {}", userId, changedBaseMetrics);

        // Lấy các giá trị cơ bản mới nhất
        // Xác định các IndicatorType cơ bản cần thiết cho tất cả các phép tính
        Set<IndicatorType> requiredBaseTypes = Stream.of(IndicatorType.HEIGHT, IndicatorType.WEIGHT, IndicatorType.WAIST,
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
        Double waist = latestBaseValues.get(IndicatorType.WAIST) != null ? latestBaseValues.get(IndicatorType.WAIST).getValue() : null;
        Double hip = latestBaseValues.get(IndicatorType.HIP) != null ? latestBaseValues.get(IndicatorType.HIP).getValue() : null;
        Double neck = latestBaseValues.get(IndicatorType.NECK) != null ? latestBaseValues.get(IndicatorType.NECK).getValue() : null;
        Double activityFactor = latestBaseValues.get(IndicatorType.ACTIVITY_FACTOR) != null ? latestBaseValues.get(IndicatorType.ACTIVITY_FACTOR).getValue() : null;

        // Tính toán BMI
        if (affects(IndicatorType.BMI, changedBaseMetrics) || isFullRecalculation(changedBaseMetrics)) {
            Double bmi = healthCalculator.calculateBMI(height, weight);
            saveSystemCalculatedMetric(userId, IndicatorType.BMI, bmi, now);
        }
        // Tính toán BMR
        // BMR bị ảnh hưởng bởi height, weight, age, gender.
        // Nếu profile (age/gender) thay đổi (trigger recalculateAll) HOẶC height/weight thay đổi.
        if (affects(IndicatorType.BMR, changedBaseMetrics) || isFullRecalculation(changedBaseMetrics)) {// Thêm điều kiện nếu age/gender thay đổi
            Double bmr = healthCalculator.calculateBMR(genderString, weight, height, age);
            saveSystemCalculatedMetric(userId, IndicatorType.BMR, bmr, now);
        }

        // Tính toán TDEE
        if (affects(IndicatorType.TDEE, changedBaseMetrics) || isFullRecalculation(changedBaseMetrics)) {
            // TDEE cần BMR, nên tính BMR trước hoặc lấy BMR đã tính
            Double bmrForTdee = healthCalculator.calculateBMR(genderString, weight, height, age);
            Double tdee = healthCalculator.calculateTDEE(activityFactor, bmrForTdee);
            saveSystemCalculatedMetric(userId, IndicatorType.TDEE, tdee, now);
        }

        // Tính toán PBF
        if (affects(IndicatorType.PBF, changedBaseMetrics) || isFullRecalculation(changedBaseMetrics)) { // PBF thường phụ thuộc vào tuổi
            Double pbf = healthCalculator.calculatePBF(genderString, waist, hip, neck, height, age); // PBF Hải quân Mỹ có yếu tố tuổi
            saveSystemCalculatedMetric(userId, IndicatorType.PBF, pbf, now);
        }

        // Tính toán WHR
        if (affects(IndicatorType.WHR, changedBaseMetrics) || isFullRecalculation(changedBaseMetrics)) {
            Double whr = healthCalculator.calculateWHR(waist, hip);
            saveSystemCalculatedMetric(userId, IndicatorType.WHR, whr, now);
        }

    }

    @Override
    @Transactional
    public void recalculateAllDerivedMetricsForUser(Long userId) {
        log.info("Recalculating all derived metrics for userId {}",userId);
        Set<IndicatorType> allRelevantBaseMetrics = Stream.of(
                IndicatorType.HEIGHT, IndicatorType.WEIGHT,IndicatorType.WAIST,
                IndicatorType.HIP,IndicatorType.NECK,IndicatorType.ACTIVITY_FACTOR
        ).collect(Collectors.toSet());
        recalculateAndSaveDerivedMetrics(userId, allRelevantBaseMetrics);
    }

    private void saveSystemCalculatedMetric(Long userId, IndicatorType type, Double value, LocalDateTime calculatedAt) {
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
                    changedBaseTypes.contains(IndicatorType.HEIGHT) || changedBaseTypes.contains(IndicatorType.WAIST)
                            || changedBaseTypes.contains(IndicatorType.HIP) || changedBaseTypes.contains(IndicatorType.NECK);
            case WHR -> changedBaseTypes.contains(IndicatorType.WAIST) || changedBaseTypes.contains(IndicatorType.HIP);
            default -> false;
        };
    }


    //Helper để kiểm tra xem có phải là recalculateAll không
    private boolean isFullRecalculation(Set<IndicatorType> changedBaseMetrics) {
        // Nếu set này chứa tất cả các base metric có thể có, coi như là full recalculate
        return changedBaseMetrics.contains(IndicatorType.HEIGHT) &&
                changedBaseMetrics.contains(IndicatorType.WEIGHT) &&
                changedBaseMetrics.contains(IndicatorType.WAIST) &&
                changedBaseMetrics.contains(IndicatorType.HIP) &&
                changedBaseMetrics.contains(IndicatorType.NECK) &&
                changedBaseMetrics.contains(IndicatorType.BUST) &&
                changedBaseMetrics.contains(IndicatorType.ACTIVITY_FACTOR);
    }
    @Override
    public Optional<CalculatedMetricSnapshot> getLatestSnapshot(Long userId, IndicatorType type) {
        if (!type.isCalculatedMetric() && type.getCategory() != IndicatorCategory.USER_PROVIDED_CALCULATED) {
            log.warn("Attempted to get snapshot for non-calculated/non-user-provided-calculated type: {} for userId {}", type, userId);
        }
        // Ưu tiên lấy bản ghi do người dùng cung cấp nếu nó mới hơn, hoặc chỉ lấy bản ghi mới nhất bất kể nguồn
        return snapshotRepository.findTopByUserIdAndIndicatorTypeOrderByCalculatedAtDesc(userId, type);
    }
}
