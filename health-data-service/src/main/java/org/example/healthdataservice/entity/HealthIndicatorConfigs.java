package org.example.healthdataservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.entity.enums.MeasurementFrequency;

@Entity
@Table(name = "health_indicator_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthIndicatorConfigs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", nullable = false, unique = true)
    private IndicatorType indicatorType; // Ví dụ: BMI, WHR, PBF,...

    @Column(name = "display_name")
    private String displayName; // Tên hiển thị: "Chỉ số BMI", "Vòng eo/hông",...

    @ManyToOne
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_frequency")
    private MeasurementFrequency measurementFrequency; // daily | weekly | monthly

    @Column(name = "is_active")
    private boolean isActive = true;
}
