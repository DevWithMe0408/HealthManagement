package org.example.nutritionservice.service.recommendation;

import org.example.nutritionservice.domain.recommendation.LoadedConfigs;
import org.example.nutritionservice.domain.recommendation.UserContext;
import org.example.nutritionservice.entity.config.MealRatioConfig;
import org.example.nutritionservice.entity.meallog.MealType;
import org.example.nutritionservice.repository.meallog.MealLogRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RecommendationOrchestratorTest {

    private final MealLogRepository mealLogRepository = mock(MealLogRepository.class);
    private final RecommendationOrchestrator orchestrator = new RecommendationOrchestrator(
            null,
            null,
            null,
            null,
            null,
            mealLogRepository,
            null,
            null
    );

    @Test
    void tomorrowPlanReturnsAllConfiguredMealsWithoutCheckingLoggedMeals() throws Exception {
        LocalDateTime requestTime = LocalDateTime.of(2026, 6, 10, 15, 30);
        UserContext userContext = UserContext.builder()
                .userId("user-1")
                .requestTime(requestTime)
                .planDay("TOMORROW")
                .build();
        LoadedConfigs configs = LoadedConfigs.builder()
                .mealRatios(List.of(
                        ratio("3_BUA", "SANG", 1),
                        ratio("3_BUA", "TRUA", 2),
                        ratio("3_BUA", "TOI", 3)
                ))
                .build();

        Object planWindow = determineRemainingMeals(userContext, configs);

        assertEquals(LocalDate.of(2026, 6, 11), planDate(planWindow));
        assertEquals(List.of(MealType.SANG, MealType.TRUA, MealType.TOI), mealTypes(planWindow));
        verifyNoInteractions(mealLogRepository);
    }

    private Object determineRemainingMeals(UserContext userContext, LoadedConfigs configs) throws Exception {
        Method method = RecommendationOrchestrator.class.getDeclaredMethod(
                "determineRemainingMeals",
                UserContext.class,
                LoadedConfigs.class
        );
        method.setAccessible(true);
        return method.invoke(orchestrator, userContext, configs);
    }

    private LocalDate planDate(Object planWindow) throws Exception {
        Method method = planWindow.getClass().getDeclaredMethod("planDate");
        method.setAccessible(true);
        return (LocalDate) method.invoke(planWindow);
    }

    @SuppressWarnings("unchecked")
    private List<MealType> mealTypes(Object planWindow) throws Exception {
        Method method = planWindow.getClass().getDeclaredMethod("mealTypes");
        method.setAccessible(true);
        return (List<MealType>) method.invoke(planWindow);
    }

    private MealRatioConfig ratio(String planType, String mealCode, int sortOrder) {
        MealRatioConfig config = new MealRatioConfig();
        config.setPlanType(planType);
        config.setMealCode(mealCode);
        config.setRatio(BigDecimal.ZERO);
        config.setSortOrder((short) sortOrder);
        return config;
    }
}
