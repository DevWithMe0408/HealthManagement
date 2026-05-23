package org.example.nutritionservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.entity.favorite.FavoriteDish;
import org.example.nutritionservice.service.favorite.FavoriteDishService;
import org.example.web.dto.response.DataResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorite-dishes")
@RequiredArgsConstructor
public class FavoriteDishController {

    private final FavoriteDishService favoriteDishService;

    @PostMapping("/{dishId}")
    public DataResponse<FavoriteDish> addFavorite(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId,
            @PathVariable String dishId) {
        return DataResponse.success(favoriteDishService.addFavorite(resolveUserId(userId, legacyUserId), dishId));
    }

    @DeleteMapping("/{dishId}")
    public DataResponse<Void> removeFavorite(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId,
            @PathVariable String dishId) {
        favoriteDishService.removeFavorite(resolveUserId(userId, legacyUserId), dishId);
        return DataResponse.success();
    }

    @GetMapping
    public DataResponse<List<FavoriteDish>> getFavorites(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId) {
        return DataResponse.success(favoriteDishService.getFavorites(resolveUserId(userId, legacyUserId)));
    }

    private String resolveUserId(String userId, String legacyUserId) {
        return userId != null ? userId : legacyUserId;
    }
}
