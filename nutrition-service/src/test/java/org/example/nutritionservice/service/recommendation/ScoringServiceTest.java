package org.example.nutritionservice.service.recommendation;

import org.example.nutritionservice.domain.recommendation.LoadedConfigs;
import org.example.nutritionservice.domain.recommendation.MacroTarget;
import org.example.nutritionservice.domain.recommendation.MealActual;
import org.example.nutritionservice.entity.config.GoalConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoringServiceTest {

    private final ScoringService scoringService = new ScoringService();

    @Test
    void skipSmallMacroTargetAndRedistributeWeight() {
        GoalConfig goalConfig = GoalConfig.builder()
                .weightP(new BigDecimal("0.45"))
                .weightF(new BigDecimal("0.20"))
                .weightC(new BigDecimal("0.25"))
                .weightKcal(new BigDecimal("0.10"))
                .build();
        LoadedConfigs configs = LoadedConfigs.builder()
                .systemConfigs(Map.of("score.threshold", "0.20"))
                .surplusPenalty(Map.of(
                        "PROTEIN", new BigDecimal("0.3"),
                        "FAT", new BigDecimal("0.8"),
                        "CARB", new BigDecimal("0.5"),
                        "KCAL", new BigDecimal("0.7")
                ))
                .build();
        MacroTarget target = MacroTarget.builder()
                .proteinG(new BigDecimal("20.00"))
                .fatG(new BigDecimal("10.00"))
                .carbG(new BigDecimal("1.00"))
                .kcal(new BigDecimal("174.00"))
                .build();
        MealActual actual = MealActual.builder()
                .proteinG(new BigDecimal("20.00"))
                .fatG(new BigDecimal("10.00"))
                .carbG(BigDecimal.ZERO)
                .kcal(new BigDecimal("174.00"))
                .build();

        BigDecimal score = scoringService.computeMacroScore(actual, target, goalConfig, configs);

        assertEquals(new BigDecimal("100.00"), score);
    }
}
