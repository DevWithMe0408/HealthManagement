package org.example.nutritionservice.service.recommendation;

import org.example.nutritionservice.domain.recommendation.LoadedConfigs;
import org.example.nutritionservice.domain.recommendation.MacroTarget;
import org.example.nutritionservice.domain.recommendation.MealActual;
import org.example.nutritionservice.entity.config.GoalConfig;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

@Service
public class ScoringService {

    private static final int CALC_SCALE = 4;
    private static final int FINAL_SCALE = 2;
    private static final BigDecimal SKIP_TARGET_THRESHOLD = new BigDecimal("2");
    private static final BigDecimal SCORE_SCALE = new BigDecimal("100");

    public BigDecimal computeMacroScore(
            MealActual actual,
            MacroTarget target,
            GoalConfig goalConfig,
            LoadedConfigs configs) {
        BigDecimal threshold = configs.getDecimal("score.threshold");
        Map<MacroCode, BigDecimal> targets = valuesOf(target);
        Map<MacroCode, BigDecimal> actuals = valuesOf(actual);
        Map<MacroCode, BigDecimal> weights = weightsOf(goalConfig);
        Map<MacroCode, BigDecimal> scores = new EnumMap<>(MacroCode.class);

        BigDecimal skippedWeight = BigDecimal.ZERO;
        int remainingCount = 0;
        for (MacroCode macro : MacroCode.values()) {
            if (targets.get(macro).compareTo(SKIP_TARGET_THRESHOLD) < 0) {
                skippedWeight = skippedWeight.add(weights.get(macro));
                weights.put(macro, BigDecimal.ZERO);
                continue;
            }
            remainingCount++;
            scores.put(macro, scoreFor(macro, actuals.get(macro), targets.get(macro), threshold, configs));
        }

        if (remainingCount == 0) {
            return SCORE_SCALE.setScale(FINAL_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal redistributed = skippedWeight.divide(
                BigDecimal.valueOf(remainingCount),
                CALC_SCALE,
                RoundingMode.HALF_UP
        );
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal adjustedWeightTotal = BigDecimal.ZERO;
        for (MacroCode macro : MacroCode.values()) {
            if (!scores.containsKey(macro)) {
                continue;
            }
            BigDecimal adjustedWeight = weights.get(macro).add(redistributed);
            adjustedWeightTotal = adjustedWeightTotal.add(adjustedWeight);
            total = total.add(adjustedWeight.multiply(scores.get(macro)));
        }

        return total.divide(adjustedWeightTotal, CALC_SCALE, RoundingMode.HALF_UP)
                .multiply(SCORE_SCALE)
                .setScale(FINAL_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scoreFor(
            MacroCode macro,
            BigDecimal actual,
            BigDecimal target,
            BigDecimal threshold,
            LoadedConfigs configs) {
        BigDecimal deviation = actual.subtract(target).abs()
                .divide(target, CALC_SCALE, RoundingMode.HALF_UP);
        if (actual.compareTo(target) > 0) {
            deviation = deviation.multiply(configs.getSurplusPenalty().get(macro.configKey()));
        }
        BigDecimal score = BigDecimal.ONE.subtract(
                deviation.divide(threshold, CALC_SCALE, RoundingMode.HALF_UP)
        );
        return score.max(BigDecimal.ZERO);
    }

    private Map<MacroCode, BigDecimal> valuesOf(MacroTarget target) {
        Map<MacroCode, BigDecimal> values = new EnumMap<>(MacroCode.class);
        values.put(MacroCode.PROTEIN, target.getProteinG());
        values.put(MacroCode.FAT, target.getFatG());
        values.put(MacroCode.CARB, target.getCarbG());
        values.put(MacroCode.KCAL, target.getKcal());
        return values;
    }

    private Map<MacroCode, BigDecimal> valuesOf(MealActual actual) {
        Map<MacroCode, BigDecimal> values = new EnumMap<>(MacroCode.class);
        values.put(MacroCode.PROTEIN, actual.getProteinG());
        values.put(MacroCode.FAT, actual.getFatG());
        values.put(MacroCode.CARB, actual.getCarbG());
        values.put(MacroCode.KCAL, actual.getKcal());
        return values;
    }

    private Map<MacroCode, BigDecimal> weightsOf(GoalConfig goalConfig) {
        Map<MacroCode, BigDecimal> weights = new EnumMap<>(MacroCode.class);
        weights.put(MacroCode.PROTEIN, goalConfig.getWeightP());
        weights.put(MacroCode.FAT, goalConfig.getWeightF());
        weights.put(MacroCode.CARB, goalConfig.getWeightC());
        weights.put(MacroCode.KCAL, goalConfig.getWeightKcal());
        return weights;
    }

    private enum MacroCode {
        PROTEIN,
        FAT,
        CARB,
        KCAL;

        private String configKey() {
            return name();
        }
    }
}
