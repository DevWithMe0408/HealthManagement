package org.example.nutritionservice.service.recommendation;

import lombok.extern.slf4j.Slf4j;
import org.example.nutritionservice.domain.recommendation.DishCandidate;
import org.example.nutritionservice.domain.recommendation.HistoryEntry;
import org.example.nutritionservice.domain.recommendation.LoadedConfigs;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.entity.config.SlotConfig;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class PenaltyService {
    /**
     * Tính penalty 3 lớp + favorite discount
     */

    private static final int FINAL_SCALE = 2;

    public BigDecimal computePenalty(
            List<DishCandidate> combo,
            List<HistoryEntry> history,
            Set<String> favoriteDishIds,
            LoadedConfigs configs,
            LocalDate targetMealDate) {
        BigDecimal total = BigDecimal.ZERO;
        for (DishCandidate dish : combo) {
            BigDecimal slotFactor = getSlotFactor(dish, configs);
            BigDecimal favoriteFactor = favoriteDishIds.contains(dish.getDishId())
                    ? configs.getDecimal("penalty.fav_discount")
                    : BigDecimal.ONE;
            for (HistoryEntry historyEntry : history) {
                int distance = distanceDays(targetMealDate, historyEntry.getMealDate());
                if (distance < 0 || distance > 2) {
                    continue;
                }
                BigDecimal contribution = penaltyContribution(
                        dish,
                        historyEntry,
                        distance,
                        slotFactor,
                        favoriteFactor,
                        configs.getPenaltyConfigs()
                );
                total = total.add(contribution);
            }
        }

        BigDecimal cap = BigDecimal.valueOf(configs.getInt("penalty.cap"));
        return total.min(cap).setScale(FINAL_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal penaltyContribution(
            DishCandidate dish,
            HistoryEntry historyEntry,
            int distance,
            BigDecimal slotFactor,
            BigDecimal favoriteFactor,
            Map<Integer, Map<Integer, Integer>> penaltyConfigs) {
        Integer rawPenalty = null;
        if (dish.getDishId().equals(historyEntry.getDishId())) {
            rawPenalty = getRawPenalty(penaltyConfigs, 1, distance);
        } else if (dish.getSlotCode() == SlotCode.CHINH
                && dish.getFoodGroupCode() == historyEntry.getFoodGroupCode()) {
            rawPenalty = getRawPenalty(penaltyConfigs, 2, distance);
        }
        if (rawPenalty == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal contribution = BigDecimal.valueOf(rawPenalty)
                .multiply(slotFactor)
                .multiply(favoriteFactor);
        log.debug("Penalty dishId={} distance={} value={}", dish.getDishId(), distance, contribution);
        return contribution;
    }

    private Integer getRawPenalty(Map<Integer, Map<Integer, Integer>> penaltyConfigs, int layer, int distance) {
        return penaltyConfigs.getOrDefault(layer, Map.of()).get(distance);
    }

    private BigDecimal getSlotFactor(DishCandidate dish, LoadedConfigs configs) {
        SlotConfig slotConfig = configs.getSlotConfigs().get(dish.getSlotCode());
        return slotConfig == null ? BigDecimal.ZERO : slotConfig.getSlotFactor();
    }

    private int distanceDays(LocalDate targetMealDate, LocalDate historyDate) {
        return Math.toIntExact(ChronoUnit.DAYS.between(historyDate, targetMealDate));
    }
}
