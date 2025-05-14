package org.example.healthdataservice.repository;

import org.example.healthdataservice.entity.Measurement;
import org.example.healthdataservice.entity.enums.MeasurementFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement,Long> {

    Optional<Measurement> findLastByUserId(Long userId);

    Optional<Measurement> findByUserId(Long userId);

    Optional<Object> findLatestByUserId(Long userId);

    List<Measurement> findAllByUserId(Long userId);
}
