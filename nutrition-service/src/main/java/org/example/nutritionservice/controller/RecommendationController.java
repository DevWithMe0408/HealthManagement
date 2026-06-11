package org.example.nutritionservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.request.ConfirmMealRequest;
import org.example.nutritionservice.dto.request.DayPlanRequest;
import org.example.nutritionservice.dto.request.RecommendFullDayRequest;
import org.example.nutritionservice.dto.request.SwapDishRequest;
import org.example.nutritionservice.dto.request.UpdateMealStatusRequest;
import org.example.nutritionservice.dto.response.DailyPlanResponse;
import org.example.nutritionservice.dto.response.DishSuggestionResponse;
import org.example.nutritionservice.dto.response.MealLogHistoryResponse;
import org.example.nutritionservice.dto.response.SwapResultResponse;
import org.example.nutritionservice.entity.meallog.MealLog;
import org.example.nutritionservice.entity.meallog.MealLogDish;
import org.example.nutritionservice.service.meallog.MealLogService;
import org.example.nutritionservice.service.recommendation.RecommendationApiService;
import org.example.web.dto.response.DataResponse;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationApiService recommendationApiService;
    private final MealLogService mealLogService;

    @PostMapping("/api/recommendation/full-day")
    public DataResponse<DailyPlanResponse> recommendFullDay(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId,
            @RequestBody @Valid RecommendFullDayRequest request) {
        return DataResponse.success(recommendationApiService.recommendFullDay(resolveUserId(userId, legacyUserId), request));
    }

    @PostMapping("/api/recommendation/day-plan")
    public DataResponse<DailyPlanResponse> dayPlan(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId,
            @RequestBody @Valid DayPlanRequest request) {
        return DataResponse.success(recommendationApiService.buildDayPlan(resolveUserId(userId, legacyUserId), request));
    }

    @PostMapping("/api/recommendation/swap-dish")
    public DataResponse<SwapResultResponse> swapDish(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId,
            @RequestBody @Valid SwapDishRequest request) {
        return DataResponse.success(recommendationApiService.swapDish(resolveUserId(userId, legacyUserId), request));
    }

    @PostMapping("/api/meal-log/confirm")
    public DataResponse<MealLogHistoryResponse> confirmMeal(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId,
            @RequestBody @Valid ConfirmMealRequest request) {
        MealLog saved = mealLogService.confirmMeal(resolveUserId(userId, legacyUserId), request);
        return DataResponse.success(toHistoryResponses(List.of(saved)).get(0));
    }

    @GetMapping("/api/meal-log/history")
    public DataResponse<List<MealLogHistoryResponse>> getHistory(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId,
            @RequestParam(defaultValue = "3") int days) {
        if (days < 1 || days > 30) {
            throw new BusinessException(ErrorCode.CONFIG_VALIDATION_FAILED, "days phai nam trong khoang 1..30");
        }
        List<MealLog> logs = mealLogService.getHistory(resolveUserId(userId, legacyUserId), days, LocalDate.now());
        return DataResponse.success(toHistoryResponses(logs));
    }

    @GetMapping("/api/meal-log/history-range")
    public DataResponse<List<MealLogHistoryResponse>> getHistoryRange(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException(ErrorCode.CONFIG_VALIDATION_FAILED, "from phai <= to");
        }
        if (from.plusDays(366).isBefore(to)) {
            throw new BusinessException(ErrorCode.CONFIG_VALIDATION_FAILED, "Khoang thoi gian toi da 366 ngay");
        }
        List<MealLog> logs = mealLogService.getHistoryRange(resolveUserId(userId, legacyUserId), from, to);
        return DataResponse.success(toHistoryResponses(logs));
    }

    @PatchMapping("/api/meal-log/{id}/status")
    public DataResponse<MealLogHistoryResponse> updateMealStatus(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId,
            @PathVariable String id,
            @RequestBody @Valid UpdateMealStatusRequest request) {
        MealLog updated = mealLogService.updateStatus(
                resolveUserId(userId, legacyUserId),
                id,
                request.getStatus(),
                request.getCustomNote()
        );
        return DataResponse.success(toHistoryResponses(List.of(updated)).get(0));
    }

    private List<MealLogHistoryResponse> toHistoryResponses(List<MealLog> logs) {
        Map<String, List<MealLogDish>> dishesByLog = mealLogService.getHistoryDishes(logs).stream()
                .collect(Collectors.groupingBy(MealLogDish::getMealLogId));
        Set<String> dishIds = dishesByLog.values().stream()
                .flatMap(List::stream)
                .map(MealLogDish::getDishId)
                .collect(Collectors.toSet());
        Map<String, String> dishNames = mealLogService.getDishNames(dishIds);
        return logs.stream()
                .map(log -> toHistoryResponse(
                        log,
                        dishesByLog.getOrDefault(log.getId(), List.of()),
                        dishNames))
                .toList();
    }

    private MealLogHistoryResponse toHistoryResponse(
            MealLog log,
            List<MealLogDish> dishes,
            Map<String, String> dishNames) {
        return MealLogHistoryResponse.builder()
                .id(log.getId())
                .mealDate(log.getMealDate())
                .mealType(log.getMealType())
                .planType(log.getPlanType())
                .goalCode(log.getGoalCode())
                .mealKcalTarget(log.getMealKcalTarget())
                .totalKcalActual(log.getTotalKcalActual())
                .totalProtein(log.getTotalProteinG())
                .totalFat(log.getTotalFatG())
                .totalCarb(log.getTotalCarbG())
                .finalScore(log.getFinalScore())
                .status(log.getStatus())
                .customNote(log.getCustomNote())
                .dishes(dishes.stream()
                        .map(dish -> toDishResponse(dish, dishNames))
                        .toList())
                .build();
    }

    private DishSuggestionResponse toDishResponse(MealLogDish dish, Map<String, String> dishNames) {
        return DishSuggestionResponse.builder()
                .dishId(dish.getDishId())
                .dishName(dishNames.get(dish.getDishId()))
                .slotCode(dish.getSlotCode())
                .foodGroupCode(dish.getFoodGroupCode())
                .servingMultiplier(dish.getServingMultiplier())
                .actualGrams(dish.getActualGrams())
                .dishKcal(dish.getDishKcal())
                .favorite(false)
                .build();
    }

    private String resolveUserId(String userId, String legacyUserId) {
        return userId != null ? userId : legacyUserId;
    }
}
