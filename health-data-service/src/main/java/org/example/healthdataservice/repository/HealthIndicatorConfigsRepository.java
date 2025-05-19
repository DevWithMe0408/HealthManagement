package org.example.healthdataservice.repository;

import org.example.healthdataservice.entity.HealthIndicatorConfigs;
import org.example.healthdataservice.entity.Measurement;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface HealthIndicatorConfigsRepository extends JpaRepository<HealthIndicatorConfigs,Long> {
    Optional<HealthIndicatorConfigs> findByIndicatorTypeAndUserId(IndicatorType indicatorType, Long userId);

    Optional<HealthIndicatorConfigs> findByIndicatorType(IndicatorType indicatorType);

    List<HealthIndicatorConfigs> findByUserIdAndIsActiveTrue(Long userId);

    Optional<HealthIndicatorConfigs> getByIndicatorType(IndicatorType indicatorType);

    boolean existsByUserId(Long userId);

}
