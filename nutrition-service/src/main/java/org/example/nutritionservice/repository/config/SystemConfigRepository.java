package org.example.nutritionservice.repository.config;

import org.example.nutritionservice.entity.config.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {

    List<SystemConfig> findByConfigKeyStartingWith(String prefix);
}
