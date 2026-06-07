package org.example.nutritionservice.service.recommendation;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.domain.recommendation.DailyPlan;
import org.example.nutritionservice.domain.recommendation.DishCandidate;
import org.example.nutritionservice.domain.recommendation.DishWithServing;
import org.example.nutritionservice.domain.recommendation.HistoryEntry;
import org.example.nutritionservice.domain.recommendation.LoadedConfigs;
import org.example.nutritionservice.domain.recommendation.MacroTarget;
import org.example.nutritionservice.domain.recommendation.MealKind;
import org.example.nutritionservice.domain.recommendation.MealActual;
import org.example.nutritionservice.domain.recommendation.MealCombination;
import org.example.nutritionservice.domain.recommendation.MealTarget;
import org.example.nutritionservice.domain.recommendation.PerMealConfig;
import org.example.nutritionservice.domain.recommendation.RecommendedMeal;
import org.example.nutritionservice.domain.recommendation.SlotAlternative;
import org.example.nutritionservice.domain.recommendation.UserContext;
import org.example.nutritionservice.dto.request.PinnedDish;
import org.example.nutritionservice.dto.request.RecommendFullDayRequest;
import org.example.nutritionservice.dto.request.SwapDishRequest;
import org.example.nutritionservice.dto.response.DailyPlanResponse;
import org.example.nutritionservice.dto.response.DishOptionResponse;
import org.example.nutritionservice.dto.response.DishSuggestionResponse;
import org.example.nutritionservice.dto.response.MealCombinationResponse;
import org.example.nutritionservice.dto.response.MealSuggestionResponse;
import org.example.nutritionservice.dto.response.SwapResultResponse;
import org.example.nutritionservice.dto.response.WarningResponse;
import org.example.nutritionservice.entity.catalog.Dish;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.entity.favorite.FavoriteDish;
import org.example.nutritionservice.entity.meallog.MealLog;
import org.example.nutritionservice.entity.meallog.MealLogDish;
import org.example.nutritionservice.entity.meallog.MealType;
import org.example.nutritionservice.repository.catalog.DishRepository;
import org.example.nutritionservice.repository.favorite.FavoriteDishRepository;
import org.example.nutritionservice.repository.meallog.MealLogDishRepository;
import org.example.nutritionservice.repository.meallog.MealLogRepository;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationApiService {

    private static final int CALC_SCALE = 4;
    private static final int FINAL_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal KCAL_PER_G_PROTEIN = new BigDecimal("4");
    private static final BigDecimal KCAL_PER_G_FAT = new BigDecimal("9");
    private static final BigDecimal KCAL_PER_G_CARB = new BigDecimal("4");

    private final RecommendationOrchestrator recommendationOrchestrator;
    private final ConfigLoaderService configLoaderService;
    private final MacroCalculator macroCalculator;
    private final DishFilterService dishFilterService;
    private final BruteForceEngine bruteForceEngine;
    private final ScoringService scoringService;
    private final PenaltyService penaltyService;
    private final FavoriteDishRepository favoriteDishRepository;
    private final DishRepository dishRepository;
    private final MealLogRepository mealLogRepository;
    private final MealLogDishRepository mealLogDishRepository;

    public DailyPlanResponse recommendFullDay(String userId, RecommendFullDayRequest request) {
        validateFullDayRequest(request);
        DailyPlanResponse.WarningResponse warning = warningFor(request.getConstitution(), request.getGoalCode());
        if (warning != null && warning.isRequireConfirm() && !request.isConstitutionConfirmed()) {
            return DailyPlanResponse.builder()
                    .planDate(LocalDate.now())
                    .goalCode(request.getGoalCode())
                    .planType(request.getPlanType())
                    .warning(warning)
                    .meals(List.of())
                    .build();
        }

        UserContext userContext = UserContext.builder()
                .userId(userId)
                .tdee(request.getTdee())
                .goalCode(request.getGoalCode())
                .planType(request.getPlanType())
                .perMealConfigs(toPerMealConfigs(request))
                .requestTime(LocalDateTime.now())
                .forceCompute(request.isForceCompute())
                .build();
        DailyPlan dailyPlan = recommendationOrchestrator.recommendFullDay(userContext);
        return toDailyPlanResponse(
                dailyPlan,
                request.getGoalCode(),
                request.getPlanType(),
                warning,
                favoriteIds(userId)
        );
    }

    public SwapResultResponse swapDish(String userId, SwapDishRequest request) {
        MealSuggestionResponse currentMeal = findMeal(request.getCurrentPlan(), request.getMealType());
        if (currentMeal.getTopCombination() == null) {
            throw invalid("Bua can doi mon khong co to hop top");
        }
        if (request.getCurrentPlan().getGoalCode() == null || request.getCurrentPlan().getPlanType() == null) {
            throw invalid("currentPlan thieu goalCode hoac planType de tinh lai score");
        }

        LoadedConfigs configs = configLoaderService.loadForRecommendation(
                request.getCurrentPlan().getGoalCode(),
                request.getCurrentPlan().getPlanType()
        );
        Set<String> favorites = favoriteIds(userId);

        List<DishSuggestionResponse> currentDishes = currentMeal.getTopCombination().getDishes();
        findSwappedIndex(currentDishes, request.getSwappedSlot());
        Map<String, String> pinnedMap = buildPinnedMap(currentDishes, request);
        Set<String> explicitPinnedSlots = explicitPinnedSlots(request);

        List<DishCandidate> pinnedCandidates = new ArrayList<>();
        for (int index = 0; index < currentDishes.size(); index++) {
            DishSuggestionResponse currentDish = currentDishes.get(index);
            String slotKey = slotKeyOf(currentDishes, index);
            String pinnedDishId = pinnedMap.get(slotKey);
            Dish pinnedDish = dishRepository.findById(pinnedDishId)
                    .orElseThrow(() -> invalid("Mon trong slot " + slotKey + " khong ton tai"));
            if (pinnedDish.getSlotCode() != currentDish.getSlotCode()) {
                throw invalid("Mon pin tai " + slotKey + " sai slot code");
            }
            pinnedCandidates.add(dishFilterService.toCandidate(pinnedDish));
        }
        Map<Integer, BigDecimal> fixedServingByIndex = buildFixedServingByIndex(currentDishes, request);

        PerMealConfig perMealConfig = buildPerMealConfig(currentDishes);
        MacroTarget macroTarget = macroCalculator.calculateMacroTarget(
                currentMeal.getMealKcalTarget(),
                configs.getGoalConfig()
        );
        MealTarget mealTarget = MealTarget.builder()
                .mealDate(request.getCurrentPlan().getPlanDate())
                .mealType(currentMeal.getMealType())
                .mealKcal(currentMeal.getMealKcalTarget())
                .macroTarget(macroTarget)
                .slotKcalTargets(macroCalculator.calculateSlotKcalTargets(
                        currentMeal.getMealKcalTarget(),
                        configs.getGoalConfig(),
                        perMealConfig
                ))
                .perMealConfig(perMealConfig)
                .build();

        List<HistoryEntry> history = loadHistory(userId, request.getCurrentPlan().getPlanDate(), configs);
        history.addAll(planHistoryWithoutMeal(request.getCurrentPlan(), request.getMealType()));
        BigDecimal penalty = penaltyService.computePenalty(
                pinnedCandidates,
                history,
                favorites,
                configs,
                mealTarget.getMealDate()
        );
        MealCombination bestCombo = bruteForceEngine.findBestServingCombo(
                pinnedCandidates,
                fixedServingByIndex,
                mealTarget,
                configs,
                penalty
        );
        if (bestCombo == null) {
            throw invalid("Khong tim duoc serving thoa man sau khi doi mon");
        }
        List<WarningResponse> warnings = buildWarnings(bestCombo, configs);

        Map<String, List<SlotAlternative>> slotAlternatives = bruteForceEngine.computeSlotAlternatives(
                bestCombo,
                loadCandidatesPerSlot(mealTarget, configs),
                mealTarget,
                configs
        );
        MealCombinationResponse updatedCombination = toCombinationResponse(bestCombo, favorites);
        BigDecimal originalFinalScore = currentMeal.getTopCombination().getFinalScore();
        boolean triggered = updatedCombination.getFinalScore()
                .compareTo(BigDecimal.valueOf(configs.getInt("reopt.score_threshold"))) < 0
                || originalFinalScore.subtract(updatedCombination.getFinalScore())
                .compareTo(BigDecimal.valueOf(configs.getInt("reopt.score_drop"))) > 0;

        MealSuggestionResponse updatedMeal = MealSuggestionResponse.builder()
                .mealType(currentMeal.getMealType())
                .mealKcalTarget(currentMeal.getMealKcalTarget())
                .kcalTarget(macroTarget.getKcal())
                .proteinTarget(macroTarget.getProteinG())
                .fatTarget(macroTarget.getFatG())
                .carbTarget(macroTarget.getCarbG())
                .topCombination(updatedCombination)
                .slotAlternatives(slotAlternatives.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().stream()
                                        .map(alternative -> toDishOptionResponse(alternative, favorites))
                                        .toList(),
                                (left, right) -> left,
                                LinkedHashMap::new
                        )))
                .build();
        return SwapResultResponse.builder()
                .updatedMeal(updatedMeal)
                .newFinalScore(updatedCombination.getFinalScore())
                .originalFinalScore(originalFinalScore)
                .scoreDropTriggered(triggered)
                .suggestion(triggered ? findBestSwapSuggestion(
                        slotAlternatives,
                        explicitPinnedSlots,
                        updatedCombination.getFinalScore()
                ) : null)
                .warnings(warnings)
                .build();
    }

    public DailyPlanResponse toDailyPlanResponse(
            DailyPlan dailyPlan,
            String goalCode,
            String planType,
            DailyPlanResponse.WarningResponse warning,
            Set<String> favorites) {
        LoadedConfigs configs = configLoaderService.loadForRecommendation(goalCode, planType);
        return DailyPlanResponse.builder()
                .planDate(dailyPlan.getPlanDate())
                .goalCode(goalCode)
                .planType(planType)
                .warning(warning)
                .meals(dailyPlan.getMeals().stream()
                        .map(meal -> toMealResponse(meal, configs, favorites))
                        .toList())
                .build();
    }

    private MealSuggestionResponse toMealResponse(
            RecommendedMeal meal,
            LoadedConfigs configs,
            Set<String> favorites) {
        MealTarget mealTarget = meal.getMealTarget();
        MacroTarget macroTarget = mealTarget.getMacroTarget();

        if (meal.getCombinations().isEmpty()) {
            return MealSuggestionResponse.builder()
                    .mealType(mealTarget.getMealType())
                    .mealKcalTarget(mealTarget.getMealKcal())
                    .kcalTarget(macroTarget.getKcal())
                    .proteinTarget(macroTarget.getProteinG())
                    .fatTarget(macroTarget.getFatG())
                    .carbTarget(macroTarget.getCarbG())
                    .topCombination(null)
                    .slotAlternatives(Map.of())
                    .build();
        }

        MealCombination topCombination = meal.getCombinations().get(0);
        Map<String, List<DishOptionResponse>> slotAlternatives = bruteForceEngine.computeSlotAlternatives(
                        topCombination,
                        meal.getCandidatesPerSlot(),
                        meal.getMealTarget(),
                        configs
                )
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(alternative -> toDishOptionResponse(alternative, favorites))
                                .toList(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return MealSuggestionResponse.builder()
                .mealType(mealTarget.getMealType())
                .mealKcalTarget(mealTarget.getMealKcal())
                .kcalTarget(macroTarget.getKcal())
                .proteinTarget(macroTarget.getProteinG())
                .fatTarget(macroTarget.getFatG())
                .carbTarget(macroTarget.getCarbG())
                .topCombination(toCombinationResponse(topCombination, favorites))
                .slotAlternatives(slotAlternatives)
                .build();
    }

    private MealCombinationResponse toCombinationResponse(MealCombination combination, Set<String> favorites) {
        Map<SlotCode, Integer> slotCounter = new EnumMap<>(SlotCode.class);
        List<DishSuggestionResponse> dishes = new ArrayList<>();
        for (DishWithServing dish : combination.getDishes()) {
            SlotCode slotCode = dish.getCandidate().getSlotCode();
            int slotIndex = slotCounter.getOrDefault(slotCode, 0);
            slotCounter.put(slotCode, slotIndex + 1);
            dishes.add(toDishResponse(dish, slotCode.name() + "_" + slotIndex, favorites));
        }
        return MealCombinationResponse.builder()
                .totalKcal(combination.getActual().getKcal())
                .totalProtein(combination.getActual().getProteinG())
                .totalFat(combination.getActual().getFatG())
                .totalCarb(combination.getActual().getCarbG())
                .macroScore(combination.getMacroScore())
                .penalty(combination.getPenalty())
                .finalScore(combination.getFinalScore())
                .dishes(dishes)
                .build();
    }

    private DishSuggestionResponse toDishResponse(DishWithServing dish, Set<String> favorites) {
        return toDishResponse(dish, null, favorites);
    }

    private DishSuggestionResponse toDishResponse(DishWithServing dish, String slotKey, Set<String> favorites) {
        return DishSuggestionResponse.builder()
                .slotKey(slotKey)
                .dishId(dish.getCandidate().getDishId())
                .dishName(dish.getCandidate().getDishName())
                .slotCode(dish.getCandidate().getSlotCode())
                .foodGroupCode(dish.getCandidate().getFoodGroupCode())
                .servingMultiplier(dish.getServingMultiplier())
                .actualGrams(dish.getActualGrams())
                .dishKcal(dish.getKcal())
                .unit(dish.getCandidate().getDish().getUnit())
                .baseServingG(dish.getCandidate().getDish().getBaseServingG())
                .favorite(favorites.contains(dish.getCandidate().getDishId()))
                .build();
    }

    private DishOptionResponse toDishOptionResponse(SlotAlternative alternative, Set<String> favorites) {
        return DishOptionResponse.builder()
                .dishId(alternative.getDishId())
                .dishName(alternative.getCandidate().getDishName())
                .slotCode(alternative.getCandidate().getSlotCode())
                .foodGroupCode(alternative.getFoodGroupCode())
                .expectedScore(alternative.getExpectedScore())
                .expectedServing(alternative.getExpectedServing())
                .expectedActualGrams(alternative.getExpectedActualGrams())
                .unit(alternative.getCandidate().getDish().getUnit())
                .baseServingG(alternative.getCandidate().getDish().getBaseServingG())
                .favorite(favorites.contains(alternative.getDishId()))
                .build();
    }

    private Map<String, String> buildPinnedMap(List<DishSuggestionResponse> currentDishes, SwapDishRequest request) {
        Map<String, String> pinnedMap = new LinkedHashMap<>();
        for (int index = 0; index < currentDishes.size(); index++) {
            pinnedMap.put(slotKeyOf(currentDishes, index), currentDishes.get(index).getDishId());
        }
        if (request.getPinnedDishes() != null) {
            for (PinnedDish pinnedDish : request.getPinnedDishes()) {
                if (!pinnedMap.containsKey(pinnedDish.getSlotKey())) {
                    throw invalid("Slot pin khong ton tai: " + pinnedDish.getSlotKey());
                }
                pinnedMap.put(pinnedDish.getSlotKey(), pinnedDish.getDishId());
            }
        }
        if (!pinnedMap.containsKey(request.getSwappedSlot())) {
            throw invalid("Khong tim thay slot " + request.getSwappedSlot());
        }
        pinnedMap.put(request.getSwappedSlot(), request.getNewDishId());
        return pinnedMap;
    }

    private Map<Integer, BigDecimal> buildFixedServingByIndex(
            List<DishSuggestionResponse> currentDishes,
            SwapDishRequest request) {
        Map<Integer, BigDecimal> fixedServingByIndex = new HashMap<>();
        if (request.getPinnedDishes() == null) {
            return fixedServingByIndex;
        }

        for (PinnedDish pinnedDish : request.getPinnedDishes()) {
            if (pinnedDish.getOverrideGrams() == null) {
                continue;
            }
            boolean found = false;
            for (int index = 0; index < currentDishes.size(); index++) {
                if (slotKeyOf(currentDishes, index).equals(pinnedDish.getSlotKey())) {
                    fixedServingByIndex.put(index, pinnedDish.getOverrideGrams());
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw invalid("Slot pin khong ton tai: " + pinnedDish.getSlotKey());
            }
        }
        return fixedServingByIndex;
    }

    private Set<String> explicitPinnedSlots(SwapDishRequest request) {
        Set<String> pinnedSlots = new java.util.LinkedHashSet<>();
        if (request.getPinnedDishes() != null) {
            request.getPinnedDishes().stream()
                    .map(PinnedDish::getSlotKey)
                    .forEach(pinnedSlots::add);
        }
        pinnedSlots.add(request.getSwappedSlot());
        return pinnedSlots;
    }

    private PerMealConfig buildPerMealConfig(List<DishSuggestionResponse> dishes) {
        Map<SlotCode, Integer> slotCounts = new EnumMap<>(SlotCode.class);
        dishes.forEach(dish -> slotCounts.merge(dish.getSlotCode(), 1, Integer::sum));
        if (slotCounts.getOrDefault(SlotCode.COMBO, 0) > 0) {
            return PerMealConfig.builder()
                    .mealKind(MealKind.COMBO)
                    .build();
        }
        return PerMealConfig.builder()
                .mealKind(MealKind.NHIEU_MON)
                .nMain(slotCounts.getOrDefault(SlotCode.CHINH, 0))
                .nRau(slotCounts.getOrDefault(SlotCode.RAU, 0))
                .nCarb(slotCounts.getOrDefault(SlotCode.TINH_BOT, 0))
                .build();
    }

    private Map<SlotCode, List<DishCandidate>> loadCandidatesPerSlot(MealTarget mealTarget, LoadedConfigs configs) {
        Map<SlotCode, List<DishCandidate>> candidates = new LinkedHashMap<>();
        for (Map.Entry<SlotCode, BigDecimal> slotTarget : mealTarget.getSlotKcalTargets().entrySet()) {
            int slotCount = mealTarget.getPerMealConfig().countForSlot(slotTarget.getKey());
            if (slotCount <= 0) {
                continue;
            }
            candidates.put(slotTarget.getKey(), dishFilterService.filterCandidatesForSlot(
                    slotTarget.getKey(),
                    slotTarget.getValue(),
                    slotCount,
                    configs,
                    dishRepository.findBySlotCodeAndIsActiveTrue(slotTarget.getKey())
            ));
        }
        return candidates;
    }

    private List<WarningResponse> buildWarnings(MealCombination bestCombo, LoadedConfigs configs) {
        BigDecimal totalKcal = bestCombo.getActual().getKcal();
        if (totalKcal.signum() <= 0) {
            return List.of();
        }

        BigDecimal carbRatioThreshold = configs.getDecimal("warn.carb_ratio_threshold");
        BigDecimal carbKcal = bestCombo.getActual().getCarbG().multiply(KCAL_PER_G_CARB);
        BigDecimal carbRatio = carbKcal.divide(totalKcal, CALC_SCALE, RoundingMode.HALF_UP);
        if (carbRatio.compareTo(carbRatioThreshold) <= 0) {
            return List.of();
        }

        int carbPercent = carbRatio.multiply(ONE_HUNDRED)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        return List.of(WarningResponse.builder()
                .type("CARB_BOMB")
                .message("Bữa này khá nặng tinh bột (" + carbPercent
                        + "% kcal từ carb). Cân nhắc giảm khẩu phần tinh bột hoặc đổi sang lựa chọn cân bằng hơn.")
                .build());
    }

    private SwapResultResponse.SwapSuggestion findBestSwapSuggestion(
            Map<String, List<SlotAlternative>> slotAlternatives,
            Set<String> explicitPinnedSlots,
            BigDecimal currentScore) {
        SlotAlternative bestAlternative = null;
        String bestSlotKey = null;
        for (Map.Entry<String, List<SlotAlternative>> entry : slotAlternatives.entrySet()) {
            if (explicitPinnedSlots.contains(entry.getKey()) || entry.getValue().isEmpty()) {
                continue;
            }
            SlotAlternative alternative = entry.getValue().get(0);
            if (bestAlternative == null
                    || alternative.getExpectedScore().compareTo(bestAlternative.getExpectedScore()) > 0) {
                bestAlternative = alternative;
                bestSlotKey = entry.getKey();
            }
        }
        if (bestAlternative == null || bestAlternative.getExpectedScore().compareTo(currentScore) <= 0) {
            return null;
        }
        return SwapResultResponse.SwapSuggestion.builder()
                .message("Đổi món ở slot " + bestSlotKey + " sang "
                        + bestAlternative.getCandidate().getDishName() + " để tăng điểm lên "
                        + bestAlternative.getExpectedScore().setScale(1, RoundingMode.HALF_UP) + ".")
                .targetSlotKey(bestSlotKey)
                .suggestedDishId(bestAlternative.getDishId())
                .suggestedScore(bestAlternative.getExpectedScore())
                .build();
    }

    private String slotKeyOf(List<DishSuggestionResponse> dishes, int index) {
        String slotKey = dishes.get(index).getSlotKey();
        return slotKey == null || slotKey.isBlank() ? slotId(dishes, index) : slotKey;
    }

    private DishSuggestionResponse toDishResponse(
            DishCandidate candidate,
            BigDecimal servingMultiplier,
            Set<String> favorites) {
        return DishSuggestionResponse.builder()
                .dishId(candidate.getDishId())
                .dishName(candidate.getDishName())
                .slotCode(candidate.getSlotCode())
                .foodGroupCode(candidate.getFoodGroupCode())
                .servingMultiplier(servingMultiplier)
                .actualGrams(candidate.getBaseServingG().multiply(servingMultiplier).setScale(FINAL_SCALE, RoundingMode.HALF_UP))
                .dishKcal(candidate.getBaseKcal().multiply(servingMultiplier).setScale(FINAL_SCALE, RoundingMode.HALF_UP))
                .unit(candidate.getDish().getUnit())
                .baseServingG(candidate.getDish().getBaseServingG())
                .favorite(favorites.contains(candidate.getDishId()))
                .build();
    }

    private MealCombinationResponse scoreCombination(
            List<DishSuggestionResponse> dishes,
            MacroTarget macroTarget,
            LoadedConfigs configs,
            List<HistoryEntry> history,
            LocalDate targetDate,
            Set<String> favorites) {
        Map<String, Dish> dishMap = loadDishMap(dishes);
        return scoreCombination(dishes, macroTarget, configs, history, targetDate, favorites, dishMap);
    }

    private MealCombinationResponse scoreCombination(
            List<DishSuggestionResponse> dishes,
            MacroTarget macroTarget,
            LoadedConfigs configs,
            List<HistoryEntry> history,
            LocalDate targetDate,
            Set<String> favorites,
            Map<String, Dish> dishMap) {
        List<DishCandidate> candidates = dishes.stream()
                .map(dish -> dishFilterService.toCandidate(dishMap.get(dish.getDishId())))
                .toList();
        MealActual actual = actualFromDishResponses(dishes, dishMap);
        BigDecimal macroScore = scoringService.computeMacroScore(actual, macroTarget, configs.getGoalConfig(), configs);
        BigDecimal penalty = penaltyService.computePenalty(candidates, history, favorites, configs, targetDate);
        return MealCombinationResponse.builder()
                .totalKcal(actual.getKcal())
                .totalProtein(actual.getProteinG())
                .totalFat(actual.getFatG())
                .totalCarb(actual.getCarbG())
                .macroScore(macroScore)
                .penalty(penalty)
                .finalScore(macroScore.subtract(penalty).max(BigDecimal.ZERO).setScale(FINAL_SCALE, RoundingMode.HALF_UP))
                .dishes(dishes)
                .build();
    }

    private MealActual actualFromDishResponses(List<DishSuggestionResponse> dishes, Map<String, Dish> dishMap) {
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal carb = BigDecimal.ZERO;
        for (DishSuggestionResponse dishResponse : dishes) {
            Dish dish = dishMap.get(dishResponse.getDishId());
            BigDecimal servingRatio = dishResponse.getActualGrams()
                    .divide(ONE_HUNDRED, CALC_SCALE, RoundingMode.HALF_UP);
            protein = protein.add(dish.getProteinPer100g().multiply(servingRatio));
            fat = fat.add(dish.getFatPer100g().multiply(servingRatio));
            carb = carb.add(dish.getCarbPer100g().multiply(servingRatio));
        }
        BigDecimal kcal = protein.multiply(KCAL_PER_G_PROTEIN)
                .add(fat.multiply(KCAL_PER_G_FAT))
                .add(carb.multiply(KCAL_PER_G_CARB));
        return MealActual.builder()
                .proteinG(protein.setScale(FINAL_SCALE, RoundingMode.HALF_UP))
                .fatG(fat.setScale(FINAL_SCALE, RoundingMode.HALF_UP))
                .carbG(carb.setScale(FINAL_SCALE, RoundingMode.HALF_UP))
                .kcal(kcal.setScale(FINAL_SCALE, RoundingMode.HALF_UP))
                .build();
    }

    private SwapResultResponse.SwapSuggestion findServingSuggestion(
            List<DishSuggestionResponse> updatedDishes,
            int swappedIndex,
            MealCombinationResponse currentScore,
            MealSuggestionResponse currentMeal,
            MacroTarget macroTarget,
            LoadedConfigs configs,
            List<HistoryEntry> history,
            LocalDate targetDate,
            Set<String> favorites) {
        Map<String, Dish> dishMap = loadDishMap(updatedDishes);
        ServingGridResult best = searchServingGrid(
                0,
                swappedIndex,
                updatedDishes,
                copyDishes(updatedDishes),
                macroTarget,
                configs,
                history,
                targetDate,
                favorites,
                dishMap
        );
        if (best == null || best.score().getFinalScore().compareTo(currentScore.getFinalScore()) <= 0) {
            return null;
        }
        int changedIndex = firstServingChange(updatedDishes, best.dishes(), swappedIndex);
        if (changedIndex < 0) {
            return null;
        }
        DishSuggestionResponse changedDish = best.dishes().get(changedIndex);
        return SwapResultResponse.SwapSuggestion.builder()
                .message("Dieu chinh [" + changedDish.getDishName() + "] sang serving "
                        + changedDish.getServingMultiplier() + " se tang score len " + best.score().getFinalScore())
                .targetSlotKey(slotId(updatedDishes, changedIndex))
                .suggestedDishId(changedDish.getDishId())
                .suggestedScore(best.score().getFinalScore())
                .build();
    }

    private ServingGridResult searchServingGrid(
            int dishIndex,
            int swappedIndex,
            List<DishSuggestionResponse> original,
            List<DishSuggestionResponse> current,
            MacroTarget macroTarget,
            LoadedConfigs configs,
            List<HistoryEntry> history,
            LocalDate targetDate,
            Set<String> favorites,
            Map<String, Dish> dishMap) {
        if (dishIndex == current.size()) {
            return new ServingGridResult(
                    copyDishes(current),
                    scoreCombination(current, macroTarget, configs, history, targetDate, favorites, dishMap)
            );
        }
        if (dishIndex == swappedIndex) {
            return searchServingGrid(
                    dishIndex + 1,
                    swappedIndex,
                    original,
                    current,
                    macroTarget,
                    configs,
                    history,
                    targetDate,
                    favorites,
                    dishMap
            );
        }

        ServingGridResult best = null;
        DishSuggestionResponse originalDish = original.get(dishIndex);
        DishCandidate candidate = dishFilterService.toCandidate(dishMap.get(originalDish.getDishId()));
        for (BigDecimal serving : servingSteps(originalDish.getSlotCode(), configs)) {
            DishSuggestionResponse adjustedDish = toDishResponse(candidate, serving, favorites);
            if (violatesWeightConstraint(adjustedDish, configs)) {
                continue;
            }
            current.set(dishIndex, adjustedDish);
            ServingGridResult result = searchServingGrid(
                    dishIndex + 1,
                    swappedIndex,
                    original,
                    current,
                    macroTarget,
                    configs,
                    history,
                    targetDate,
                    favorites,
                    dishMap
            );
            if (result != null && (best == null
                    || result.score().getFinalScore().compareTo(best.score().getFinalScore()) > 0)) {
                best = result;
            }
        }
        current.set(dishIndex, copyDish(originalDish));
        return best;
    }

    private int firstServingChange(
            List<DishSuggestionResponse> original,
            List<DishSuggestionResponse> suggested,
            int swappedIndex) {
        for (int index = 0; index < original.size(); index++) {
            if (index != swappedIndex
                    && original.get(index).getServingMultiplier()
                    .compareTo(suggested.get(index).getServingMultiplier()) != 0) {
                return index;
            }
        }
        return -1;
    }

    private List<BigDecimal> servingSteps(SlotCode slotCode, LoadedConfigs configs) {
        return slotCode == SlotCode.COMBO
                ? configs.getDecimalArray("filter.combo_serving_steps")
                : configs.getDecimalArray("filter.serving_steps");
    }

    private boolean violatesWeightConstraint(DishSuggestionResponse dish, LoadedConfigs configs) {
        org.example.nutritionservice.entity.config.SlotConfig slotConfig = configs.getSlotConfigs()
                .get(dish.getSlotCode());
        return slotConfig != null
                && (dish.getActualGrams().compareTo(BigDecimal.valueOf(slotConfig.getMinG())) < 0
                || dish.getActualGrams().compareTo(BigDecimal.valueOf(slotConfig.getMaxG())) > 0);
    }

    private Map<MealType, PerMealConfig> toPerMealConfigs(RecommendFullDayRequest request) {
        Map<MealType, PerMealConfig> configs = new EnumMap<>(MealType.class);
        request.getPerMealConfig().forEach((mealType, item) -> configs.put(mealType, PerMealConfig.builder()
                .mealKind(item.getMealKind())
                .nMain(item.getNMain())
                .nRau(item.getNRau())
                .nCarb(item.getNCarb())
                .build()));
        return configs;
    }

    private void validateFullDayRequest(RecommendFullDayRequest request) {
        Set<MealType> expectedMeals = "3_BUA".equals(request.getPlanType())
                ? EnumSet.of(MealType.SANG, MealType.TRUA, MealType.TOI)
                : EnumSet.allOf(MealType.class);
        if (!request.getPerMealConfig().keySet().equals(expectedMeals)) {
            throw invalid("perMealConfig khong khop planType " + request.getPlanType());
        }
        request.getPerMealConfig().forEach((mealType, config) -> {
            if (config.getMealKind() == null) {
                throw invalid("mealKind la bat buoc cho " + mealType);
            }
            if (config.getMealKind() == org.example.nutritionservice.domain.recommendation.MealKind.NHIEU_MON) {
                if (!between(config.getNMain(), 1, 3)
                        || !between(config.getNRau(), 0, 2)
                        || !between(config.getNCarb(), 0, 1)) {
                    throw invalid("So mon khong hop le cho " + mealType);
                }
            }
        });
    }

    private DailyPlanResponse.WarningResponse warningFor(String constitution, String goalCode) {
        if ("GAY".equals(constitution) && "GIAM".equals(goalCode)) {
            return warning("WARNING", "UNDERWEIGHT_BUT_LOSE_WEIGHT", "Can nhac muc tieu giam can.", true);
        }
        if ("GAY".equals(constitution) && "DUY_TRI".equals(goalCode)) {
            return warning("INFO", "UNDERWEIGHT_MAINTAIN_WEIGHT", "Nen theo doi the trang khi duy tri can nang.", false);
        }
        if ("THUA_CAN".equals(constitution) && "DUY_TRI".equals(goalCode)) {
            return warning("INFO", "OVERWEIGHT_MAINTAIN_WEIGHT", "Nen theo doi the trang khi duy tri can nang.", false);
        }
        if ("THUA_CAN".equals(constitution) && "TANG".equals(goalCode)) {
            return warning("WARNING", "OVERWEIGHT_BUT_GAIN_WEIGHT", "Can nhac muc tieu tang can.", true);
        }
        if ("BEO_PHI".equals(constitution) && "DUY_TRI".equals(goalCode)) {
            return warning("WARNING", "OBESE_MAINTAIN_WEIGHT", "Can nhac muc tieu duy tri can nang.", true);
        }
        if ("BEO_PHI".equals(constitution) && "TANG".equals(goalCode)) {
            return warning("STRONG_WARNING", "OBESE_BUT_GAIN_WEIGHT", "Khuyến nghị đổi mục tiêu trước khi tăng cân.", true);
        }
        return null;
    }

    private DailyPlanResponse.WarningResponse warning(
            String level,
            String code,
            String message,
            boolean requireConfirm) {
        return DailyPlanResponse.WarningResponse.builder()
                .level(level)
                .code(code)
                .message(message)
                .requireConfirm(requireConfirm)
                .build();
    }

    private List<HistoryEntry> loadHistory(String userId, LocalDate targetDate, LoadedConfigs configs) {
        int days = configs.getInt("penalty.lookback_days");
        LocalDate from = targetDate.minusDays(Math.max(days - 1L, 0L));
        List<MealLog> mealLogs = mealLogRepository
                .findByUserIdAndMealDateBetweenOrderByMealDateDescMealTypeAsc(userId, from, targetDate);
        Map<String, LocalDate> dates = mealLogs.stream()
                .collect(Collectors.toMap(MealLog::getId, MealLog::getMealDate));
        if (dates.isEmpty()) {
            return new ArrayList<>();
        }
        return mealLogDishRepository.findByMealLogIdIn(dates.keySet()).stream()
                .map(dish -> HistoryEntry.builder()
                        .mealDate(dates.get(dish.getMealLogId()))
                        .dishId(dish.getDishId())
                        .foodGroupCode(dish.getFoodGroupCode())
                        .slotCode(dish.getSlotCode())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<HistoryEntry> planHistoryWithoutMeal(DailyPlanResponse currentPlan, MealType skippedMeal) {
        if (currentPlan.getMeals() == null) {
            return List.of();
        }
        return currentPlan.getMeals().stream()
                .filter(meal -> meal.getMealType() != skippedMeal)
                .filter(meal -> meal.getTopCombination() != null)
                .flatMap(meal -> meal.getTopCombination().getDishes().stream())
                .map(dish -> HistoryEntry.builder()
                        .mealDate(currentPlan.getPlanDate())
                        .dishId(dish.getDishId())
                        .foodGroupCode(dish.getFoodGroupCode())
                        .slotCode(dish.getSlotCode())
                        .build())
                .toList();
    }

    private MealSuggestionResponse findMeal(DailyPlanResponse plan, MealType mealType) {
        if (plan.getMeals() == null) {
            throw invalid("currentPlan khong co bua an");
        }
        return plan.getMeals().stream()
                .filter(meal -> meal.getMealType() == mealType)
                .findFirst()
                .orElseThrow(() -> invalid("Khong tim thay bua " + mealType + " trong currentPlan"));
    }

    private int findSwappedIndex(List<DishSuggestionResponse> dishes, String swappedSlot) {
        String[] parts = swappedSlot.split("_");
        if (parts.length < 2) {
            throw invalid("swappedSlot phai co dang SLOT_index");
        }
        String indexText = parts[parts.length - 1];
        String slotText = swappedSlot.substring(0, swappedSlot.length() - indexText.length() - 1);
        SlotCode slotCode;
        int slotIndex;
        try {
            slotCode = SlotCode.valueOf(slotText);
            slotIndex = Integer.parseInt(indexText);
        } catch (Exception e) {
            throw invalid("swappedSlot khong hop le");
        }
        int currentSlotIndex = 0;
        for (int index = 0; index < dishes.size(); index++) {
            if (dishes.get(index).getSlotCode() == slotCode) {
                if (currentSlotIndex == slotIndex) {
                    return index;
                }
                currentSlotIndex++;
            }
        }
        throw invalid("Khong tim thay slot " + swappedSlot);
    }

    private String slotId(List<DishSuggestionResponse> dishes, int targetIndex) {
        SlotCode slotCode = dishes.get(targetIndex).getSlotCode();
        int slotIndex = 0;
        for (int index = 0; index < targetIndex; index++) {
            if (dishes.get(index).getSlotCode() == slotCode) {
                slotIndex++;
            }
        }
        return slotCode.name() + "_" + slotIndex;
    }

    private List<DishSuggestionResponse> copyDishes(List<DishSuggestionResponse> dishes) {
        return dishes.stream()
                .map(this::copyDish)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private DishSuggestionResponse copyDish(DishSuggestionResponse dish) {
        return DishSuggestionResponse.builder()
                .slotKey(dish.getSlotKey())
                .dishId(dish.getDishId())
                .dishName(dish.getDishName())
                .slotCode(dish.getSlotCode())
                .foodGroupCode(dish.getFoodGroupCode())
                .servingMultiplier(dish.getServingMultiplier())
                .actualGrams(dish.getActualGrams())
                .dishKcal(dish.getDishKcal())
                .unit(dish.getUnit())
                .baseServingG(dish.getBaseServingG())
                .favorite(dish.isFavorite())
                .build();
    }

    private Map<String, Dish> loadDishMap(List<DishSuggestionResponse> dishes) {
        Map<String, Dish> dishMap = new HashMap<>();
        dishRepository.findAllById(dishes.stream().map(DishSuggestionResponse::getDishId).toList())
                .forEach(dish -> dishMap.put(dish.getId(), dish));
        Optional<String> missingDish = dishes.stream()
                .map(DishSuggestionResponse::getDishId)
                .filter(id -> !dishMap.containsKey(id))
                .findFirst();
        missingDish.ifPresent(id -> {
            throw invalid("Mon trong currentPlan khong ton tai: " + id);
        });
        return dishMap;
    }

    private Set<String> favoriteIds(String userId) {
        return favoriteDishRepository.findByUserId(userId).stream()
                .map(FavoriteDish::getDishId)
                .collect(Collectors.toSet());
    }

    private boolean between(Integer value, int min, int max) {
        return value != null && value >= min && value <= max;
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.CONFIG_VALIDATION_FAILED, message);
    }

    private record ServingGridResult(
            List<DishSuggestionResponse> dishes,
            MealCombinationResponse score) {
    }
}
