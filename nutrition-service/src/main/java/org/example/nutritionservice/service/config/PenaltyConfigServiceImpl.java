package org.example.nutritionservice.service.config;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.request.PenaltyConfigUpdateRequest;
import org.example.nutritionservice.dto.response.PenaltyConfigResponse;
import org.example.nutritionservice.entity.config.PenaltyConfig;
import org.example.nutritionservice.entity.config.PenaltyConfigId;
import org.example.nutritionservice.entity.config.SlotConfig;
import org.example.nutritionservice.entity.config.SystemConfig;
import org.example.nutritionservice.repository.config.PenaltyConfigRepository;
import org.example.nutritionservice.repository.config.SlotConfigRepository;
import org.example.nutritionservice.repository.config.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PenaltyConfigServiceImpl implements PenaltyConfigService {

    private final PenaltyConfigRepository penaltyConfigRepository;
    private final SlotConfigRepository slotConfigRepository;
    private final SystemConfigRepository systemConfigRepository;

    @Override
    public PenaltyConfigResponse get() {
        List<PenaltyConfig> layer1List = penaltyConfigRepository.findByLayerOrderByDistanceDaysAsc(1);
        List<PenaltyConfig> layer2List = penaltyConfigRepository.findByLayerOrderByDistanceDaysAsc(2);
        Map<String, SlotConfig> slotMap = slotConfigRepository.findAll().stream()
                .collect(Collectors.toMap(SlotConfig::getSlotCode, s -> s));
        Map<String, SystemConfig> sysMap = getSystemConfigMap();

        LocalDateTime latestUpdated = findLatestUpdatedAt(layer1List, layer2List, slotMap, sysMap);
        String latestUpdatedBy = findLatestUpdatedBy(layer1List, layer2List);

        return PenaltyConfigResponse.builder()
                .layer1(toLayerPenalty(layer1List))
                .layer2(toLayerPenalty(layer2List))
                .slotFactors(PenaltyConfigResponse.SlotFactors.builder()
                        .main(getSlotFactor(slotMap, "CHINH"))
                        .veg(getSlotFactor(slotMap, "RAU"))
                        .carb(getSlotFactor(slotMap, "TINH_BOT"))
                        .combo(getSlotFactor(slotMap, "COMBO"))
                        .build())
                .others(PenaltyConfigResponse.OtherParams.builder()
                        .penaltyCap(getInt(sysMap, "penalty.cap"))
                        .favoriteDiscount(getDecimal(sysMap, "penalty.fav_discount"))
                        .lookbackDays(getInt(sysMap, "penalty.lookback_days"))
                        .build())
                .updatedAt(latestUpdated)
                .updatedBy(latestUpdatedBy)
                .build();
    }

    @Override
    @Transactional
    public PenaltyConfigResponse update(PenaltyConfigUpdateRequest req, String updatedBy) {
        updateLayerPenalty(1, req.getLayer1(), updatedBy);
        updateLayerPenalty(2, req.getLayer2(), updatedBy);

        Map<String, SlotConfig> slotMap = slotConfigRepository.findAll().stream()
                .collect(Collectors.toMap(SlotConfig::getSlotCode, s -> s));
        updateSlotFactor(slotMap, "CHINH", req.getSlotFactors().getMain(), updatedBy);
        updateSlotFactor(slotMap, "RAU", req.getSlotFactors().getVeg(), updatedBy);
        updateSlotFactor(slotMap, "TINH_BOT", req.getSlotFactors().getCarb(), updatedBy);
        updateSlotFactor(slotMap, "COMBO", req.getSlotFactors().getCombo(), updatedBy);

        updateSystemKey("penalty.cap", String.valueOf(req.getOthers().getPenaltyCap()), updatedBy);
        updateSystemKey("penalty.fav_discount", req.getOthers().getFavoriteDiscount().toString(), updatedBy);
        updateSystemKey("penalty.lookback_days", String.valueOf(req.getOthers().getLookbackDays()), updatedBy);

        return get();
    }

    private void updateLayerPenalty(int layer, PenaltyConfigUpdateRequest.LayerPenalty layerReq, String updatedBy) {
        saveOrUpdate(layer, 0, layerReq.getSameDay(), updatedBy);
        saveOrUpdate(layer, 1, layerReq.getOneDayBefore(), updatedBy);
        saveOrUpdate(layer, 2, layerReq.getTwoDayBefore(), updatedBy);
    }

    private void saveOrUpdate(int layer, int distanceDays, int value, String updatedBy) {
        PenaltyConfigId id = new PenaltyConfigId(layer, distanceDays);
        PenaltyConfig config = penaltyConfigRepository.findById(id)
                .orElse(new PenaltyConfig(layer, distanceDays, value, null, null, null, null));
        config.setPenaltyValue(value);
        config.setUpdatedBy(updatedBy);
        penaltyConfigRepository.save(config);
    }

    private void updateSlotFactor(Map<String, SlotConfig> slotMap, String slotCode, BigDecimal factor, String updatedBy) {
        SlotConfig sc = slotMap.get(slotCode);
        if (sc != null) {
            sc.setSlotFactor(factor);
            sc.setUpdatedBy(updatedBy);
            slotConfigRepository.save(sc);
        }
    }

    private void updateSystemKey(String key, String value, String updatedBy) {
        SystemConfig config = systemConfigRepository.findById(key)
                .orElse(new SystemConfig(key, value, "DECIMAL", null, true, null, null, null, null));
        config.setConfigValue(value);
        config.setUpdatedBy(updatedBy);
        systemConfigRepository.save(config);
    }

    private PenaltyConfigResponse.LayerPenalty toLayerPenalty(List<PenaltyConfig> list) {
        Map<Integer, Integer> byDistance = list.stream()
                .collect(Collectors.toMap(PenaltyConfig::getDistanceDays, PenaltyConfig::getPenaltyValue));
        return PenaltyConfigResponse.LayerPenalty.builder()
                .sameDay(byDistance.getOrDefault(0, 0))
                .oneDayBefore(byDistance.getOrDefault(1, 0))
                .twoDayBefore(byDistance.getOrDefault(2, 0))
                .build();
    }

    private BigDecimal getSlotFactor(Map<String, SlotConfig> slotMap, String slotCode) {
        SlotConfig sc = slotMap.get(slotCode);
        return sc != null ? sc.getSlotFactor() : BigDecimal.ZERO;
    }

    private Map<String, SystemConfig> getSystemConfigMap() {
        return systemConfigRepository.findByConfigKeyStartingWith("penalty.")
                .stream()
                .collect(Collectors.toMap(SystemConfig::getConfigKey, c -> c));
    }

    private Integer getInt(Map<String, SystemConfig> map, String key) {
        SystemConfig c = map.get(key);
        return c != null ? Integer.parseInt(c.getConfigValue()) : null;
    }

    private BigDecimal getDecimal(Map<String, SystemConfig> map, String key) {
        SystemConfig c = map.get(key);
        return c != null ? new BigDecimal(c.getConfigValue()) : null;
    }

    private LocalDateTime findLatestUpdatedAt(List<PenaltyConfig> l1, List<PenaltyConfig> l2,
                                              Map<String, SlotConfig> slotMap, Map<String, SystemConfig> sysMap) {
        return java.util.stream.Stream.of(
                        l1.stream().map(PenaltyConfig::getUpdatedAt),
                        l2.stream().map(PenaltyConfig::getUpdatedAt),
                        slotMap.values().stream().map(SlotConfig::getUpdatedAt),
                        sysMap.values().stream().map(SystemConfig::getUpdatedAt))
                .flatMap(s -> s)
                .filter(dt -> dt != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private String findLatestUpdatedBy(List<PenaltyConfig> l1, List<PenaltyConfig> l2) {
        return java.util.stream.Stream.concat(l1.stream(), l2.stream())
                .filter(c -> c.getUpdatedAt() != null)
                .max(java.util.Comparator.comparing(PenaltyConfig::getUpdatedAt))
                .map(PenaltyConfig::getUpdatedBy)
                .orElse(null);
    }
}
