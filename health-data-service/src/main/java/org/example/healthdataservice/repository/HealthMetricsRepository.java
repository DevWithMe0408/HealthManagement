package org.example.healthdataservice.repository;

import org.example.healthdataservice.entity.HealthMetrics;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthMetricsRepository extends JpaRepository<HealthMetrics,Long> {

    @Query("""
        SELECT hm FROM HealthMetrics hm
        JOIN FETCH hm.measurement m
        JOIN FETCH hm.indicatorConfig c
        WHERE m.userId = :userId AND c.indicatorType = :type
        ORDER BY m.measurement_time DESC 
        LIMIT 1
""")
    Optional<HealthMetrics> findLatestByIndicatorTypeAndUserId(@Param("type")IndicatorType type,
                                                               @Param("userId") Long userId);

    @Query("""
        SELECT hm FROM HealthMetrics hm
        WHERE hm.measurement.id = :measurementId
        AND hm.indicatorConfig.id = :indicatorConfigId
""")
    Optional<HealthMetrics> findByMeasurementAndIndicatorConfig(@Param("measurementId") Long measurementId,
                                                                @Param("indicatorConfigId") Long indicatorConfigId);

}
