package org.example.healthdataservice.service;

import org.example.healthdataservice.entity.BaseMetricValue;
import org.example.healthdataservice.entity.Unit;
import org.example.healthdataservice.entity.enums.IndicatorType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface BaseMetricService {
    /**
     * Lưu một giá trị chỉ số cơ bản mới nếu nó đã thay đổi so với giá trị gần nhất.
     * @param userId ID của người dùng
     * @param type Loại chỉ số cơ bản
     * @param newValue Giá trị mới
     * @param unit Đơn vị của giá trị (có thể null)
     * @param recordedAt Thời điểm ghi nhận
     * @return BaseMetricValue đã được lưu (nếu có thay đổi), hoặc Optional.empty() nếu không có thay đổi.
     */

    Optional<BaseMetricValue> saveBaseMetricIfChanged(Long userId, IndicatorType type, Double newValue, Unit unit, LocalDateTime recordedAt);

    /**
     * Lấy giá trị mới nhất của một chỉ số cơ bản cho người dùng.
     */
    Optional<BaseMetricValue> getLatestBaseMetric(Long userId, IndicatorType type);

    /**
     * Lấy map các giá trị mới nhất của một tập hợp các chỉ số cơ bản cho người dùng.
     * Key là IndicatorType, Value là BaseMetricValue.
     */
    Map<IndicatorType, BaseMetricValue> getLatestBaseMetrics(Long userId, Set<IndicatorType> types);
}
