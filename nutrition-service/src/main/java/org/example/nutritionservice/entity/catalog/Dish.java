package org.example.nutritionservice.entity.catalog;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dish (mon an) — don vi co ban cua thuc don.
 *
 * <p>Luu macro TU THAN (Cach A da chot) — KHONG tinh tu dish_ingredients.
 * Ly do: ingredient list chi de hien thi, co the thieu macro (52 ingredient NULL).
 *
 * <p>Thuat toan recommendation v3.1 doc cac field:
 * <ul>
 *   <li>slotCode, foodGroupCode -> filter ung vien theo slot/nhom</li>
 *   <li>kcalPer100g, protein/fat/carbPer100g, baseServingG -> tinh kcal/macro cho 1 phan</li>
 *   <li>isActive -> filter dish bi disable</li>
 * </ul>
 */
@Entity
@Table(
    name = "dishes",
    uniqueConstraints = @UniqueConstraint(name = "uk_dishes_name", columnNames = "name"),
    indexes = {
        @Index(name = "idx_dishes_slot_active", columnList = "slot_code, is_active"),
        @Index(name = "idx_dishes_food_group_active", columnList = "food_group_code, is_active")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dish {

    @Id
    @UuidGenerator
    @Column(name = "id", length = 36, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_code", nullable = false, length = 20)
    private SlotCode slotCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_group_code", nullable = false, length = 20)
    private FoodGroup foodGroupCode;

    // Macro per 100g — BAT BUOC cho dish (khong nhu ingredient)
    @Column(name = "kcal_per_100g", nullable = false, precision = 7, scale = 2)
    private BigDecimal kcalPer100g;

    @Column(name = "protein_per_100g", nullable = false, precision = 6, scale = 2)
    private BigDecimal proteinPer100g;

    @Column(name = "fat_per_100g", nullable = false, precision = 6, scale = 2)
    private BigDecimal fatPer100g;

    @Column(name = "carb_per_100g", nullable = false, precision = 6, scale = 2)
    private BigDecimal carbPer100g;

    @Column(name = "base_serving_g", nullable = false)
    private Integer baseServingG;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ===== Audit fields =====
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
