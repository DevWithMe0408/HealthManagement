package org.example.nutritionservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.response.DishOptionResponse;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.service.recommendation.DishSearchService;
import org.example.web.dto.response.DataResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/nutrition/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishSearchService dishSearchService;

    @GetMapping("/search")
    public DataResponse<List<DishOptionResponse>> searchDishes(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId,
            @RequestParam SlotCode slotCode,
            @RequestParam String q,
            @RequestParam BigDecimal slotKcalTarget) {
        return DataResponse.success(dishSearchService.searchDishes(
                resolveUserId(userId, legacyUserId),
                slotCode,
                q,
                slotKcalTarget
        ));
    }

    private String resolveUserId(String userId, String legacyUserId) {
        return userId != null ? userId : legacyUserId;
    }
}
