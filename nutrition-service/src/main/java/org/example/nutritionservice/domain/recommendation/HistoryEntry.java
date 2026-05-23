package org.example.nutritionservice.domain.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.entity.catalog.FoodGroup;
import org.example.nutritionservice.entity.catalog.SlotCode;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryEntry {
    /**
     * 1 dish trong lịch sử, dùng cho penalty
     */

    private LocalDate mealDate;
    private String dishId;
    private FoodGroup foodGroupCode;
    private SlotCode slotCode;
}
