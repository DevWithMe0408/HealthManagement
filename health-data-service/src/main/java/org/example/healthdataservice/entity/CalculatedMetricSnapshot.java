package org.example.healthdataservice.entity;

import jakarta.persistence.*;
import lombok.Data;
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

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", nullable = false)
    private IndicatorType indicatorType;

    @Column(nullable = false)
    private Double value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_category", nullable = false)
    private IndicatorCategory sourceCategory;

    @Column(name = "method")
    private String method;
}
