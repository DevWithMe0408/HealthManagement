package org.example.nutritionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinnedDish {

    @NotBlank
    private String slotKey;

    @NotBlank
    private String dishId;

    @Positive
    private BigDecimal overrideGrams;
}
