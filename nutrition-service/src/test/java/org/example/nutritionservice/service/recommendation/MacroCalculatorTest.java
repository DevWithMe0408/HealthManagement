package org.example.nutritionservice.service.recommendation;

import org.example.nutritionservice.domain.recommendation.MacroTarget;
import org.example.nutritionservice.entity.config.GoalConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MacroCalculatorTest {

    private final MacroCalculator macroCalculator = new MacroCalculator();

    @Test
    void calculateMacroTargetForGiamGoal() {
        GoalConfig goalConfig = GoalConfig.builder()
                .calMultiplier(new BigDecimal("0.80"))
                .proteinRatio(new BigDecimal("0.35"))
                .fatRatio(new BigDecimal("0.30"))
                .carbRatio(new BigDecimal("0.35"))
                .build();

        BigDecimal dailyKcal = macroCalculator.calculateDailyKcal(new BigDecimal("2000"), goalConfig);
        MacroTarget target = macroCalculator.calculateMacroTarget(dailyKcal, goalConfig);

        assertEquals(new BigDecimal("1600.00"), target.getKcal());
        assertEquals(new BigDecimal("140.00"), target.getProteinG());
        assertEquals(new BigDecimal("53.33"), target.getFatG());
        assertEquals(new BigDecimal("140.00"), target.getCarbG());
    }
}
