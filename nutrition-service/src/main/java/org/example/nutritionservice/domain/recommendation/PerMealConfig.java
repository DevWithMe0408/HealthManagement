package org.example.nutritionservice.domain.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.entity.catalog.SlotCode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerMealConfig {

    private MealKind mealKind;
    private Integer nMain;
    private Integer nRau;
    private Integer nCarb;

    public boolean isCombo() {
        return mealKind == MealKind.COMBO;
    }

    public int countForSlot(SlotCode slotCode) {
        if (isCombo()) {
            return slotCode == SlotCode.COMBO ? 1 : 0;
        }
        return switch (slotCode) {
            case CHINH -> defaultZero(nMain);
            case RAU -> defaultZero(nRau);
            case TINH_BOT -> defaultZero(nCarb);
            default -> 0;
        };
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }
}
