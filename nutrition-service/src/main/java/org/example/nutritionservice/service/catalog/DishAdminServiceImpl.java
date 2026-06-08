package org.example.nutritionservice.service.catalog;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.request.DishUpsertRequest;
import org.example.nutritionservice.dto.response.DishAdminResponse;
import org.example.nutritionservice.entity.catalog.Dish;
import org.example.nutritionservice.entity.catalog.FoodGroup;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.repository.catalog.DishRepository;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DishAdminServiceImpl implements DishAdminService {

    private final DishRepository dishRepository;

    @Override
    public Page<DishAdminResponse> list(String search, SlotCode slotCode, Boolean isActive, Pageable pageable) {
        return dishRepository.findForAdmin(search, slotCode, isActive, pageable).map(this::toResponse);
    }

    @Override
    public DishAdminResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public DishAdminResponse create(DishUpsertRequest req, String actor) {
        String name = req.getName().trim();
        if (dishRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.DISH_NAME_TAKEN);
        }
        validateFoodGroup(req.getFoodGroupCode());

        Dish dish = new Dish();
        applyEditableFields(dish, req);
        applyMacros(dish, req);
        dish.setIsActive(req.getIsActive() == null ? Boolean.TRUE : req.getIsActive());
        dish.setCreatedBy(actor);
        dish.setUpdatedBy(actor);
        return toResponse(dishRepository.save(dish));
    }

    @Override
    @Transactional
    public DishAdminResponse update(String id, DishUpsertRequest req, String actor) {
        Dish dish = findOrThrow(id);
        String name = req.getName().trim();
        if (dishRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.DISH_NAME_TAKEN);
        }
        validateFoodGroup(req.getFoodGroupCode());

        applyEditableFields(dish, req);
        applyMacros(dish, req);
        if (req.getIsActive() != null) {
            dish.setIsActive(req.getIsActive());
        }
        dish.setUpdatedBy(actor);
        return toResponse(dishRepository.save(dish));
    }

    @Override
    @Transactional
    public DishAdminResponse setActive(String id, boolean active, String actor) {
        Dish dish = findOrThrow(id);
        dish.setIsActive(active);
        dish.setUpdatedBy(actor);
        return toResponse(dishRepository.save(dish));
    }

    private Dish findOrThrow(String id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DISH_NOT_FOUND));
    }

    private void validateFoodGroup(FoodGroup foodGroup) {
        // GIA_VI chi danh cho nguyen lieu, khong dung cho mon an
        if (foodGroup == FoodGroup.GIA_VI) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "foodGroupCode GIA_VI chi danh cho nguyen lieu, khong dung cho mon an");
        }
    }

    private void applyEditableFields(Dish dish, DishUpsertRequest req) {
        dish.setName(req.getName().trim());
        dish.setSlotCode(req.getSlotCode());
        dish.setFoodGroupCode(req.getFoodGroupCode());
        dish.setBaseServingG(req.getBaseServingG());
        dish.setUnit(req.getUnit().trim());
        dish.setDescription(req.getDescription());
    }

    // Diem mo rong tuong lai: neu tinh macro tu dish_ingredients, chi sua ham nay.
    private void applyMacros(Dish dish, DishUpsertRequest req) {
        dish.setKcalPer100g(req.getKcalPer100g());
        dish.setProteinPer100g(req.getProteinPer100g());
        dish.setFatPer100g(req.getFatPer100g());
        dish.setCarbPer100g(req.getCarbPer100g());
    }

    private DishAdminResponse toResponse(Dish dish) {
        return DishAdminResponse.builder()
                .id(dish.getId())
                .name(dish.getName())
                .slotCode(dish.getSlotCode())
                .foodGroupCode(dish.getFoodGroupCode())
                .kcalPer100g(dish.getKcalPer100g())
                .proteinPer100g(dish.getProteinPer100g())
                .fatPer100g(dish.getFatPer100g())
                .carbPer100g(dish.getCarbPer100g())
                .baseServingG(dish.getBaseServingG())
                .unit(dish.getUnit())
                .description(dish.getDescription())
                .isActive(dish.getIsActive())
                .createdAt(dish.getCreatedAt())
                .updatedAt(dish.getUpdatedAt())
                .updatedBy(dish.getUpdatedBy())
                .build();
    }
}
