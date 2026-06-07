package org.example.nutritionservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CatalogStatsResponse {

    private long dishTotal;
    private long dishActive;
    private long ingredientTotal;
    private long ingredientWithMacro;
    private long mealLogTotal;
    private Map<String, Long> dishCountBySlot;
}
