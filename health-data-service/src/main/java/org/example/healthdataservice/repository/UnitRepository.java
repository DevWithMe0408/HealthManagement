package org.example.healthdataservice.repository;

import org.example.healthdataservice.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {
    boolean existsByCode(String code);

    Unit findById(long id);

    Optional<Unit> findByCode(String code);
}
