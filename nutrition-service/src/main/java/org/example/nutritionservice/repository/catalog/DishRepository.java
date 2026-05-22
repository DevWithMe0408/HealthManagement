package org.example.nutritionservice.repository.catalog;

import org.example.nutritionservice.entity.catalog.Dish;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, String> {

    List<Dish> findBySlotCodeAndIsActiveTrue(SlotCode slotCode);
}
