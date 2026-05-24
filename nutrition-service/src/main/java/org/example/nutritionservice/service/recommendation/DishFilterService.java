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

    /**
     * Loc ung vien cho slot, chia target theo so mon va noi bien [0.5x, 1.5x].
     */
    public List<DishCandidate> filterCandidatesForSlot(
            SlotCode slot,
            BigDecimal slotKcalTarget,
            int dishesNeededInSlot,
            LoadedConfigs configs,
            List<Dish> allActiveDishesInSlot) {
        if (dishesNeededInSlot <= 0) {
            return List.of();
        }

        BigDecimal minServing = configs.getDecimal("filter.serving_min");
        BigDecimal maxServing = configs.getDecimal("filter.serving_max");
        BigDecimal targetPerDish = slotKcalTarget.divide(
                BigDecimal.valueOf(dishesNeededInSlot),
                CALC_SCALE,
                RoundingMode.HALF_UP
        );
        BigDecimal lowerBound = targetPerDish.multiply(new BigDecimal("0.5"));
        BigDecimal upperBound = targetPerDish.multiply(new BigDecimal("1.5"));

        return allActiveDishesInSlot.stream()
                .filter(dish -> dish.getSlotCode() == slot && Boolean.TRUE.equals(dish.getIsActive()))
                .map(this::toCandidate)
                .filter(candidate -> candidate.getBaseKcal().multiply(minServing).compareTo(upperBound) <= 0)
                .filter(candidate -> candidate.getBaseKcal().multiply(maxServing).compareTo(lowerBound) >= 0)
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
