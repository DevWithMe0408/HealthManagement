package org.example.nutritionservice.repository.meallog;

import org.example.nutritionservice.entity.meallog.MealLogDish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MealLogDishRepository extends JpaRepository<MealLogDish, String> {

    List<MealLogDish> findByMealLogIdIn(Collection<String> mealLogIds);

    void deleteByMealLogId(String mealLogId);
}
