package org.example.nutritionservice.entity.catalog;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bang noi dish - ingredient (join table) voi metadata.
 *
 * <p>Moi row = 1 nguyen lieu trong 1 dish, voi quantity cu the.
 *
 * <p>Luu y:
 * <ul>
 *   <li>dishId va ingredientId la FK LONG (khong co constraint, tuan theo database-per-service)</li>
 *   <li>Tich hop du lieu thuc hien o application layer (service code)</li>
 *   <li>UNIQUE (dish_id, ingredient_id) -> 1 dish chi co 1 quantity duy nhat cua 1 ingredient</li>
 *   <li>KHONG co updated_by/updated_at — junction table chi insert/delete, khong update</li>
 * </ul>
 */
@Entity
@Table(
    name = "dish_ingredients",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_dish_ingredient",
        columnNames = {"dish_id", "ingredient_id"}
    ),
    indexes = {
        @Index(name = "idx_dish_ingredients_dish_id", columnList = "dish_id"),
        @Index(name = "idx_dish_ingredients_ingredient_id", columnList = "ingredient_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishIngredient {

    @Id
    @UuidGenerator
    @Column(name = "id", length = 36, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "dish_id", nullable = false, length = 36)
    private String dishId;

    @Column(name = "ingredient_id", nullable = false, length = 36)
    private String ingredientId;

    @Column(name = "quantity", nullable = false, precision = 8, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit", nullable = false, length = 10)
    private String unit;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
