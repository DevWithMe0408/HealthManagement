package org.example.nutritionservice.repository.favorite;

import org.example.nutritionservice.entity.favorite.FavoriteDish;
import org.example.nutritionservice.entity.favorite.FavoriteDishId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteDishRepository extends JpaRepository<FavoriteDish, FavoriteDishId> {

    // Lấy tất cả favorite của user (để in-memory check khi tính penalty)
    List<FavoriteDish> findByUserId(String userId);

    // Check 1 dish cụ thể có favorite không
    boolean existsByUserIdAndDishId(String userId, String dishId);

    // Delete cho toggle off
    void deleteByUserIdAndDishId(String userId, String dishId);
}
