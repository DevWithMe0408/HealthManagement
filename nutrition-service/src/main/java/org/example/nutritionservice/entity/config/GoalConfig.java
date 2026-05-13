package org.example.nutritionservice.entity.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cau hinh theo muc tieu (Giam can / Duy tri / Tang can).
 * Bang co dung 3 rows fixed (1 row cho moi muc tieu).
 */
@Entity
@Table(name = "goal_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalConfig {

    @Id
    @Column(name = "goal_code", length = 20)
    private String goalCode;        // GIAM, DUY_TRI, TANG

    // ===== He so deficit/surplus =====
    @Column(name = "cal_multiplier", precision = 3, scale = 2, nullable = false)
    private BigDecimal calMultiplier;

    // ===== Ty le macro (tong = 1.00) =====
    @Column(name = "protein_ratio", precision = 3, scale = 2, nullable = false)
    private BigDecimal proteinRatio;

    @Column(name = "fat_ratio", precision = 3, scale = 2, nullable = false)
    private BigDecimal fatRatio;

    @Column(name = "carb_ratio", precision = 3, scale = 2, nullable = false)
    private BigDecimal carbRatio;

    // ===== Ty le slot (tong = 1.00) =====
    @Column(name = "slot_main_ratio", precision = 3, scale = 2, nullable = false)
    private BigDecimal slotMainRatio;

    @Column(name = "slot_veg_ratio", precision = 3, scale = 2, nullable = false)
    private BigDecimal slotVegRatio;

    @Column(name = "slot_carb_ratio", precision = 3, scale = 2, nullable = false)
    private BigDecimal slotCarbRatio;

    // ===== Trong so scoring (tong = 1.00) =====
    @Column(name = "weight_p", precision = 3, scale = 2, nullable = false)
    private BigDecimal weightP;

    @Column(name = "weight_f", precision = 3, scale = 2, nullable = false)
    private BigDecimal weightF;

    @Column(name = "weight_c", precision = 3, scale = 2, nullable = false)
    private BigDecimal weightC;

    @Column(name = "weight_kcal", precision = 3, scale = 2, nullable = false)
    private BigDecimal weightKcal;

    // ===== Mo ta + audit =====
    @Column(length = 100, nullable = false)
    private String description;

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