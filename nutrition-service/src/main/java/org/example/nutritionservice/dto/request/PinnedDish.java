package org.example.nutritionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinnedDish {

    @NotBlank
    private String slotKey;

    @NotBlank
    private String dishId;
}
