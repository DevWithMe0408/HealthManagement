package org.example.nutritionservice.repository.config;

import org.example.nutritionservice.entity.config.PenaltyConfig;
import org.example.nutritionservice.entity.config.PenaltyConfigId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PenaltyConfigRepository extends JpaRepository<PenaltyConfig, PenaltyConfigId> {

    List<PenaltyConfig> findByLayerOrderByDistanceDaysAsc(Integer layer);
}
