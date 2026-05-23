package org.example.nutritionservice.service.recommendation;

import org.example.nutritionservice.domain.recommendation.MacroTarget;
import org.example.nutritionservice.domain.recommendation.PerMealConfig;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.entity.config.GoalConfig;
import org.example.nutritionservice.entity.config.MealRatioConfig;
import org.example.nutritionservice.entity.meallog.MealType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MacroCalculator {
    /**
     * Tính daily_kcal, meal_kcal, macro_target, slot_target
     */

    private static final int FINAL_SCALE = 2;
    private static final int CALC_SCALE = 4;
    private static final BigDecimal KCAL_PER_G_PROTEIN = new BigDecimal("4");
    private static final BigDecimal KCAL_PER_G_FAT = new BigDecimal("9");
    private static final BigDecimal KCAL_PER_G_CARB = new BigDecimal("4");

    /**
     * Tính daily_kcal = TDEE * cal_multiplier
     * multiply là hàm nhân của BigDecimal
     * setScale quyết định chọn lấy bao nhiêu chữ số thập phân sau dấu phẩy
     * HALF_UP là kiểu làm tròn, từ 5 trở lên thì làm tròn lên
     * @param tdee
     * @param goalConfig
     * @return
     */
    public BigDecimal calculateDailyKcal(BigDecimal tdee, GoalConfig goalConfig) {
        return tdee.multiply(goalConfig.getCalMultiplier()).setScale(FINAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Tinh meal_kcal = daily_kcal * ratio của meal_type
     * @param dailyKcal
     * @param mealType Loại bữa: SANG, TRUA, TOI, PHU_SANG, PHU_CHIEU
     * @param ratios Tỷ lệ của bữa đó
     * @return
     */
    public BigDecimal calculateMealKcal(
            BigDecimal dailyKcal,
            MealType mealType,
            List<MealRatioConfig> ratios) {
        BigDecimal ratio = ratios.stream()
                .filter(item -> mealType.name().equals(item.getMealCode()))
                .map(MealRatioConfig::getRatio)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay meal ratio cho " + mealType));
        return dailyKcal.multiply(ratio).setScale(FINAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Tính macro_target với đơn vị là gram
     * macro_kcal = meal_kcal * macro_ratio
     * macro_target_g = macro_kcal / kcal_per_gram
     * @param mealKcal Kcal của bữa
     * @param goalConfig
     * @return
     */
    public MacroTarget calculateMacroTarget(BigDecimal mealKcal, GoalConfig goalConfig) {
        return MacroTarget.builder()
                .proteinG(toGrams(mealKcal, goalConfig.getProteinRatio(), KCAL_PER_G_PROTEIN))
                .fatG(toGrams(mealKcal, goalConfig.getFatRatio(), KCAL_PER_G_FAT))
                .carbG(toGrams(mealKcal, goalConfig.getCarbRatio(), KCAL_PER_G_CARB))
                .kcal(mealKcal.setScale(FINAL_SCALE, RoundingMode.HALF_UP))
                .build();
    }

    /**
     * Tính tỷ lệ của slot-thành phần món trong một bữa
     * Phân bổ lại khi thiếu slot
     * @param mealKcal kcal của bữa
     * @param goalConfig cấu hình theo mục tiêu
     * @param perMeal
     * @return
     */
    public Map<SlotCode, BigDecimal> calculateSlotKcalTargets(
            BigDecimal mealKcal,
            GoalConfig goalConfig,
            PerMealConfig perMeal) {

        Map<SlotCode, BigDecimal> targets = new LinkedHashMap<>();
        // Nếu là bữa COMBO -> 1 slot duy nhất COMBO = 100%
        if (perMeal.isCombo()) {
            targets.put(SlotCode.COMBO, mealKcal.setScale(FINAL_SCALE, RoundingMode.HALF_UP));
            return targets;
        }

        BigDecimal mainRatio = goalConfig.getSlotMainRatio();
        BigDecimal rauRatio = goalConfig.getSlotVegRatio();
        BigDecimal carbRatio = goalConfig.getSlotCarbRatio();
        boolean hasRau = perMeal.countForSlot(SlotCode.RAU) > 0;
        boolean hasCarb = perMeal.countForSlot(SlotCode.TINH_BOT) > 0;

        // Nếu không có rau và tinh bột -> Có mỗi món chính
        if (!hasRau && !hasCarb) {
            mainRatio = BigDecimal.ONE;
            rauRatio = BigDecimal.ZERO;
            carbRatio = BigDecimal.ZERO;
        } else if (!hasRau) { // Nếu chỉ không có rau
            mainRatio = mainRatio.add(rauRatio.multiply(new BigDecimal("0.60")));
            carbRatio = carbRatio.add(rauRatio.multiply(new BigDecimal("0.40")));
            rauRatio = BigDecimal.ZERO;
        } else if (!hasCarb) { // Nếu chỉ không có tinh bột
            mainRatio = mainRatio.add(carbRatio.multiply(new BigDecimal("0.70")));
            rauRatio = rauRatio.add(carbRatio.multiply(new BigDecimal("0.30")));
            carbRatio = BigDecimal.ZERO;
        }

        putTarget(targets, SlotCode.CHINH, mealKcal, mainRatio, perMeal);
        putTarget(targets, SlotCode.RAU, mealKcal, rauRatio, perMeal);
        putTarget(targets, SlotCode.TINH_BOT, mealKcal, carbRatio, perMeal);
        return targets;
    }

    private BigDecimal toGrams(BigDecimal mealKcal, BigDecimal ratio, BigDecimal kcalPerGram) {
        return mealKcal.multiply(ratio)
                .divide(kcalPerGram, CALC_SCALE, RoundingMode.HALF_UP)
                .setScale(FINAL_SCALE, RoundingMode.HALF_UP);
    }

    private void putTarget(
            Map<SlotCode, BigDecimal> targets,
            SlotCode slotCode,
            BigDecimal mealKcal,
            BigDecimal ratio,
            PerMealConfig perMeal) {
        if (perMeal.countForSlot(slotCode) > 0 && ratio.compareTo(BigDecimal.ZERO) > 0) {
            targets.put(slotCode, mealKcal.multiply(ratio).setScale(FINAL_SCALE, RoundingMode.HALF_UP));
        }
    }
}
