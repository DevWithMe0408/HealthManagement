package org.example.nutritionservice.service.config;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.request.MealConfigUpdateRequest;
import org.example.nutritionservice.dto.response.MealConfigResponse;
import org.example.nutritionservice.dto.response.MealRatioItemResponse;
import org.example.nutritionservice.entity.config.MealRatioConfig;
import org.example.nutritionservice.entity.config.MealRatioConfigId;
import org.example.nutritionservice.repository.config.MealRatioConfigRepository;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MealConfigServiceImpl implements MealConfigService {

    private static final BigDecimal SUM_TOLERANCE = new BigDecimal("0.01");
    private static final String PLAN_3 = "3_BUA";
    private static final String PLAN_5 = "5_BUA";
    private static final Set<String> VALID_PLAN_TYPES = Set.of(PLAN_3, PLAN_5);

    private final MealRatioConfigRepository mealRatioConfigRepository;

    @Override
    public MealConfigResponse getAll() {
        List<MealRatioConfig> plan3 = mealRatioConfigRepository.findByPlanTypeOrderBySortOrderAsc(PLAN_3);
        List<MealRatioConfig> plan5 = mealRatioConfigRepository.findByPlanTypeOrderBySortOrderAsc(PLAN_5);

        LocalDateTime latestUpdated = findLatestUpdatedAt(plan3, plan5);
        String latestUpdatedBy = findLatestUpdatedBy(plan3, plan5);

        return MealConfigResponse.builder()
                .plan3Meals(toItemResponseList(plan3))
                .plan5Meals(toItemResponseList(plan5))
                .updatedAt(latestUpdated)
                .updatedBy(latestUpdatedBy)
                .build();
    }

    @Override
    @Transactional
    public List<MealRatioItemResponse> update(String planType, MealConfigUpdateRequest req, String updatedBy) {
        if (!VALID_PLAN_TYPES.contains(planType)) {
            throw new BusinessException(ErrorCode.CONFIG_VALIDATION_FAILED,
                    "planType khong hop le. Chi chap nhan: 3_BUA, 5_BUA");
        }

        int expectedSize = PLAN_3.equals(planType) ? 3 : 5;
        if (req.getMeals().size() != expectedSize) {
            throw new BusinessException(ErrorCode.CONFIG_VALIDATION_FAILED,
                    "Plan " + planType + " phai co dung " + expectedSize + " bua");
        }

        BigDecimal total = req.getMeals().stream()
                .map(MealConfigUpdateRequest.MealItem::getRatio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.subtract(BigDecimal.ONE).abs().compareTo(SUM_TOLERANCE) > 0) {
            throw new BusinessException(ErrorCode.CONFIG_SUM_INVALID,
                    "Tong ratio cac bua phai bang 1.00 (hien tai: " + total + ")");
        }

        List<MealRatioConfig> existing = mealRatioConfigRepository.findByPlanTypeOrderBySortOrderAsc(planType);

        for (int i = 0; i < req.getMeals().size(); i++) {
            MealConfigUpdateRequest.MealItem item = req.getMeals().get(i);
            MealRatioConfigId id = new MealRatioConfigId(planType, item.getMealCode());
            MealRatioConfig config = mealRatioConfigRepository.findById(id)
                    .orElse(new MealRatioConfig(planType, item.getMealCode(), item.getRatio(),
                            (short) (i + 1), null, null, null, null));
            config.setRatio(item.getRatio());
            config.setUpdatedBy(updatedBy);
            mealRatioConfigRepository.save(config);
        }

        return mealRatioConfigRepository.findByPlanTypeOrderBySortOrderAsc(planType)
                .stream()
                .map(this::toItemResponse)
                .toList();
    }

    private List<MealRatioItemResponse> toItemResponseList(List<MealRatioConfig> list) {
        return list.stream().map(this::toItemResponse).toList();
    }

    private MealRatioItemResponse toItemResponse(MealRatioConfig c) {
        return MealRatioItemResponse.builder()
                .mealCode(c.getMealCode())
                .ratio(c.getRatio())
                .sortOrder(c.getSortOrder())
                .build();
    }

    private LocalDateTime findLatestUpdatedAt(List<MealRatioConfig> plan3, List<MealRatioConfig> plan5) {
        return java.util.stream.Stream.concat(plan3.stream(), plan5.stream())
                .map(MealRatioConfig::getUpdatedAt)
                .filter(dt -> dt != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private String findLatestUpdatedBy(List<MealRatioConfig> plan3, List<MealRatioConfig> plan5) {
        return java.util.stream.Stream.concat(plan3.stream(), plan5.stream())
                .filter(c -> c.getUpdatedAt() != null)
                .max(java.util.Comparator.comparing(MealRatioConfig::getUpdatedAt))
                .map(MealRatioConfig::getUpdatedBy)
                .orElse(null);
    }
}
