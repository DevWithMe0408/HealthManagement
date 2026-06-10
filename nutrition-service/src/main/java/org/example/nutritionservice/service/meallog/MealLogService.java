package org.example.nutritionservice.service.meallog;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.request.ConfirmMealRequest;
import org.example.nutritionservice.dto.response.DishSuggestionResponse;
import org.example.nutritionservice.dto.response.MealCombinationResponse;
import org.example.nutritionservice.entity.catalog.Dish;
import org.example.nutritionservice.entity.meallog.MealLog;
import org.example.nutritionservice.entity.meallog.MealLogDish;
import org.example.nutritionservice.entity.meallog.MealStatus;
import org.example.nutritionservice.repository.catalog.DishRepository;
import org.example.nutritionservice.repository.meallog.MealLogDishRepository;
import org.example.nutritionservice.repository.meallog.MealLogRepository;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealLogService {

    private final MealLogRepository mealLogRepository;
    private final MealLogDishRepository mealLogDishRepository;
    private final DishRepository dishRepository;

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
        if (mealLogs.isEmpty()) {
            return List.of();
        }
        return mealLogDishRepository.findByMealLogIdIn(mealLogs.stream()
                .map(MealLog::getId)
                .toList());
    }

    @Transactional(readOnly = true)
    public Map<String, String> getDishNames(Collection<String> dishIds) {
        if (dishIds.isEmpty()) {
            return Map.of();
        }
        return dishRepository.findAllById(dishIds).stream()
                .collect(Collectors.toMap(Dish::getId, Dish::getName));
    }

    @Transactional(readOnly = true)
    public List<MealLog> getHistoryRange(String userId, LocalDate from, LocalDate to) {
        return mealLogRepository.findByUserIdAndMealDateBetweenOrderByMealDateDescMealTypeAsc(
                userId,
                from,
                to
        );
    }

    @Transactional
    public MealLog updateStatus(String userId, String id, MealStatus status, String customNote) {
        MealLog log = mealLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MEAL_LOG_NOT_FOUND,
                        "Khong tim thay ban ghi bua an"
                ));
        if (!log.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Khong co quyen sua ban ghi nay");
        }
        log.setStatus(status);
        log.setCustomNote(status == MealStatus.CUSTOM ? customNote : null);
        return mealLogRepository.save(log);
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
