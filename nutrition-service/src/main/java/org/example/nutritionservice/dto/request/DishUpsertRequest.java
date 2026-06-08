package org.example.nutritionservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.nutritionservice.entity.catalog.FoodGroup;
import org.example.nutritionservice.entity.catalog.SlotCode;

import java.math.BigDecimal;

@Data
public class DishUpsertRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private SlotCode slotCode;

    @NotNull
    private FoodGroup foodGroupCode;

    // kcal cot precision(7,2) -> toi da 99999.99
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("99999.99")
    private BigDecimal kcalPer100g;

    // protein/fat/carb cot precision(6,2) -> toi da 9999.99
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("9999.99")
    private BigDecimal proteinPer100g;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("9999.99")
    private BigDecimal fatPer100g;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("9999.99")
    private BigDecimal carbPer100g;

    @NotNull
    @Min(1)
    @Max(5000)
    private Integer baseServingG;

    @NotBlank
    @Size(max = 20)
    private String unit;

    @Size(max = 1000)
    private String description;

    // Optional: khi tao neu null -> mac dinh true
    private Boolean isActive;
}
