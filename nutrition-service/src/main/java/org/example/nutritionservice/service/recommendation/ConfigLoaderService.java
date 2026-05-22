package org.example.nutritionservice.service.recommendation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.domain.recommendation.LoadedConfigs;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.entity.config.PenaltyConfig;
import org.example.nutritionservice.entity.config.SystemConfig;
import org.example.nutritionservice.repository.config.GoalConfigRepository;
import org.example.nutritionservice.repository.config.MealRatioConfigRepository;
import org.example.nutritionservice.repository.config.PenaltyConfigRepository;
import org.example.nutritionservice.repository.config.SlotConfigRepository;
import org.example.nutritionservice.repository.config.SurplusPenaltyConfigRepository;
import org.example.nutritionservice.repository.config.SystemConfigRepository;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfigLoaderService {

    private final GoalConfigRepository goalConfigRepository;
    private final MealRatioConfigRepository mealRatioConfigRepository;
    private final SlotConfigRepository slotConfigRepository;
    private final PenaltyConfigRepository penaltyConfigRepository;
    private final SurplusPenaltyConfigRepository surplusPenaltyConfigRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final ObjectMapper objectMapper;

    public LoadedConfigs loadForRecommendation(String goalCode, String planType) {
        List<SystemConfig> systemConfigs = systemConfigRepository.findAll();
        Map<String, String> rawSystemConfigs = systemConfigs.stream()
                .collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue));

        return LoadedConfigs.builder()
                .goalConfig(goalConfigRepository.findById(goalCode)
                        .orElseThrow(() -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND)))
                .mealRatios(mealRatioConfigRepository.findByPlanTypeOrderBySortOrderAsc(planType))
                .slotConfigs(slotConfigRepository.findAll().stream()
                        .collect(Collectors.toMap(
                                slot -> SlotCode.valueOf(slot.getSlotCode()),
                                slot -> slot,
                                (first, second) -> first,
                                LinkedHashMap::new
                        )))
                .penaltyConfigs(toPenaltyMap(penaltyConfigRepository.findAll()))
                .surplusPenalty(surplusPenaltyConfigRepository.findAll().stream()
                        .collect(Collectors.toMap(config -> config.getMacroCode(), config -> config.getFactor())))
                .systemConfigs(rawSystemConfigs)
                .decimalArrayConfigs(systemConfigs.stream()
                        .filter(config -> "JSON_ARRAY".equals(config.getValueType()))
                        .collect(Collectors.toMap(SystemConfig::getConfigKey, this::parseDecimalArray)))
                .build();
    }

    public BigDecimal getDecimal(String key) {
        return new BigDecimal(getSystemValue(key));
    }

    public Integer getInt(String key) {
        return Integer.parseInt(getSystemValue(key));
    }

    public List<BigDecimal> getDecimalArray(String key) {
        return parseDecimalArray(systemConfigRepository.findById(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND)));
    }

    private Map<Integer, Map<Integer, Integer>> toPenaltyMap(List<PenaltyConfig> configs) {
        return configs.stream()
                .collect(Collectors.groupingBy(
                        PenaltyConfig::getLayer,
                        Collectors.toMap(PenaltyConfig::getDistanceDays, PenaltyConfig::getPenaltyValue)
                ));
    }

    private String getSystemValue(String key) {
        return systemConfigRepository.findById(key)
                .map(SystemConfig::getConfigValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND));
    }

    private List<BigDecimal> parseDecimalArray(SystemConfig config) {
        try {
            return objectMapper.readValue(config.getConfigValue(), new TypeReference<List<BigDecimal>>() {});
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CONFIG_VALIDATION_FAILED,
                    "Gia tri JSON_ARRAY khong hop le cho config " + config.getConfigKey());
        }
    }
}
