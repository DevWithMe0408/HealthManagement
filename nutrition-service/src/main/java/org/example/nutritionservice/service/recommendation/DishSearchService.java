package org.example.nutritionservice.service.recommendation;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.response.DishOptionResponse;
import org.example.nutritionservice.entity.catalog.Dish;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.entity.favorite.FavoriteDish;
import org.example.nutritionservice.repository.catalog.DishRepository;
import org.example.nutritionservice.repository.favorite.FavoriteDishRepository;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DishSearchService {

    private static final int SEARCH_LIMIT = 20;
    private static final int CALC_SCALE = 4;
    private static final int FINAL_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final DishRepository dishRepository;
    private final FavoriteDishRepository favoriteDishRepository;
    private final ConfigLoaderService configLoaderService;

    public List<DishOptionResponse> searchDishes(
            String userId,
            SlotCode slotCode,
            String query,
            BigDecimal slotKcalTarget) {
        validateSearchRequest(query, slotKcalTarget);
        String keyword = query.trim();
        List<BigDecimal> servingSteps = servingSteps(slotCode);
        Set<String> favoriteIds = favoriteIds(userId);

        return dishRepository.searchByName(slotCode, keyword, PageRequest.of(0, SEARCH_LIMIT)).stream()
                .map(dish -> toOption(dish, slotKcalTarget, servingSteps, favoriteIds))
                .toList();
    }

    private DishOptionResponse toOption(
            Dish dish,
            BigDecimal slotKcalTarget,
            List<BigDecimal> servingSteps,
            Set<String> favoriteIds) {
        BigDecimal baseKcal = baseKcal(dish);
        BigDecimal expectedServing = nearestServing(slotKcalTarget, baseKcal, servingSteps);
        BigDecimal expectedActualGrams = BigDecimal.valueOf(dish.getBaseServingG())
                .multiply(expectedServing)
                .setScale(FINAL_SCALE, RoundingMode.HALF_UP);

        return DishOptionResponse.builder()
                .dishId(dish.getId())
                .dishName(dish.getName())
                .slotCode(dish.getSlotCode())
                .foodGroupCode(dish.getFoodGroupCode())
                .expectedScore(null)
                .expectedServing(expectedServing)
                .expectedActualGrams(expectedActualGrams)
                .unit(dish.getUnit())
                .baseServingG(dish.getBaseServingG())
                .favorite(favoriteIds.contains(dish.getId()))
                .build();
    }

    private BigDecimal baseKcal(Dish dish) {
        BigDecimal baseServingRatio = BigDecimal.valueOf(dish.getBaseServingG())
                .divide(ONE_HUNDRED, CALC_SCALE, RoundingMode.HALF_UP);
        return dish.getKcalPer100g().multiply(baseServingRatio);
    }

    private BigDecimal nearestServing(
            BigDecimal slotKcalTarget,
            BigDecimal baseKcal,
            List<BigDecimal> servingSteps) {
        if (servingSteps.isEmpty()) {
            return BigDecimal.ONE;
        }
        if (baseKcal.signum() <= 0) {
            return servingSteps.get(0);
        }

        BigDecimal rawServing = slotKcalTarget.divide(baseKcal, CALC_SCALE, RoundingMode.HALF_UP);
        return servingSteps.stream()
                .min(Comparator.comparing(step -> step.subtract(rawServing).abs()))
                .orElse(servingSteps.get(0));
    }

    private List<BigDecimal> servingSteps(SlotCode slotCode) {
        return slotCode == SlotCode.COMBO
                ? configLoaderService.getDecimalArray("filter.combo_serving_steps")
                : configLoaderService.getDecimalArray("filter.serving_steps");
    }

    private Set<String> favoriteIds(String userId) {
        if (userId == null || userId.isBlank()) {
            return Set.of();
        }
        return favoriteDishRepository.findByUserId(userId).stream()
                .map(FavoriteDish::getDishId)
                .collect(Collectors.toSet());
    }

    private void validateSearchRequest(String query, BigDecimal slotKcalTarget) {
        if (query == null || query.trim().isEmpty()) {
            throw invalid("Tu khoa tim kiem khong duoc rong");
        }
        if (query.trim().length() > 50) {
            throw invalid("Tu khoa tim kiem toi da 50 ky tu");
        }
        if (slotKcalTarget == null || slotKcalTarget.signum() <= 0) {
            throw invalid("slotKcalTarget phai lon hon 0");
        }
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.CONFIG_VALIDATION_FAILED, message);
    }
}
