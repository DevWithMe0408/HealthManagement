package org.example.nutritionservice.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.nutritionservice.entity.catalog.FoodGroup;
import org.example.nutritionservice.entity.catalog.SlotCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DishAdminResponse {
    private String id;
    private String name;
    private SlotCode slotCode;
    private FoodGroup foodGroupCode;
    private BigDecimal kcalPer100g;
    private BigDecimal proteinPer100g;
    private BigDecimal fatPer100g;
    private BigDecimal carbPer100g;
    private Integer baseServingG;
    private String unit;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
