package org.example.nutritionservice.domain.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.entity.config.GoalConfig;
import org.example.nutritionservice.entity.config.MealRatioConfig;
import org.example.nutritionservice.entity.config.SlotConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadedConfigs {

    private GoalConfig goalConfig;
    private List<MealRatioConfig> mealRatios;
    private Map<SlotCode, SlotConfig> slotConfigs;
    private Map<Integer, Map<Integer, Integer>> penaltyConfigs;
    private Map<String, BigDecimal> surplusPenalty;
    private Map<String, String> systemConfigs;
    private Map<String, List<BigDecimal>> decimalArrayConfigs;

    public BigDecimal getDecimal(String key) {
        return new BigDecimal(systemConfigs.get(key));
    }

    public Integer getInt(String key) {
        return Integer.parseInt(systemConfigs.get(key));
    }

    public List<BigDecimal> getDecimalArray(String key) {
        return decimalArrayConfigs.getOrDefault(key, List.of());
    }
}
