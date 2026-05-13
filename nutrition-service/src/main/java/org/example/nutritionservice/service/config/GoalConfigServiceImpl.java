package org.example.nutritionservice.service.config;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.request.GoalConfigUpdateRequest;
import org.example.nutritionservice.dto.response.GoalConfigResponse;
import org.example.nutritionservice.entity.config.GoalConfig;
import org.example.nutritionservice.repository.config.GoalConfigRepository;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalConfigServiceImpl implements GoalConfigService {

    private static final BigDecimal SUM_TOLERANCE = new BigDecimal("0.01");

    private final GoalConfigRepository goalConfigRepository;

    @Override
    public List<GoalConfigResponse> getAll() {
        return goalConfigRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public GoalConfigResponse getByCode(String goalCode) {
        GoalConfig config = goalConfigRepository.findById(goalCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND));
        return toResponse(config);
    }

    @Override
    @Transactional
    public GoalConfigResponse update(String goalCode, GoalConfigUpdateRequest req, String updatedBy) {
        GoalConfig config = goalConfigRepository.findById(goalCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND));

        validateRatioSum(req.getProteinRatio(), req.getFatRatio(), req.getCarbRatio(), "macro");
        validateRatioSum(req.getSlotMainRatio(), req.getSlotVegRatio(), req.getSlotCarbRatio(), "slot");
        validateRatioSum(req.getWeightP(), req.getWeightF(), req.getWeightC(), req.getWeightKcal(), "weight");

        config.setCalMultiplier(req.getCalMultiplier());
        config.setProteinRatio(req.getProteinRatio());
        config.setFatRatio(req.getFatRatio());
        config.setCarbRatio(req.getCarbRatio());
        config.setSlotMainRatio(req.getSlotMainRatio());
        config.setSlotVegRatio(req.getSlotVegRatio());
        config.setSlotCarbRatio(req.getSlotCarbRatio());
        config.setWeightP(req.getWeightP());
        config.setWeightF(req.getWeightF());
        config.setWeightC(req.getWeightC());
        config.setWeightKcal(req.getWeightKcal());
        config.setUpdatedBy(updatedBy);

        return toResponse(goalConfigRepository.save(config));
    }

    private void validateRatioSum(BigDecimal a, BigDecimal b, BigDecimal c, String groupName) {
        BigDecimal sum = a.add(b).add(c);
        if (sum.subtract(BigDecimal.ONE).abs().compareTo(SUM_TOLERANCE) > 0) {
            throw new BusinessException(ErrorCode.CONFIG_SUM_INVALID,
                    "Tong " + groupName + " ratio phai bang 1.00 (hien tai: " + sum + ")");
        }
    }

    private void validateRatioSum(BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal d, String groupName) {
        BigDecimal sum = a.add(b).add(c).add(d);
        if (sum.subtract(BigDecimal.ONE).abs().compareTo(SUM_TOLERANCE) > 0) {
            throw new BusinessException(ErrorCode.CONFIG_SUM_INVALID,
                    "Tong " + groupName + " ratio phai bang 1.00 (hien tai: " + sum + ")");
        }
    }

    private GoalConfigResponse toResponse(GoalConfig c) {
        return GoalConfigResponse.builder()
                .goalCode(c.getGoalCode())
                .calMultiplier(c.getCalMultiplier())
                .proteinRatio(c.getProteinRatio())
                .fatRatio(c.getFatRatio())
                .carbRatio(c.getCarbRatio())
                .slotMainRatio(c.getSlotMainRatio())
                .slotVegRatio(c.getSlotVegRatio())
                .slotCarbRatio(c.getSlotCarbRatio())
                .weightP(c.getWeightP())
                .weightF(c.getWeightF())
                .weightC(c.getWeightC())
                .weightKcal(c.getWeightKcal())
                .description(c.getDescription())
                .updatedAt(c.getUpdatedAt())
                .updatedBy(c.getUpdatedBy())
                .build();
    }
}
