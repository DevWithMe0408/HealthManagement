package org.example.nutritionservice.service.favorite;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.entity.favorite.FavoriteDish;
import org.example.nutritionservice.repository.favorite.FavoriteDishRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteDishService {

    private final FavoriteDishRepository favoriteDishRepository;

    @Transactional
    public FavoriteDish addFavorite(String userId, String dishId) {
        if (favoriteDishRepository.existsByUserIdAndDishId(userId, dishId)) {
            return favoriteDishRepository.findById(new org.example.nutritionservice.entity.favorite.FavoriteDishId(
                    userId,
                    dishId
            )).orElseThrow();
        }
        return favoriteDishRepository.save(FavoriteDish.builder()
                .userId(userId)
                .dishId(dishId)
                .build());
    }

    @Transactional
    public void removeFavorite(String userId, String dishId) {
        favoriteDishRepository.deleteByUserIdAndDishId(userId, dishId);
    }

    @Transactional(readOnly = true)
    public List<FavoriteDish> getFavorites(String userId) {
        return favoriteDishRepository.findByUserId(userId);
    }
}
