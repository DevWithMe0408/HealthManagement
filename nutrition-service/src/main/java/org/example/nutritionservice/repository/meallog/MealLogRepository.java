package org.example.nutritionservice.repository.meallog;

import org.example.nutritionservice.entity.meallog.MealLog;
import org.example.nutritionservice.entity.meallog.MealType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealLogRepository extends JpaRepository<MealLog, String> {

    // Query lịch sử N ngày gần nhất (cho penalty)
    List<MealLog> findByUserIdAndMealDateBetweenOrderByMealDateDescMealTypeAsc(
        String userId,
        LocalDate from,
        LocalDate to
    );

    // Check bữa nào trong ngày đã có record (cho determine_remaining_meals)
    List<MealLog> findByUserIdAndMealDate(String userId, LocalDate mealDate);

    // Optional: query 1 bữa cụ thể
    Optional<MealLog> findByUserIdAndMealDateAndMealType(
        String userId,
        LocalDate mealDate,
        MealType mealType
    );
}
