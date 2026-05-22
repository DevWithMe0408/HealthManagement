package org.example.nutritionservice.service.recommendation;

import org.example.nutritionservice.domain.recommendation.DishCandidate;
import org.example.nutritionservice.domain.recommendation.LoadedConfigs;
import org.example.nutritionservice.entity.catalog.Dish;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class DishFilterService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int CALC_SCALE = 4;

    public List<DishCandidate> filterCandidatesForSlot(
            SlotCode slot,
            BigDecimal slotKcalTarget,
            LoadedConfigs configs,
            List<Dish> allActiveDishesInSlot) {
        BigDecimal tolerance = configs.getDecimal("filter.kcal_tolerance");
        BigDecimal minServing = configs.getDecimal("filter.serving_min");
        BigDecimal maxServing = configs.getDecimal("filter.serving_max");
        BigDecimal maxAcceptedMin = slotKcalTarget.multiply(BigDecimal.ONE.add(tolerance));
        BigDecimal minAcceptedMax = slotKcalTarget.multiply(BigDecimal.ONE.subtract(tolerance));

        return allActiveDishesInSlot.stream()
                .filter(dish -> dish.getSlotCode() == slot && Boolean.TRUE.equals(dish.getIsActive()))
                .map(this::toCandidate)
                .filter(candidate -> candidate.getBaseKcal().multiply(minServing).compareTo(maxAcceptedMin) <= 0)
                .filter(candidate -> candidate.getBaseKcal().multiply(maxServing).compareTo(minAcceptedMax) >= 0)
                .toList();
    }

    public DishCandidate toCandidate(Dish dish) {
        BigDecimal baseServingRatio = BigDecimal.valueOf(dish.getBaseServingG())
                .divide(ONE_HUNDRED, CALC_SCALE, RoundingMode.HALF_UP);
        return DishCandidate.builder()
                .dish(dish)
                .baseKcal(dish.getKcalPer100g().multiply(baseServingRatio).setScale(CALC_SCALE, RoundingMode.HALF_UP))
                .baseProteinG(dish.getProteinPer100g().multiply(baseServingRatio).setScale(CALC_SCALE, RoundingMode.HALF_UP))
                .baseFatG(dish.getFatPer100g().multiply(baseServingRatio).setScale(CALC_SCALE, RoundingMode.HALF_UP))
                .baseCarbG(dish.getCarbPer100g().multiply(baseServingRatio).setScale(CALC_SCALE, RoundingMode.HALF_UP))
                .build();
    }
}
