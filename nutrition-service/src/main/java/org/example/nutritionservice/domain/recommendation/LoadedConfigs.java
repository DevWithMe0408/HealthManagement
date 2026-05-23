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

    private GoalConfig goalConfig; // 1 goal
    private List<MealRatioConfig> mealRatios; //ratios cho planType được chọn (ratios cho 3 bữa/5 bữa)
    private Map<SlotCode, SlotConfig> slotConfigs; // 4 slot (CHINH/RAU/TINH_BOT/COMBO)
    private Map<Integer, Map<Integer, Integer>> penaltyConfigs; //layer (lớp nào) -> distance (ktg cách bao lâu) -> penalty_value (điểm phạt)
    private Map<String, BigDecimal> surplusPenalty; //PROTEIN/FAT/CARB/KCAL -> penalty_value khi thừa
    private Map<String, String> systemConfigs;
    private Map<String, List<BigDecimal>> decimalArrayConfigs;

    // convert system_configs string value to BigDecimal
    public BigDecimal getDecimal(String key) {
        return new BigDecimal(systemConfigs.get(key));
    }

    public Integer getInt(String key) {
        return Integer.parseInt(systemConfigs.get(key));
    }

    // parse JSON_ARRAY
    public List<BigDecimal> getDecimalArray(String key) {
        return decimalArrayConfigs.getOrDefault(key, List.of());
    }
}
