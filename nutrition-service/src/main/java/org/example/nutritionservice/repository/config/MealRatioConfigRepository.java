package org.example.nutritionservice.repository.config;

import org.example.nutritionservice.entity.config.MealRatioConfig;
import org.example.nutritionservice.entity.config.MealRatioConfigId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MealRatioConfigRepository extends JpaRepository<MealRatioConfig, MealRatioConfigId> {

    List<MealRatioConfig> findByPlanTypeOrderBySortOrderAsc(String planType);
}
