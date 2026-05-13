package org.example.nutritionservice.entity.config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_ratio_configs")
@IdClass(MealRatioConfigId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealRatioConfig {

    @Id
    @Column(name = "plan_type", length = 10)
    private String planType;     // 3_BUA, 5_BUA

    @Id
    @Column(name = "meal_code", length = 20)
    private String mealCode;     // SANG, TRUA, TOI, PHU_SANG, PHU_CHIEU

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal ratio;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;
}
