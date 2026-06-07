package org.example.nutritionservice.repository.catalog;

import org.example.nutritionservice.entity.catalog.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, String> {

    // Dem nguyen lieu da co du lieu dinh duong, coi kcal NULL la chua co macro
    long countByKcalPer100gIsNotNull();
}
