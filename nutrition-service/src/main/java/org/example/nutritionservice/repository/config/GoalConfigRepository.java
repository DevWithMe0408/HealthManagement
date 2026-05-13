package org.example.nutritionservice.repository.config;

import org.example.nutritionservice.entity.config.GoalConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalConfigRepository extends JpaRepository<GoalConfig, String> {
    // dung cac method cua JpaRepository cung cap san
}
