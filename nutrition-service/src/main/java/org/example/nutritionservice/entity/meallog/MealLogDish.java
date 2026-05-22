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

    @Id
    @UuidGenerator
    @Column(name = "id", length = 36, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "meal_log_id", nullable = false, length = 36)
    private String mealLogId;

    @Column(name = "dish_id", nullable = false, length = 36)
    private String dishId;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_group_code", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private FoodGroup foodGroupCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_code", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private SlotCode slotCode;

    @Column(name = "serving_multiplier", nullable = false, precision = 3, scale = 2)
    private BigDecimal servingMultiplier;

    @Column(name = "actual_grams", nullable = false, precision = 6, scale = 2)
    private BigDecimal actualGrams;

    @Column(name = "dish_kcal", nullable = false, precision = 6, scale = 2)
    private BigDecimal dishKcal;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
