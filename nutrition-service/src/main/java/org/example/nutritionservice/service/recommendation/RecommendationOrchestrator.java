package org.example.nutritionservice.service.recommendation;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.domain.recommendation.DailyPlan;
import org.example.nutritionservice.domain.recommendation.DishWithServing;
import org.example.nutritionservice.domain.recommendation.HistoryEntry;
import org.example.nutritionservice.domain.recommendation.LoadedConfigs;
import org.example.nutritionservice.domain.recommendation.MacroTarget;
import org.example.nutritionservice.domain.recommendation.MealCombination;
import org.example.nutritionservice.domain.recommendation.MealTarget;
import org.example.nutritionservice.domain.recommendation.PerMealConfig;
import org.example.nutritionservice.domain.recommendation.RecommendedMeal;
import org.example.nutritionservice.domain.recommendation.UserContext;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.entity.favorite.FavoriteDish;
import org.example.nutritionservice.entity.meallog.MealLog;
import org.example.nutritionservice.entity.meallog.MealLogDish;
import org.example.nutritionservice.entity.meallog.MealType;
import org.example.nutritionservice.repository.catalog.DishRepository;
import org.example.nutritionservice.repository.favorite.FavoriteDishRepository;
import org.example.nutritionservice.repository.meallog.MealLogDishRepository;
import org.example.nutritionservice.repository.meallog.MealLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationOrchestrator {
    /**
     * orchestrate luồng full-day theo §4
     * Đề xuất dựa trên thời điểm sử dụng
     */

    private final ConfigLoaderService configLoaderService;
    private final MacroCalculator macroCalculator;
    private final DishFilterService dishFilterService;
    private final BruteForceEngine bruteForceEngine;
    private final DishRepository dishRepository;
    private final MealLogRepository mealLogRepository;
    private final MealLogDishRepository mealLogDishRepository;
    private final FavoriteDishRepository favoriteDishRepository;

    public DailyPlan recommendFullDay(UserContext userCtx) {
        LoadedConfigs configs = configLoaderService.loadForRecommendation(userCtx.getGoalCode(), userCtx.getPlanType());
        PlanWindow planWindow = determineRemainingMeals(userCtx, configs);
        List<HistoryEntry> effectiveHistory = new ArrayList<>(loadHistory(
                userCtx.getUserId(),
                planWindow.planDate(),
                configs.getInt("penalty.lookback_days")
        ));
        Set<String> favoriteIds = loadFavoriteIds(userCtx.getUserId());
        List<RecommendedMeal> recommendedMeals = new ArrayList<>();

        for (MealType mealType : planWindow.mealTypes()) {
            RecommendedMeal recommendedMeal = recommendForMeal(
                    userCtx,
                    mealType,
                    planWindow.planDate(),
                    effectiveHistory,
                    favoriteIds,
                    configs
            );
            recommendedMeals.add(recommendedMeal);
            recommendedMeal.getCombinations().stream()
                    .findFirst()
                    .ifPresent(combination -> effectiveHistory.addAll(toHistory(
                            planWindow.planDate(),
                            combination
                    )));
        }

        return DailyPlan.builder()
                .planDate(planWindow.planDate())
                .meals(recommendedMeals)
                .build();
    }

    public RecommendedMeal recommendSingleMeal(
            UserContext userCtx,
            MealType mealType,
            List<HistoryEntry> history) {
        LoadedConfigs configs = configLoaderService.loadForRecommendation(userCtx.getGoalCode(), userCtx.getPlanType());
        return recommendForMeal(
                userCtx,
                mealType,
                requestTime(userCtx).toLocalDate(),
                history,
                loadFavoriteIds(userCtx.getUserId()),
                configs
        );
    }

    private RecommendedMeal recommendForMeal(
            UserContext userCtx,
            MealType mealType,
            LocalDate targetDate,
            List<HistoryEntry> history,
            Set<String> favoriteIds,
            LoadedConfigs configs) {
        MealTarget mealTarget = buildMealTarget(userCtx, mealType, targetDate, configs);
        Map<SlotCode, List<org.example.nutritionservice.domain.recommendation.DishCandidate>> candidates =
                loadCandidates(mealTarget, configs);
        List<MealCombination> combinations = bruteForceEngine.findTopK(
                userCtx,
                mealTarget,
                candidates,
                history,
                favoriteIds,
                configs,
                configs.getInt("display.top_k")
        );
        return RecommendedMeal.builder()
                .mealTarget(mealTarget)
                .combinations(combinations)
                .build();
    }

    private MealTarget buildMealTarget(
            UserContext userCtx,
            MealType mealType,
            LocalDate targetDate,
            LoadedConfigs configs) {
        PerMealConfig perMealConfig = userCtx.getPerMealConfigs().get(mealType);
        if (perMealConfig == null) {
            throw new IllegalArgumentException("Thieu cau hinh mon cho bua " + mealType);
        }
        BigDecimal dailyKcal = macroCalculator.calculateDailyKcal(userCtx.getTdee(), configs.getGoalConfig());
        BigDecimal mealKcal = macroCalculator.calculateMealKcal(dailyKcal, mealType, configs.getMealRatios());
        MacroTarget macroTarget = macroCalculator.calculateMacroTarget(mealKcal, configs.getGoalConfig());
        Map<SlotCode, BigDecimal> slotTargets = macroCalculator.calculateSlotKcalTargets(
                mealKcal,
                configs.getGoalConfig(),
                perMealConfig
        );
        return MealTarget.builder()
                .mealDate(targetDate)
                .mealType(mealType)
                .mealKcal(mealKcal)
                .macroTarget(macroTarget)
                .slotKcalTargets(slotTargets)
                .perMealConfig(perMealConfig)
                .build();
    }

    private Map<SlotCode, List<org.example.nutritionservice.domain.recommendation.DishCandidate>> loadCandidates(
            MealTarget mealTarget,
            LoadedConfigs configs) {
        Map<SlotCode, List<org.example.nutritionservice.domain.recommendation.DishCandidate>> candidates =
                new LinkedHashMap<>();
        for (Map.Entry<SlotCode, BigDecimal> slotTarget : mealTarget.getSlotKcalTargets().entrySet()) {
            int slotCount = mealTarget.getPerMealConfig().countForSlot(slotTarget.getKey());
            BigDecimal targetPerDish = slotTarget.getValue()
                    .divide(BigDecimal.valueOf(slotCount), 2, RoundingMode.HALF_UP);
            candidates.put(slotTarget.getKey(), dishFilterService.filterCandidatesForSlot(
                    slotTarget.getKey(),
                    targetPerDish,
                    configs,
                    dishRepository.findBySlotCodeAndIsActiveTrue(slotTarget.getKey())
            ));
        }
        return candidates;
    }

    private List<HistoryEntry> loadHistory(String userId, LocalDate targetDate, int lookbackDays) {
        LocalDate from = targetDate.minusDays(Math.max(lookbackDays - 1L, 0L));
        List<MealLog> mealLogs = mealLogRepository
                .findByUserIdAndMealDateBetweenOrderByMealDateDescMealTypeAsc(userId, from, targetDate);
        if (mealLogs.isEmpty()) {
            return List.of();
        }

        Map<String, LocalDate> mealDates = mealLogs.stream()
                .collect(Collectors.toMap(MealLog::getId, MealLog::getMealDate));
        return mealLogDishRepository.findByMealLogIdIn(mealDates.keySet()).stream()
                .map(dish -> toHistoryEntry(dish, mealDates.get(dish.getMealLogId())))
                .toList();
    }

    private HistoryEntry toHistoryEntry(MealLogDish dish, LocalDate mealDate) {
        return HistoryEntry.builder()
                .mealDate(mealDate)
                .dishId(dish.getDishId())
                .foodGroupCode(dish.getFoodGroupCode())
                .slotCode(dish.getSlotCode())
                .build();
    }

    private List<HistoryEntry> toHistory(LocalDate mealDate, MealCombination combination) {
        return combination.getDishes().stream()
                .map(DishWithServing::getCandidate)
                .map(candidate -> HistoryEntry.builder()
                        .mealDate(mealDate)
                        .dishId(candidate.getDishId())
                        .foodGroupCode(candidate.getFoodGroupCode())
                        .slotCode(candidate.getSlotCode())
                        .build())
                .toList();
    }

    private Set<String> loadFavoriteIds(String userId) {
        return favoriteDishRepository.findByUserId(userId).stream()
                .map(FavoriteDish::getDishId)
                .collect(Collectors.toSet());
    }

    private PlanWindow determineRemainingMeals(UserContext userCtx, LoadedConfigs configs) {
        LocalDateTime requestTime = requestTime(userCtx);
        if (requestTime.getHour() >= 21 || requestTime.getHour() < 5) {
            return new PlanWindow(requestTime.toLocalDate().plusDays(1), orderedMealTypes(configs));
        }

        LocalDate today = requestTime.toLocalDate();
        Set<MealType> loggedMeals = mealLogRepository.findByUserIdAndMealDate(userCtx.getUserId(), today)
                .stream()
                .map(MealLog::getMealType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(MealType.class)));
        Set<MealType> eligibleMeals = eligibleMealsFromHour(requestTime.getHour());
        List<MealType> remaining = orderedMealTypes(configs).stream()
                .filter(eligibleMeals::contains)
                .filter(mealType -> !loggedMeals.contains(mealType))
                .toList();
        return new PlanWindow(today, remaining);
    }

    private Set<MealType> eligibleMealsFromHour(int hour) {
        if (hour < 11) {
            return EnumSet.allOf(MealType.class);
        }
        if (hour < 14) {
            return EnumSet.of(MealType.TRUA, MealType.PHU_CHIEU, MealType.TOI);
        }
        return EnumSet.of(MealType.TOI);
    }

    private List<MealType> orderedMealTypes(LoadedConfigs configs) {
        return configs.getMealRatios().stream()
                .sorted(Comparator.comparing(item -> item.getSortOrder()))
                .map(item -> MealType.valueOf(item.getMealCode()))
                .toList();
    }

    private LocalDateTime requestTime(UserContext userCtx) {
        return userCtx.getRequestTime() == null ? LocalDateTime.now() : userCtx.getRequestTime();
    }

    private record PlanWindow(LocalDate planDate, List<MealType> mealTypes) {
    }
}
