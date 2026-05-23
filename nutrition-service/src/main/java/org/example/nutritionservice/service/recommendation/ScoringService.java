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

    /**
     * Tính Macro Score
     * @param actual lượng macro thực tế của bữa ăn
     * @param target lượng macro mục tiêu
     * @param goalConfig cấu hình theo từng mục tiêu
     * @param configs
     * @return
     */
    public BigDecimal computeMacroScore(
            MealActual actual,
            MacroTarget target,
            GoalConfig goalConfig,
            LoadedConfigs configs) {

        BigDecimal threshold = configs.getDecimal("score.threshold");// Lấy ngưỡng sai lệch cho phép
        Map<MacroCode, BigDecimal> targets = valuesOf(target);
        Map<MacroCode, BigDecimal> actuals = valuesOf(actual);
        Map<MacroCode, BigDecimal> weights = weightsOf(goalConfig);
        Map<MacroCode, BigDecimal> scores = new EnumMap<>(MacroCode.class);

        BigDecimal skippedWeight = BigDecimal.ZERO;
        int remainingCount = 0;
        // Vòng gặp qua từng Macro -> Nếu target của macro nhỏ hơn 2 thì bỏ qua Macro đó
        for (MacroCode macro : MacroCode.values()) {
            if (targets.get(macro).compareTo(SKIP_TARGET_THRESHOLD) < 0) {
                skippedWeight = skippedWeight.add(weights.get(macro));
                weights.put(macro, BigDecimal.ZERO);
                continue;
            }
            // Tính điểm từng macro còn lại
            remainingCount++;
            scores.put(macro, scoreFor(macro, actuals.get(macro), targets.get(macro), threshold, configs));
        }

        // Trường hợp tất cả macro đều bị bỏ qua -> Không có target đủ lớn để đánh giá
        if (remainingCount == 0) {
            return SCORE_SCALE.setScale(FINAL_SCALE, RoundingMode.HALF_UP);
        }

        // Phân phối lại trọng số của macro bị bỏ qua -> Chia đều cho các macro còn lại
        BigDecimal redistributed = skippedWeight.divide(
                BigDecimal.valueOf(remainingCount),
                CALC_SCALE,
                RoundingMode.HALF_UP
        );
        // Tổng điểm
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal adjustedWeightTotal = BigDecimal.ZERO; // Tổng trọng số sau điều chỉnh
        for (MacroCode macro : MacroCode.values()) {
            if (!scores.containsKey(macro)) { // Bỏ qua các macro bị skip
                continue;
            }
            BigDecimal adjustedWeight = weights.get(macro).add(redistributed); // Trọng số gốc + phân trọng số chia lại
            adjustedWeightTotal = adjustedWeightTotal.add(adjustedWeight);
            total = total.add(adjustedWeight.multiply(scores.get(macro))); // total += adjustedWeight * score
        }

        // finalScore = total / adjustedWeightTotal * 100
        return total.divide(adjustedWeightTotal, CALC_SCALE, RoundingMode.HALF_UP)
                .multiply(SCORE_SCALE)
                .setScale(FINAL_SCALE, RoundingMode.HALF_UP);
    }

    // Hàm tính điểm riêng cho Macro đó
    private BigDecimal scoreFor(
            MacroCode macro,
            BigDecimal actual,
            BigDecimal target,
            BigDecimal threshold,
            LoadedConfigs configs) {

        // Tính độ lệch so với target
        BigDecimal deviation = actual.subtract(target).abs()
                .divide(target, CALC_SCALE, RoundingMode.HALF_UP);

        // Vượt target -> Nhân penalty -> Lấy trọng số phạt trong cấu hình
        // deviation = deviation * surplusPenalty[macro]
        if (actual.compareTo(target) > 0) {
            deviation = deviation.multiply(configs.getSurplusPenalty().get(macro.configKey()));
        }
        BigDecimal score = BigDecimal.ONE.subtract(
                deviation.divide(threshold, CALC_SCALE, RoundingMode.HALF_UP)
        );
        return score.max(BigDecimal.ZERO);
    }

    // Chuyển MacroTarget thành dạng map
    private Map<MacroCode, BigDecimal> valuesOf(MacroTarget target) {
        Map<MacroCode, BigDecimal> values = new EnumMap<>(MacroCode.class);
        values.put(MacroCode.PROTEIN, target.getProteinG());
        values.put(MacroCode.FAT, target.getFatG());
        values.put(MacroCode.CARB, target.getCarbG());
        values.put(MacroCode.KCAL, target.getKcal());
        return values;
    }

    // Chuyển MealActual thành dạng map -> Tại sao không gộp 2 hàm vào làm 1 ?
    private Map<MacroCode, BigDecimal> valuesOf(MealActual actual) {
        Map<MacroCode, BigDecimal> values = new EnumMap<>(MacroCode.class);
        values.put(MacroCode.PROTEIN, actual.getProteinG());
        values.put(MacroCode.FAT, actual.getFatG());
        values.put(MacroCode.CARB, actual.getCarbG());
        values.put(MacroCode.KCAL, actual.getKcal());
        return values;
    }

    // Chuyển trọng số từ GoalConfig thành dạng map
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
