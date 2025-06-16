package org.example.healthdataservice.service;

import org.example.healthdataservice.entity.CalculatedMetricSnapshot;
import org.example.healthdataservice.entity.Unit;
import org.example.healthdataservice.entity.enums.IndicatorType;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface CalculatedMetricService {

    /**
     * Lưu một giá trị chỉ số tính toán được cung cấp bởi người dùng.
     */
    void saveUserProvidedCalculatedMetric(Long userId, IndicatorType type, Double value, Unit unit, LocalDateTime providedAt);

    /**
     * Tính toán lại và lưu trữ các chỉ số dẫn xuất dựa trên những thay đổi của chỉ số cơ bản.
     * @param userId ID người dùng
     * @param changedBaseMetrics Tập hợp các IndicatorType cơ bản đã thay đổi.
     *                         Service sẽ xác định các chỉ số tính toán nào cần cập nhật.
     */
    void recalculateAndSaveDerivedMetrics(Long userId, Set<IndicatorType> changedBaseMetrics);

    /**
     * Tính toán lại và lưu trữ TẤT CẢ các chỉ số dẫn xuất cho người dùng.
     * Hữu ích khi user mới được tạo hoặc khi có thay đổi lớn (vd: tuổi, giới tính thay đổi).
     */
    void recalculateAllDerivedMetricsForUser(Long userId);

    Optional<CalculatedMetricSnapshot> getLatestSnapshot(Long userId, IndicatorType type);
}
