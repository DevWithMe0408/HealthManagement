package org.example.healthdataservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.healthdataservice.entity.enums.IndicatorType;

import java.time.LocalDateTime;

@Entity
@Table(name = "base_metric_values")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseMetricValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Tham chiếu logic tới User ID từ User Service

    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", nullable = false)
    private IndicatorType indicatorType;

    @Column(nullable = false)
    private Double value;

    @ManyToOne(fetch = FetchType.LAZY) // Giả sử bạn muốn lazy load Unit
    @JoinColumn(name = "unit_id") // Unit có thể null nếu chỉ số không có đơn vị (ví dụ: ActivityFactor)
    private Unit unit;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    // Tùy chọn: nguồn gốc dữ liệu
    // @Enumerated(EnumType.STRING)
    // @Column(name = "source")
    // private MetricSource source; // Ví dụ: USER_INPUT, DEVICE_SYNC
}
