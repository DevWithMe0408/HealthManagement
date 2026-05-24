package org.example.nutritionservice.service.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nutritionservice.domain.recommendation.DishCandidate;
import org.example.nutritionservice.domain.recommendation.DishWithServing;
import org.example.nutritionservice.domain.recommendation.HistoryEntry;
import org.example.nutritionservice.domain.recommendation.LoadedConfigs;
import org.example.nutritionservice.domain.recommendation.MealActual;
import org.example.nutritionservice.domain.recommendation.MealCombination;
import org.example.nutritionservice.domain.recommendation.MealTarget;
import org.example.nutritionservice.domain.recommendation.PerMealConfig;
import org.example.nutritionservice.domain.recommendation.SlotAlternative;
import org.example.nutritionservice.domain.recommendation.UserContext;
import org.example.nutritionservice.entity.catalog.FoodGroup;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.entity.config.SlotConfig;
import org.example.nutritionservice.exception.RecommendationTooComplexException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BruteForceEngine {
    /**
     * Sinh tổ hợp + serving grid + sort top K
     */

    private static final long MAX_WORK_UNITS = 50_000_000L;
    private static final int CALC_SCALE = 4;
    private static final int FINAL_SCALE = 2;
    private static final BigDecimal MAX_KCAL_DEVIATION = new BigDecimal("0.25");
    private static final BigDecimal KCAL_PER_G_PROTEIN = new BigDecimal("4");
    private static final BigDecimal KCAL_PER_G_FAT = new BigDecimal("9");
    private static final BigDecimal KCAL_PER_G_CARB = new BigDecimal("4");

    private final ScoringService scoringService;
    private final PenaltyService penaltyService;

    public List<MealCombination> findTopK(
            UserContext userCtx,
            MealTarget mealTarget,
            Map<SlotCode, List<DishCandidate>> candidatesPerSlot,
            List<HistoryEntry> history,
            Set<String> favoriteIds,
            LoadedConfigs configs,
            int topK) {
        long estimatedWork = estimateWork(mealTarget.getPerMealConfig(), candidatesPerSlot, configs);
        log.debug("Recommendation mealType={} estimatedWork={}", mealTarget.getMealType(), estimatedWork);
        if (!userCtx.isForceCompute() && estimatedWork > MAX_WORK_UNITS) {
            throw new RecommendationTooComplexException(estimatedWork);
        }

        PriorityQueue<MealCombination> topCombinations = new PriorityQueue<>(
                Comparator.comparing(MealCombination::getFinalScore)
        );
        int internalTopK = Math.max(topK, topK * 5);
        for (List<DishCandidate> dishCombo : buildDishCombinations(
                mealTarget.getPerMealConfig(),
                candidatesPerSlot,
                configs
        )) {
            BigDecimal penalty = penaltyService.computePenalty(
                    dishCombo,
                    history,
                    favoriteIds,
                    configs,
                    mealTarget.getMealDate()
            );
            enumerateServings(
                    dishCombo,
                    0,
                    new ArrayList<>(),
                    mealTarget,
                    configs,
                    penalty,
                    internalTopK,
                    topCombinations
            );
        }

        List<MealCombination> sortedCombinations = topCombinations.stream()
                .sorted(Comparator.comparing(MealCombination::getFinalScore).reversed())
                .toList();
        Set<String> seenMainKeys = new HashSet<>();
        List<MealCombination> diverseResults = new ArrayList<>();
        for (MealCombination combination : sortedCombinations) {
            String mainKey = extractMainKey(combination);
            if (!seenMainKeys.add(mainKey)) {
                continue;
            }
            diverseResults.add(combination);
            if (diverseResults.size() >= topK) {
                break;
            }
        }
        return diverseResults;
    }

    /**
     * Toi uu serving khi danh sach mon da duoc pin trong luong swap.
     */
    public MealCombination findBestServingCombo(
            List<DishCandidate> pinnedDishes,
            MealTarget mealTarget,
            LoadedConfigs configs,
            BigDecimal penalty) {
        PriorityQueue<MealCombination> topCombinations = new PriorityQueue<>(
                Comparator.comparing(MealCombination::getFinalScore)
        );
        enumerateServings(
                pinnedDishes,
                0,
                new ArrayList<>(),
                mealTarget,
                configs,
                penalty,
                1,
                topCombinations
        );
        return topCombinations.peek();
    }

    /**
     * Tao key mon chinh de moi nhom chi lay dai dien score cao nhat.
     */
    private String extractMainKey(MealCombination combination) {
        return combination.getDishes().stream()
                .filter(dish -> dish.getCandidate().getSlotCode() == SlotCode.CHINH
                        || dish.getCandidate().getSlotCode() == SlotCode.COMBO)
                .map(dish -> dish.getCandidate().getDishId())
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    /**
     * Tinh danh sach mon thay the cho tung slot cua top combination.
     */
    public Map<String, List<SlotAlternative>> computeSlotAlternatives(
            MealCombination topCombination,
            Map<SlotCode, List<DishCandidate>> candidatesPerSlot,
            MealTarget mealTarget,
            LoadedConfigs configs) {
        Map<String, List<SlotAlternative>> result = new java.util.LinkedHashMap<>();
        int maxPerSlot = Integer.parseInt(
                configs.getSystemConfigs().getOrDefault("display.slot_alternatives_count", "10")
        );
        boolean forbidSameGroup = forbidSameFoodGroupInMain(configs);
        Map<SlotCode, Integer> slotCounter = new EnumMap<>(SlotCode.class);

        for (int dishIndex = 0; dishIndex < topCombination.getDishes().size(); dishIndex++) {
            DishWithServing currentDish = topCombination.getDishes().get(dishIndex);
            SlotCode slot = currentDish.getCandidate().getSlotCode();
            int slotIndex = slotCounter.getOrDefault(slot, 0);
            String slotKey = slot.name() + "_" + slotIndex;
            slotCounter.put(slot, slotIndex + 1);

            Set<String> excludeDishIds = topCombination.getDishes().stream()
                    .filter(dish -> dish.getCandidate().getSlotCode() == slot)
                    .map(dish -> dish.getCandidate().getDishId())
                    .collect(java.util.stream.Collectors.toSet());

            int currentDishIndex = dishIndex;
            List<SlotAlternative> alternatives = candidatesPerSlot.getOrDefault(slot, List.of()).stream()
                    .filter(candidate -> !excludeDishIds.contains(candidate.getDishId()))
                    .map(candidate -> computeOneAlternative(topCombination, currentDishIndex, candidate, mealTarget, configs))
                    .filter(java.util.Objects::nonNull)
                    .sorted(Comparator.comparing(SlotAlternative::getExpectedScore).reversed())
                    .toList();

            result.put(slotKey, diverseAlternatives(slot, alternatives, topCombination, forbidSameGroup, maxPerSlot));
        }
        return result;
    }

    private List<SlotAlternative> diverseAlternatives(
            SlotCode slot,
            List<SlotAlternative> alternatives,
            MealCombination topCombination,
            boolean forbidSameGroup,
            int maxPerSlot) {
        if (slot != SlotCode.CHINH || !forbidSameGroup) {
            return alternatives.stream().limit(maxPerSlot).toList();
        }

        Set<FoodGroup> seenGroups = topCombination.getDishes().stream()
                .filter(dish -> dish.getCandidate().getSlotCode() == SlotCode.CHINH)
                .map(dish -> dish.getCandidate().getFoodGroupCode())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        List<SlotAlternative> diverse = new ArrayList<>();
        for (SlotAlternative alternative : alternatives) {
            if (!seenGroups.add(alternative.getFoodGroupCode())) {
                continue;
            }
            diverse.add(alternative);
            if (diverse.size() >= maxPerSlot) {
                break;
            }
        }
        return diverse;
    }

    private SlotAlternative computeOneAlternative(
            MealCombination topCombination,
            int swappedDishIndex,
            DishCandidate newCandidate,
            MealTarget mealTarget,
            LoadedConfigs configs) {
        BigDecimal bestScore = BigDecimal.valueOf(-1);
        DishWithServing bestServing = null;
        for (BigDecimal serving : servingSteps(newCandidate.getSlotCode(), configs)) {
            DishWithServing newDishWithServing = withServing(newCandidate, serving);
            if (violatesWeightConstraint(newDishWithServing, configs)) {
                continue;
            }

            List<DishWithServing> testCombo = new ArrayList<>(topCombination.getDishes());
            testCombo.set(swappedDishIndex, newDishWithServing);
            MealActual actual = toActual(testCombo);
            BigDecimal kcalDeviation = actual.getKcal().subtract(mealTarget.getMealKcal()).abs()
                    .divide(mealTarget.getMealKcal(), CALC_SCALE, RoundingMode.HALF_UP);
            if (kcalDeviation.compareTo(MAX_KCAL_DEVIATION) > 0) {
                continue;
            }

            BigDecimal macroScore = scoringService.computeMacroScore(
                    actual,
                    mealTarget.getMacroTarget(),
                    configs.getGoalConfig(),
                    configs
            );
            BigDecimal score = macroScore.subtract(topCombination.getPenalty()).max(BigDecimal.ZERO);
            if (score.compareTo(bestScore) > 0) {
                bestScore = score;
                bestServing = newDishWithServing;
            }
        }

        if (bestServing == null) {
            return null;
        }
        return SlotAlternative.builder()
                .candidate(newCandidate)
                .expectedScore(bestScore.setScale(FINAL_SCALE, RoundingMode.HALF_UP))
                .expectedServing(bestServing.getServingMultiplier())
                .expectedActualGrams(bestServing.getActualGrams())
                .build();
    }

    private void enumerateServings(
            List<DishCandidate> dishCombo,
            int dishIndex,
            List<DishWithServing> current,
            MealTarget mealTarget,
            LoadedConfigs configs,
            BigDecimal penalty,
            int topK,
            PriorityQueue<MealCombination> topCombinations) {
        if (dishIndex == dishCombo.size()) {
            scoreServingCombination(current, mealTarget, configs, penalty, topK, topCombinations);
            return;
        }

        if (dishIndex > 0 && shouldPrune(current, dishCombo, dishIndex, mealTarget, configs)) {
            if (log.isDebugEnabled()) {
                log.debug("Pruned serving branch mealType={} dishIndex={}", mealTarget.getMealType(), dishIndex);
            }
            return;
        }

        DishCandidate candidate = dishCombo.get(dishIndex);
        for (BigDecimal serving : servingSteps(candidate.getSlotCode(), configs)) {
            DishWithServing dishWithServing = withServing(candidate, serving);
            if (violatesWeightConstraint(dishWithServing, configs)) {
                continue;
            }
            current.add(dishWithServing);
            enumerateServings(
                    dishCombo,
                    dishIndex + 1,
                    current,
                    mealTarget,
                    configs,
                    penalty,
                    topK,
                    topCombinations
            );
            current.remove(current.size() - 1);
        }
    }

    private boolean shouldPrune(
            List<DishWithServing> current,
            List<DishCandidate> dishCombo,
            int dishIndex,
            MealTarget mealTarget,
            LoadedConfigs configs) {
        BigDecimal kcalSoFar = current.stream()
                .map(DishWithServing::getKcal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal minServing = configs.getDecimal("filter.serving_min");
        BigDecimal maxServing = configs.getDecimal("filter.serving_max");
        BigDecimal kcalRemainingMin = BigDecimal.ZERO;
        BigDecimal kcalRemainingMax = BigDecimal.ZERO;
        for (int index = dishIndex; index < dishCombo.size(); index++) {
            BigDecimal baseKcal = dishCombo.get(index).getBaseKcal();
            kcalRemainingMin = kcalRemainingMin.add(baseKcal.multiply(minServing));
            kcalRemainingMax = kcalRemainingMax.add(baseKcal.multiply(maxServing));
        }

        BigDecimal lowerBound = mealTarget.getMealKcal().multiply(BigDecimal.ONE.subtract(MAX_KCAL_DEVIATION));
        BigDecimal upperBound = mealTarget.getMealKcal().multiply(BigDecimal.ONE.add(MAX_KCAL_DEVIATION));
        return kcalSoFar.add(kcalRemainingMax).compareTo(lowerBound) < 0
                || kcalSoFar.add(kcalRemainingMin).compareTo(upperBound) > 0;
    }

    private void scoreServingCombination(
            List<DishWithServing> current,
            MealTarget mealTarget,
            LoadedConfigs configs,
            BigDecimal penalty,
            int topK,
            PriorityQueue<MealCombination> topCombinations) {
        MealActual actual = toActual(current);
        BigDecimal kcalDeviation = actual.getKcal().subtract(mealTarget.getMealKcal()).abs()
                .divide(mealTarget.getMealKcal(), CALC_SCALE, RoundingMode.HALF_UP);
        if (kcalDeviation.compareTo(MAX_KCAL_DEVIATION) > 0) {
            return;
        }

        BigDecimal macroScore = scoringService.computeMacroScore(
                actual,
                mealTarget.getMacroTarget(),
                configs.getGoalConfig(),
                configs
        );
        BigDecimal finalScore = macroScore.subtract(penalty).max(BigDecimal.ZERO)
                .setScale(FINAL_SCALE, RoundingMode.HALF_UP);
        MealCombination result = MealCombination.builder()
                .dishes(List.copyOf(current))
                .actual(actual)
                .macroScore(macroScore)
                .penalty(penalty)
                .finalScore(finalScore)
                .build();

        if (topCombinations.size() < topK) {
            topCombinations.add(result);
        } else if (topCombinations.peek() != null
                && result.getFinalScore().compareTo(topCombinations.peek().getFinalScore()) > 0) {
            topCombinations.poll();
            topCombinations.add(result);
        }
    }

    private DishWithServing withServing(DishCandidate candidate, BigDecimal serving) {
        return DishWithServing.builder()
                .candidate(candidate)
                .servingMultiplier(serving)
                .actualGrams(candidate.getBaseServingG().multiply(serving).setScale(FINAL_SCALE, RoundingMode.HALF_UP))
                .kcal(candidate.getBaseKcal().multiply(serving).setScale(FINAL_SCALE, RoundingMode.HALF_UP))
                .proteinG(candidate.getBaseProteinG().multiply(serving).setScale(CALC_SCALE, RoundingMode.HALF_UP))
                .fatG(candidate.getBaseFatG().multiply(serving).setScale(CALC_SCALE, RoundingMode.HALF_UP))
                .carbG(candidate.getBaseCarbG().multiply(serving).setScale(CALC_SCALE, RoundingMode.HALF_UP))
                .build();
    }

    private MealActual toActual(List<DishWithServing> dishes) {
        BigDecimal protein = sum(dishes, DishWithServing::getProteinG);
        BigDecimal fat = sum(dishes, DishWithServing::getFatG);
        BigDecimal carb = sum(dishes, DishWithServing::getCarbG);
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

    private BigDecimal sum(List<DishWithServing> dishes, ValueExtractor extractor) {
        return dishes.stream()
                .map(extractor::extract)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean violatesWeightConstraint(DishWithServing dish, LoadedConfigs configs) {
        SlotConfig slotConfig = configs.getSlotConfigs().get(dish.getCandidate().getSlotCode());
        if (slotConfig == null) {
            return false;
        }
        return dish.getActualGrams().compareTo(BigDecimal.valueOf(slotConfig.getMinG())) < 0
                || dish.getActualGrams().compareTo(BigDecimal.valueOf(slotConfig.getMaxG())) > 0;
    }

    private List<BigDecimal> servingSteps(SlotCode slotCode, LoadedConfigs configs) {
        return slotCode == SlotCode.COMBO
                ? configs.getDecimalArray("filter.combo_serving_steps")
                : configs.getDecimalArray("filter.serving_steps");
    }

    private List<List<DishCandidate>> buildDishCombinations(
            PerMealConfig perMealConfig,
            Map<SlotCode, List<DishCandidate>> candidatesPerSlot,
            LoadedConfigs configs) {
        Map<SlotCode, Integer> counts = requestedCounts(perMealConfig);
        List<List<DishCandidate>> allCombinations = new ArrayList<>();
        allCombinations.add(new ArrayList<>());
        for (Map.Entry<SlotCode, Integer> entry : counts.entrySet()) {
            List<List<DishCandidate>> slotCombinations = choose(
                    candidatesPerSlot.getOrDefault(entry.getKey(), List.of()),
                    entry.getValue()
            );
            List<List<DishCandidate>> combined = new ArrayList<>();
            for (List<DishCandidate> prefix : allCombinations) {
                for (List<DishCandidate> slotCombination : slotCombinations) {
                    List<DishCandidate> next = new ArrayList<>(prefix);
                    next.addAll(slotCombination);
                    combined.add(next);
                }
            }
            allCombinations = combined;
        }
        if (forbidSameFoodGroupInMain(configs)) {
            allCombinations = allCombinations.stream()
                    .filter(this::hasDistinctMainFoodGroups)
                    .toList();
        }
        return allCombinations;
    }

    private boolean forbidSameFoodGroupInMain(LoadedConfigs configs) {
        return Boolean.parseBoolean(
                configs.getSystemConfigs().getOrDefault("filter.forbid_same_food_group_in_main", "true")
        );
    }

    private boolean hasDistinctMainFoodGroups(List<DishCandidate> combination) {
        Set<org.example.nutritionservice.entity.catalog.FoodGroup> seenGroups = new HashSet<>();
        for (DishCandidate candidate : combination) {
            if (candidate.getSlotCode() == SlotCode.CHINH
                    && !seenGroups.add(candidate.getFoodGroupCode())) {
                return false;
            }
        }
        return true;
    }

    private List<List<DishCandidate>> choose(List<DishCandidate> candidates, int count) {
        List<List<DishCandidate>> results = new ArrayList<>();
        choose(candidates, count, 0, new ArrayList<>(), results);
        return results;
    }

    private void choose(
            List<DishCandidate> candidates,
            int remaining,
            int start,
            List<DishCandidate> current,
            List<List<DishCandidate>> results) {
        if (remaining == 0) {
            results.add(List.copyOf(current));
            return;
        }
        for (int index = start; index <= candidates.size() - remaining; index++) {
            current.add(candidates.get(index));
            choose(candidates, remaining - 1, index + 1, current, results);
            current.remove(current.size() - 1);
        }
    }

    private long estimateWork(
            PerMealConfig perMealConfig,
            Map<SlotCode, List<DishCandidate>> candidatesPerSlot,
            LoadedConfigs configs) {
        long dishCombinationCount = 1;
        long servingCombinationCount = 1;
        for (Map.Entry<SlotCode, Integer> entry : requestedCounts(perMealConfig).entrySet()) {
            int candidateCount = candidatesPerSlot.getOrDefault(entry.getKey(), List.of()).size();
            dishCombinationCount = safeMultiply(dishCombinationCount, combinations(candidateCount, entry.getValue()));
            int servingStepCount = servingSteps(entry.getKey(), configs).size();
            servingCombinationCount = safeMultiply(
                    servingCombinationCount,
                    pow(servingStepCount, entry.getValue())
            );
        }
        return safeMultiply(dishCombinationCount, servingCombinationCount);
    }

    private Map<SlotCode, Integer> requestedCounts(PerMealConfig perMealConfig) {
        Map<SlotCode, Integer> counts = new EnumMap<>(SlotCode.class);
        for (SlotCode slotCode : List.of(SlotCode.COMBO, SlotCode.CHINH, SlotCode.RAU, SlotCode.TINH_BOT)) {
            int count = perMealConfig.countForSlot(slotCode);
            if (count > 0) {
                counts.put(slotCode, count);
            }
        }
        return counts;
    }

    private long combinations(int n, int k) {
        if (k < 0 || k > n) {
            return 0;
        }
        long result = 1;
        for (int index = 1; index <= k; index++) {
            result = safeMultiply(result, n - k + index) / index;
        }
        return result;
    }

    private long pow(int base, int exponent) {
        long result = 1;
        for (int index = 0; index < exponent; index++) {
            result = safeMultiply(result, base);
        }
        return result;
    }

    private long safeMultiply(long left, long right) {
        if (left == 0 || right == 0) {
            return 0;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }

    @FunctionalInterface
    private interface ValueExtractor {
        BigDecimal extract(DishWithServing dish);
    }
}
