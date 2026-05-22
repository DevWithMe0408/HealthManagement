package org.example.nutritionservice.repository.favorite;

import org.example.nutritionservice.entity.favorite.FavoriteDish;
import org.example.nutritionservice.entity.favorite.FavoriteDishId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteDishRepository extends JpaRepository<FavoriteDish, FavoriteDishId> {

    List<FavoriteDish> findByUserId(String userId);

    boolean existsByUserIdAndDishId(String userId, String dishId);

    void deleteByUserIdAndDishId(String userId, String dishId);
}
