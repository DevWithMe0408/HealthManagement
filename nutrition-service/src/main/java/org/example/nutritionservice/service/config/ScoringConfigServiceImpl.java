package org.example.nutritionservice.service.config;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.request.ScoringConfigUpdateRequest;
import org.example.nutritionservice.dto.response.ScoringConfigResponse;
import org.example.nutritionservice.entity.config.SurplusPenaltyConfig;
import org.example.nutritionservice.entity.config.SystemConfig;
import org.example.nutritionservice.repository.config.SurplusPenaltyConfigRepository;
import org.example.nutritionservice.repository.config.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ScoringConfigServiceImpl implements ScoringConfigService {

    private final SurplusPenaltyConfigRepository surplusPenaltyConfigRepository;
    private final SystemConfigRepository systemConfigRepository;

    @Override
    public ScoringConfigResponse get() {
        Map<String, SurplusPenaltyConfig> surplusMap = surplusPenaltyConfigRepository.findAll()
                .stream()
                .collect(Collectors.toMap(SurplusPenaltyConfig::getMacroCode, c -> c));
        Map<String, SystemConfig> sysMap = getScoringSysMap();

        LocalDateTime latestUpdated = findLatestUpdatedAt(surplusMap, sysMap);
        String latestUpdatedBy = findLatestUpdatedBy(surplusMap, sysMap);

        return ScoringConfigResponse.builder()
                .threshold(getDecimal(sysMap, "score.threshold"))
                .surplusFactors(ScoringConfigResponse.SurplusFactors.builder()
                        .protein(getFactor(surplusMap, "PROTEIN"))
                        .fat(getFactor(surplusMap, "FAT"))
                        .carb(getFactor(surplusMap, "CARB"))
                        .kcal(getFactor(surplusMap, "KCAL"))
                        .build())
                .reoptimize(ScoringConfigResponse.ReoptimizeParams.builder()
                        .scoreThreshold(getInt(sysMap, "reopt.score_threshold"))
                        .scoreDrop(getInt(sysMap, "reopt.score_drop"))
                        .build())
                .updatedAt(latestUpdated)
                .updatedBy(latestUpdatedBy)
                .build();
    }

    @Override
    @Transactional
    public ScoringConfigResponse update(ScoringConfigUpdateRequest req, String updatedBy) {
        updateSystemKey("score.threshold", req.getThreshold().toString(), updatedBy);
        updateSystemKey("reopt.score_threshold", String.valueOf(req.getReoptimize().getScoreThreshold()), updatedBy);
        updateSystemKey("reopt.score_drop", String.valueOf(req.getReoptimize().getScoreDrop()), updatedBy);

        updateSurplusConfig("PROTEIN", req.getSurplusFactors().getProtein(), updatedBy);
        updateSurplusConfig("FAT", req.getSurplusFactors().getFat(), updatedBy);
        updateSurplusConfig("CARB", req.getSurplusFactors().getCarb(), updatedBy);
        updateSurplusConfig("KCAL", req.getSurplusFactors().getKcal(), updatedBy);

        return get();
    }

    private void updateSystemKey(String key, String value, String updatedBy) {
        SystemConfig config = systemConfigRepository.findById(key)
                .orElse(new SystemConfig(key, value, "DECIMAL", null, true, null, null, null, null));
        config.setConfigValue(value);
        config.setUpdatedBy(updatedBy);
        systemConfigRepository.save(config);
    }

    private void updateSurplusConfig(String macroCode, BigDecimal factor, String updatedBy) {
        SurplusPenaltyConfig config = surplusPenaltyConfigRepository.findById(macroCode)
                .orElse(new SurplusPenaltyConfig(macroCode, factor, null, null, null, null));
        config.setFactor(factor);
        config.setUpdatedBy(updatedBy);
        surplusPenaltyConfigRepository.save(config);
    }

    private Map<String, SystemConfig> getScoringSysMap() {
        List<String> keys = List.of("score.threshold", "reopt.score_threshold", "reopt.score_drop");
        return systemConfigRepository.findAll().stream()
                .filter(c -> keys.contains(c.getConfigKey()))
                .collect(Collectors.toMap(SystemConfig::getConfigKey, c -> c));
    }

    private BigDecimal getFactor(Map<String, SurplusPenaltyConfig> map, String macroCode) {
        SurplusPenaltyConfig c = map.get(macroCode);
        return c != null ? c.getFactor() : BigDecimal.ZERO;
    }

    private Integer getInt(Map<String, SystemConfig> map, String key) {
        SystemConfig c = map.get(key);
        return c != null ? Integer.parseInt(c.getConfigValue()) : null;
    }

    private BigDecimal getDecimal(Map<String, SystemConfig> map, String key) {
        SystemConfig c = map.get(key);
        return c != null ? new BigDecimal(c.getConfigValue()) : null;
    }

    private LocalDateTime findLatestUpdatedAt(Map<String, SurplusPenaltyConfig> surplusMap,
                                              Map<String, SystemConfig> sysMap) {
        return Stream.concat(
                        surplusMap.values().stream().map(SurplusPenaltyConfig::getUpdatedAt),
                        sysMap.values().stream().map(SystemConfig::getUpdatedAt))
                .filter(dt -> dt != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private String findLatestUpdatedBy(Map<String, SurplusPenaltyConfig> surplusMap,
                                       Map<String, SystemConfig> sysMap) {
        LocalDateTime latestSurplus = surplusMap.values().stream()
                .map(SurplusPenaltyConfig::getUpdatedAt)
                .filter(dt -> dt != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime latestSys = sysMap.values().stream()
                .map(SystemConfig::getUpdatedAt)
                .filter(dt -> dt != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        if (latestSurplus == null && latestSys == null) return null;
        if (latestSurplus == null) return getLatestUpdatedBy(sysMap.values().stream()
                .max(Comparator.comparing(SystemConfig::getUpdatedAt))
                .orElse(null));
        if (latestSys == null) return getLatestUpdatedBy(surplusMap.values().stream()
                .max(Comparator.comparing(SurplusPenaltyConfig::getUpdatedAt))
                .orElse(null));

        if (latestSurplus.isAfter(latestSys)) {
            return surplusMap.values().stream()
                    .max(Comparator.comparing(SurplusPenaltyConfig::getUpdatedAt))
                    .map(SurplusPenaltyConfig::getUpdatedBy)
                    .orElse(null);
        } else {
            return sysMap.values().stream()
                    .max(Comparator.comparing(SystemConfig::getUpdatedAt))
                    .map(SystemConfig::getUpdatedBy)
                    .orElse(null);
        }
    }

    private String getLatestUpdatedBy(SystemConfig c) {
        return c != null ? c.getUpdatedBy() : null;
    }

    private String getLatestUpdatedBy(SurplusPenaltyConfig c) {
        return c != null ? c.getUpdatedBy() : null;
    }
}
