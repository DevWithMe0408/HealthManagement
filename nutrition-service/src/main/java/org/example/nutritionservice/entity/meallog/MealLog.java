package org.example.nutritionservice.entity.meallog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "meal_log",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_meal_log_user_date_type",
        columnNames = {"user_id", "meal_date", "meal_type"}
    ),
    indexes = {
        @Index(name = "idx_meal_log_user_date", columnList = "user_id, meal_date"),
        @Index(name = "idx_meal_log_user_date_type", columnList = "user_id, meal_date, meal_type")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealLog {
    /**
     * Lich su moi bua an duoc de xuat
     * 1 row = 1 bua cua user trong 1 ngay
     */

    @Id
    @UuidGenerator
    @Column(name = "id", length = 36, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId; // lay tu User-id tu header

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate; // Ngay lich cua bua an

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private MealType mealType; // loai bua an - SANG/PHU_SANG/TRUA/PHU_CHIEU/TOI

    @Column(name = "plan_type", nullable = false, length = 10)
    private String planType; // 3_BUA/5_BUA

    @Column(name = "goal_code", nullable = false, length = 20)
    private String goalCode; // GIAM/DUY_TRI/TANG

    @Column(name = "meal_kcal_target", nullable = false, precision = 7, scale = 2)
    private BigDecimal mealKcalTarget; // Luong kcal muc tieu

    @Column(name = "total_kcal_actual", nullable = false, precision = 7, scale = 2)
    private BigDecimal totalKcalActual; // Tong kcal thuc te cua to hop duoc chon

    @Column(name = "total_protein_g", nullable = false, precision = 6, scale = 2)
    private BigDecimal totalProteinG; // Tong protein cua to hop

    @Column(name = "total_fat_g", nullable = false, precision = 6, scale = 2)
    private BigDecimal totalFatG; // Tong fat

    @Column(name = "total_carb_g", nullable = false, precision = 6, scale = 2)
    private BigDecimal totalCarbG; // Tong carb

    @Column(name = "final_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal finalScore; // Diem so cuoi cung cua to hop

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 20,
        columnDefinition = "VARCHAR(20) DEFAULT 'SUGGESTED'"
    )
    @Builder.Default
    private MealStatus status = MealStatus.SUGGESTED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
