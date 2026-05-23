package org.example.nutritionservice.service.meallog;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.request.ConfirmMealRequest;
import org.example.nutritionservice.dto.response.DishSuggestionResponse;
import org.example.nutritionservice.dto.response.MealCombinationResponse;
import org.example.nutritionservice.entity.meallog.MealLog;
import org.example.nutritionservice.entity.meallog.MealLogDish;
import org.example.nutritionservice.entity.meallog.MealStatus;
import org.example.nutritionservice.repository.meallog.MealLogDishRepository;
import org.example.nutritionservice.repository.meallog.MealLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealLogService {

    private final MealLogRepository mealLogRepository;
    private final MealLogDishRepository mealLogDishRepository;

    @Transactional
    public MealLog confirmMeal(String userId, ConfirmMealRequest request) {
        MealLog mealLog = mealLogRepository.findByUserIdAndMealDateAndMealType(
                        userId,
                        request.getMealDate(),
                        request.getMealType()
                )
                .orElseGet(() -> MealLog.builder()
                        .userId(userId)
                        .mealDate(request.getMealDate())
                        .mealType(request.getMealType())
                        .status(MealStatus.SUGGESTED)
                        .build());

        MealCombinationResponse combination = request.getSelectedCombination();
        mealLog.setPlanType(request.getPlanType());
        mealLog.setGoalCode(request.getGoalCode());
        mealLog.setMealKcalTarget(request.getMealKcalTarget());
        mealLog.setTotalKcalActual(combination.getTotalKcal());
        mealLog.setTotalProteinG(combination.getTotalProtein());
        mealLog.setTotalFatG(combination.getTotalFat());
        mealLog.setTotalCarbG(combination.getTotalCarb());
        mealLog.setFinalScore(combination.getFinalScore());
        mealLog.setStatus(MealStatus.SUGGESTED);
        MealLog saved = mealLogRepository.save(mealLog);

        mealLogDishRepository.deleteByMealLogId(saved.getId());
        mealLogDishRepository.saveAll(toMealLogDishes(saved.getId(), combination.getDishes()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<MealLog> getHistory(String userId, int days, LocalDate today) {
        LocalDate from = today.minusDays(Math.max(days - 1L, 0L));
        return mealLogRepository.findByUserIdAndMealDateBetweenOrderByMealDateDescMealTypeAsc(
                userId,
                from,
                today
        );
    }

    @Transactional(readOnly = true)
    public List<MealLogDish> getHistoryDishes(List<MealLog> mealLogs) {
        return mealLogDishRepository.findByMealLogIdIn(mealLogs.stream()
                .map(MealLog::getId)
                .toList());
    }

    private List<MealLogDish> toMealLogDishes(String mealLogId, List<DishSuggestionResponse> dishes) {
        short sortOrder = 1;
        List<MealLogDish> rows = new java.util.ArrayList<>();
        for (DishSuggestionResponse dish : dishes) {
            rows.add(MealLogDish.builder()
                    .mealLogId(mealLogId)
                    .dishId(dish.getDishId())
                    .foodGroupCode(dish.getFoodGroupCode())
                    .slotCode(dish.getSlotCode())
                    .servingMultiplier(dish.getServingMultiplier())
                    .actualGrams(dish.getActualGrams())
                    .dishKcal(dish.getDishKcal())
                    .sortOrder(sortOrder++)
                    .build());
        }
        return rows;
    }
}
