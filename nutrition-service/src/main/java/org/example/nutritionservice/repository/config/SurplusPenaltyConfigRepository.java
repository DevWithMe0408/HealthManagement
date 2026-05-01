package org.example.nutritionservice.repository.config;

import org.example.nutritionservice.entity.config.SurplusPenaltyConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurplusPenaltyConfigRepository extends JpaRepository<SurplusPenaltyConfig, String> {
}
