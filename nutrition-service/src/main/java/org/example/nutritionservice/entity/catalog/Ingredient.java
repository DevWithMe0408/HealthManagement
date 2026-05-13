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
 * Nguyen lieu (ingredient) — thanh phan cau tao dish.
 *
 * <p>Muc dich chinh:
 * <ul>
 *   <li>Hien thi danh sach thanh phan khi user xem chi tiet dish</li>
 *   <li>Tham chieu toi VN FCT cho macro/100g (read-only, khong tinh macro dish tu ingredient)</li>
 * </ul>
 *
 * <p>Luu y:
 * <ul>
 *   <li>Macro co the NULL (52/203 dong) — ingredient khong co trong VN FCT, fill o production</li>
 *   <li>name UNIQUE — dam bao khong trung lap (da clean ky tu 222 -> 203 unique)</li>
 * </ul>
 */
@Entity
@Table(
    name = "ingredients",
    uniqueConstraints = @UniqueConstraint(name = "uk_ingredients_name", columnNames = "name"),
    indexes = {
        @Index(name = "idx_ingredients_group_code", columnList = "group_code"),
        @Index(name = "idx_ingredients_is_active", columnList = "is_active")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingredient {

    @Id
    @UuidGenerator
    @Column(name = "id", length = 36, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "default_unit", nullable = false, length = 10)
    private String defaultUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_code", nullable = false, length = 20)
    private FoodGroup groupCode;

    // Macro per 100g — co the NULL cho ingredient chua co data
    @Column(name = "kcal_per_100g", precision = 7, scale = 2)
    private BigDecimal kcalPer100g;

    @Column(name = "protein_per_100g", precision = 6, scale = 2)
    private BigDecimal proteinPer100g;

    @Column(name = "fat_per_100g", precision = 6, scale = 2)
    private BigDecimal fatPer100g;

    @Column(name = "carb_per_100g", precision = 6, scale = 2)
    private BigDecimal carbPer100g;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "description", length = 500)
    private String description;

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
