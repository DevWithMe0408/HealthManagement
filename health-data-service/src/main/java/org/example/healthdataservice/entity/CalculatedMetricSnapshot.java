package org.example.healthdataservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.entity.enums.IndicatorCategory;
import org.example.healthdataservice.entity.enums.IndicatorType;

import java.time.LocalDateTime;

@Entity
@Table(name = "calculated_metric_snapshots")
@Data
@RequiredArgsConstructor
public class CalculatedMetricSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name ="indicator_type", nullable = false)
    private IndicatorType indicatorType; // Sẽ là các IndicatorType.CALCULATED hoặc .USER_PROVIDED_CALCULATED

    @Column(nullable = false)
    private Double value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit; // Unit có thể null nếu chỉ số không có đơn vị

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt; // Thời điểm giá trị này được tính toán hoặc người dùng cung cấp

    @Enumerated(EnumType.STRING)
    @Column(name = "source_category", nullable = false)
    private IndicatorCategory sourceCategory; // Phân biệt SYSTEM_CALCULATED hay USER_PROVIDED_CALCULATED

    // Tùy chọn: Nếu bạn muốn liên kết với một "sự kiện submit" cụ thể
    // @Column(name = "submit_event_id")
    // private Long submitEventId;

    // Tùy chọn: Nếu bạn muốn lưu trữ các ID của base_metric_values đã dùng để tính
    // @Column(name = "source_base_metric_ids", columnDefinition = "TEXT") // Hoặc JSONB nếu DB hỗ trợ
    // private String sourceBaseMetricIds;
}
