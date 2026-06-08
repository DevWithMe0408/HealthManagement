package org.example.nutritionservice.repository.catalog;

import org.example.nutritionservice.entity.catalog.Dish;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, String> {

    List<Dish> findBySlotCodeAndIsActiveTrue(SlotCode slotCode);

    // Dem mon dang active cho thong ke dashboard admin
    long countByIsActiveTrue();

    // Dem so mon active theo tung slot cho bieu do do phu kho mon
    @Query("SELECT d.slotCode, COUNT(d) FROM Dish d WHERE d.isActive = TRUE GROUP BY d.slotCode")
    List<Object[]> countActiveBySlot();

    @Query("""
            SELECT d FROM Dish d
            WHERE d.slotCode = :slotCode
              AND d.isActive = TRUE
              AND LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))
            ORDER BY d.name
            """)
    List<Dish> searchByName(
            @Param("slotCode") SlotCode slotCode,
            @Param("name") String name,
            Pageable pageable);

    // ===== Admin CRUD =====
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);

    @Query("""
            SELECT d FROM Dish d
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:slotCode IS NULL OR d.slotCode = :slotCode)
              AND (:isActive IS NULL OR d.isActive = :isActive)
            """)
    Page<Dish> findForAdmin(
            @Param("search") String search,
            @Param("slotCode") SlotCode slotCode,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
