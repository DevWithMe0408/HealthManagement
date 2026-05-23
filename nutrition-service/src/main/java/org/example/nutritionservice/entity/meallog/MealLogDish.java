package org.example.nutritionservice.entity.meallog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.entity.catalog.FoodGroup;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "meal_log_dish",
    indexes = {
        @Index(name = "idx_meal_log_dish_meal_log_id", columnList = "meal_log_id"),
        @Index(name = "idx_meal_log_dish_dish_id", columnList = "dish_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealLogDish {
    /**
     * Lich su moi mon an duoc de xuat
     * 1 row = 1 dish trong 1 bua
     */

    @Id
    @UuidGenerator
    @Column(name = "id", length = 36, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "meal_log_id", nullable = false, length = 36)
    private String mealLogId;

    @Column(name = "dish_id", nullable = false, length = 36)
    private String dishId; // FK voi bang dish

    @Enumerated(EnumType.STRING)
    @Column(name = "food_group_code", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private FoodGroup foodGroupCode; // Nhom thuc pham

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_code", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private SlotCode slotCode; // Vi tri - slot cua mon trong thuc don

    @Column(name = "serving_multiplier", nullable = false, precision = 3, scale = 2)
    private BigDecimal servingMultiplier; // khau phan = 0.5 -> 2.0

    @Column(name = "actual_grams", nullable = false, precision = 6, scale = 2)
    private BigDecimal actualGrams; // Khoi luong thuc te cua mon an = base_serving × serving_multiplier (compute va luu san)

    @Column(name = "dish_kcal", nullable = false, precision = 6, scale = 2)
    private BigDecimal dishKcal; // Kcal cua rieng mon nay bua an

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder; // Thu tu hien thi trong bua

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
