package org.example.nutritionservice.service.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.request.SystemConfigUpdateRequest;
import org.example.nutritionservice.dto.response.SystemConfigResponse;
import org.example.nutritionservice.entity.config.SlotConfig;
import org.example.nutritionservice.entity.config.SystemConfig;
import org.example.nutritionservice.repository.config.SlotConfigRepository;
import org.example.nutritionservice.repository.config.SystemConfigRepository;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
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
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final SlotConfigRepository slotConfigRepository;
    private final ObjectMapper objectMapper;

    @Override
    public SystemConfigResponse get() {
        Map<String, SystemConfig> sysMap = systemConfigRepository.findAll()
                .stream()
                .collect(Collectors.toMap(SystemConfig::getConfigKey, c -> c));
        List<SlotConfig> slotConfigs = slotConfigRepository.findAll();

        LocalDateTime latestUpdated = findLatestUpdatedAt(sysMap, slotConfigs);
        String latestUpdatedBy = findLatestUpdatedBy(sysMap, slotConfigs);

        return SystemConfigResponse.builder()
                .filter(SystemConfigResponse.FilterConfig.builder()
                        .kcalTolerance(getDecimal(sysMap, "filter.kcal_tolerance"))
                        .servingMin(getDecimal(sysMap, "filter.serving_min"))
                        .servingMax(getDecimal(sysMap, "filter.serving_max"))
                        .servingSteps(parseJsonArray(sysMap, "filter.serving_steps"))
                        .comboServingSteps(parseJsonArray(sysMap, "filter.combo_serving_steps"))
                        .build())
                .constraints(slotConfigs.stream()
                        .map(sc -> SystemConfigResponse.SlotConstraint.builder()
                                .slotCode(sc.getSlotCode())
                                .minG(sc.getMinG())
                                .maxG(sc.getMaxG())
                                .build())
                        .toList())
                .display(SystemConfigResponse.DisplayConfig.builder()
                        .topK(getInt(sysMap, "display.top_k"))
                        .roundStepG(getInt(sysMap, "display.round_step_g"))
                        .build())
                .updatedAt(latestUpdated)
                .updatedBy(latestUpdatedBy)
                .build();
    }

    @Override
    @Transactional
    public SystemConfigResponse update(SystemConfigUpdateRequest req, String updatedBy) {
        SystemConfigUpdateRequest.FilterConfig f = req.getFilter();
        updateKey("filter.kcal_tolerance", f.getKcalTolerance().toString(), "DECIMAL", updatedBy);
        updateKey("filter.serving_min", f.getServingMin().toString(), "DECIMAL", updatedBy);
        updateKey("filter.serving_max", f.getServingMax().toString(), "DECIMAL", updatedBy);
        updateKey("filter.serving_steps", toJsonArray(f.getServingSteps()), "JSON_ARRAY", updatedBy);
        updateKey("filter.combo_serving_steps", toJsonArray(f.getComboServingSteps()), "JSON_ARRAY", updatedBy);

        updateKey("display.top_k", String.valueOf(req.getDisplay().getTopK()), "INT", updatedBy);
        updateKey("display.round_step_g", String.valueOf(req.getDisplay().getRoundStepG()), "INT", updatedBy);

        Map<String, SlotConfig> slotMap = slotConfigRepository.findAll().stream()
                .collect(Collectors.toMap(SlotConfig::getSlotCode, s -> s));
        for (SystemConfigUpdateRequest.SlotConstraint constraint : req.getConstraints()) {
            if (constraint.getMaxG() <= constraint.getMinG()) {
                throw new BusinessException(ErrorCode.CONFIG_VALIDATION_FAILED,
                        "maxG phai lon hon minG cho slot " + constraint.getSlotCode());
            }
            SlotConfig sc = slotMap.get(constraint.getSlotCode());
            if (sc != null) {
                sc.setMinG(constraint.getMinG());
                sc.setMaxG(constraint.getMaxG());
                sc.setUpdatedBy(updatedBy);
                slotConfigRepository.save(sc);
            }
        }

        return get();
    }

    private void updateKey(String key, String value, String valueType, String updatedBy) {
        SystemConfig config = systemConfigRepository.findById(key)
                .orElse(new SystemConfig(key, value, valueType, null, true, null, null, null, null));
        config.setConfigValue(value);
        config.setUpdatedBy(updatedBy);
        systemConfigRepository.save(config);
    }

    private BigDecimal getDecimal(Map<String, SystemConfig> map, String key) {
        SystemConfig c = map.get(key);
        return c != null ? new BigDecimal(c.getConfigValue()) : null;
    }

    private Integer getInt(Map<String, SystemConfig> map, String key) {
        SystemConfig c = map.get(key);
        return c != null ? Integer.parseInt(c.getConfigValue()) : null;
    }

    private List<BigDecimal> parseJsonArray(Map<String, SystemConfig> map, String key) {
        SystemConfig c = map.get(key);
        if (c == null) return List.of();
        try {
            return objectMapper.readValue(c.getConfigValue(), new TypeReference<List<BigDecimal>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJsonArray(List<BigDecimal> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            return "[]";
        }
    }

    private LocalDateTime findLatestUpdatedAt(Map<String, SystemConfig> sysMap, List<SlotConfig> slotConfigs) {
        return Stream.concat(
                        sysMap.values().stream().map(SystemConfig::getUpdatedAt),
                        slotConfigs.stream().map(SlotConfig::getUpdatedAt))
                .filter(dt -> dt != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private String findLatestUpdatedBy(Map<String, SystemConfig> sysMap, List<SlotConfig> slotConfigs) {
        LocalDateTime latestSys = sysMap.values().stream()
                .map(SystemConfig::getUpdatedAt)
                .filter(dt -> dt != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime latestSlot = slotConfigs.stream()
                .map(SlotConfig::getUpdatedAt)
                .filter(dt -> dt != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        if (latestSys == null && latestSlot == null) return null;
        if (latestSys == null) return slotConfigs.stream()
                .max(Comparator.comparing(SlotConfig::getUpdatedAt))
                .map(SlotConfig::getUpdatedBy).orElse(null);
        if (latestSlot == null) return sysMap.values().stream()
                .max(Comparator.comparing(SystemConfig::getUpdatedAt))
                .map(SystemConfig::getUpdatedBy).orElse(null);

        return latestSys.isAfter(latestSlot)
                ? sysMap.values().stream().max(Comparator.comparing(SystemConfig::getUpdatedAt))
                        .map(SystemConfig::getUpdatedBy).orElse(null)
                : slotConfigs.stream().max(Comparator.comparing(SlotConfig::getUpdatedAt))
                        .map(SlotConfig::getUpdatedBy).orElse(null);
    }
}
