package org.example.nutritionservice.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.request.DishUpsertRequest;
import org.example.nutritionservice.dto.response.DishAdminResponse;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.service.catalog.DishAdminService;
import org.example.web.dto.response.DataResponse;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/dishes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDishController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "name",
            "slotCode",
            "foodGroupCode",
            "kcalPer100g",
            "proteinPer100g",
            "fatPer100g",
            "carbPer100g",
            "baseServingG",
            "unit",
            "isActive",
            "createdAt",
            "updatedAt",
            "updatedBy"
    );

    private final DishAdminService dishAdminService;

    @GetMapping
    public DataResponse<Page<DishAdminResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String slotCode,
            @RequestParam(required = false) Boolean isActive) {

        Pageable pageable = buildPageable(page, size, sort);
        SlotCode slot = parseSlotCode(slotCode);
        return DataResponse.success(dishAdminService.list(search, slot, isActive, pageable));
    }

    @GetMapping("/{id}")
    public DataResponse<DishAdminResponse> getById(@PathVariable String id) {
        return DataResponse.success(dishAdminService.getById(id));
    }

    @PostMapping
    public DataResponse<DishAdminResponse> create(
            @RequestBody @Valid DishUpsertRequest request,
            Authentication auth) {
        return DataResponse.success(dishAdminService.create(request, actor(auth)));
    }

    @PutMapping("/{id}")
    public DataResponse<DishAdminResponse> update(
            @PathVariable String id,
            @RequestBody @Valid DishUpsertRequest request,
            Authentication auth) {
        return DataResponse.success(dishAdminService.update(id, request, actor(auth)));
    }

    @PatchMapping("/{id}/active")
    public DataResponse<DishAdminResponse> setActive(
            @PathVariable String id,
            @RequestParam boolean active,
            Authentication auth) {
        return DataResponse.success(dishAdminService.setActive(id, active, actor(auth)));
    }

    private Pageable buildPageable(int page, int size, String sort) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "page phai lon hon hoac bang 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "size phai nam trong khoang 1 den " + MAX_PAGE_SIZE);
        }

        String[] sortParts = sort.split(",", 2);
        String sortField = sortParts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "sort field khong hop le: " + sortField);
        }

        Sort.Direction direction = Sort.Direction.ASC;
        if (sortParts.length > 1) {
            String rawDirection = sortParts[1].trim();
            if ("desc".equalsIgnoreCase(rawDirection)) {
                direction = Sort.Direction.DESC;
            } else if (!"asc".equalsIgnoreCase(rawDirection)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "sort direction chi chap nhan asc hoac desc");
            }
        }

        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }

    private SlotCode parseSlotCode(String slotCode) {
        if (slotCode == null || slotCode.isBlank()) {
            return null;
        }
        try {
            return SlotCode.valueOf(slotCode.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "slotCode khong hop le: " + slotCode);
        }
    }

    private String actor(Authentication auth) {
        return auth != null ? auth.getName() : null;
    }
}
