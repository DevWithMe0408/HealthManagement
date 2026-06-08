package org.example.nutritionservice.service.catalog;

import org.example.nutritionservice.dto.request.DishUpsertRequest;
import org.example.nutritionservice.dto.response.DishAdminResponse;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DishAdminService {
    Page<DishAdminResponse> list(String search, SlotCode slotCode, Boolean isActive, Pageable pageable);

    DishAdminResponse getById(String id);

    DishAdminResponse create(DishUpsertRequest req, String actor);

    DishAdminResponse update(String id, DishUpsertRequest req, String actor);

    DishAdminResponse setActive(String id, boolean active, String actor);
}
