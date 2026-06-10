# Feature 2 — Nhật ký bữa ăn & Tuân thủ (BE)

> **Repo:** `HealthManagement`, service `nutrition-service` (trừ Bước 1 nằm ở module `common`). Chạy guide trong session Agent Code mở tại repo BE.
> **Làm BE TRƯỚC FE** vì FE phụ thuộc các endpoint/field thêm ở đây.
> **Quyết định đã chốt:** PATCH cập nhật trạng thái **theo `id`** (bữa phải đã tồn tại). Thêm **endpoint range `?from&to`** cho điều hướng tuần/tháng.

## Phạm vi & nguyên tắc

5 việc, tất cả nhỏ và additive (không phá schema/contract cũ):
1. Thêm cột nullable `custom_note` vào `meal_log` + field vào DTO history.
2. Điền `dishName` cho món trong history (hiện đang null vì `MealLogDish` chỉ lưu `dishId`).
3. Endpoint `PATCH /api/meal-log/{id}/status` để đặt FOLLOWED/CUSTOM/SKIPPED.
4. Endpoint `GET /api/meal-log/history-range?from&to` (repo đã có sẵn query).
5. Sửa `confirmMeal` để **không reset** trạng thái đã-báo-cáo về SUGGESTED.

| File | Hành động |
|---|---|
| `common/.../web/exception/ErrorCode.java` | Thêm `MEAL_LOG_NOT_FOUND` |
| `nutrition-service/.../entity/meallog/MealLog.java` | Thêm cột `custom_note` |
| `nutrition-service/.../dto/response/MealLogHistoryResponse.java` | Thêm field `customNote` |
| `nutrition-service/.../dto/request/UpdateMealStatusRequest.java` | **Tạo mới** |
| `nutrition-service/.../service/meallog/MealLogService.java` | Inject `DishRepository` + 3 method + sửa `confirmMeal` |
| `nutrition-service/.../controller/RecommendationController.java` | Helper `toHistoryResponses` + sửa mapping + 2 endpoint mới |

---

## Bước 1 — Thêm ErrorCode (`common` module)

File `common/src/main/java/org/example/web/exception/ErrorCode.java`. Thêm một dòng vào enum (đặt cạnh `DISH_NOT_FOUND` cho hợp nhóm; thứ tự không quan trọng):

```java
MEAL_LOG_NOT_FOUND      ("MEALLOG-001", HttpStatus.NOT_FOUND,   "Khong tim thay ban ghi bua an"),
```

> Vì `common` đổi, khi build phải `mvn install` lại module `common` trước (xem Bước 7).

## Bước 2 — Thêm cột `custom_note` vào entity `MealLog`

File `entity/meallog/MealLog.java`. Thêm field sau (đặt ngay sau khối `status`, trước `createdAt`). **Nullable** (không có `nullable=false`):

```java
    @Column(name = "custom_note", length = 500)
    private String customNote; // Ghi chu tu do "da an gi" khi status = CUSTOM; null neu khac
```

> `ddl-auto: update` sẽ tự thêm cột này khi khởi động, không cần migration tay.

## Bước 3 — Thêm `customNote` vào `MealLogHistoryResponse`

File `dto/response/MealLogHistoryResponse.java`. Thêm field (đặt cạnh `status`):

```java
    private String customNote;
```

## Bước 4 — Tạo DTO request cho PATCH

Tạo file mới `dto/request/UpdateMealStatusRequest.java`:

```java
package org.example.nutritionservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.nutritionservice.entity.meallog.MealStatus;

@Data
public class UpdateMealStatusRequest {

    @NotNull
    private MealStatus status; // SUGGESTED | FOLLOWED | CUSTOM | SKIPPED (MODIFIED khong dung tu FE)

    @Size(max = 500)
    private String customNote; // chi dung khi status = CUSTOM
}
```

## Bước 5 — `MealLogService`: inject DishRepository + 3 method + sửa confirm

File `service/meallog/MealLogService.java`.

**5a. Thêm imports:**

```java
import org.example.nutritionservice.entity.catalog.Dish;
import org.example.nutritionservice.repository.catalog.DishRepository;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
```

**5b. Inject `DishRepository`** — thêm vào danh sách field `final` (class đang dùng `@RequiredArgsConstructor`):

```java
    private final DishRepository dishRepository;
```

**5c. Sửa `confirmMeal`:** XÓA dòng `mealLog.setStatus(MealStatus.SUGGESTED);` (dòng nằm ngay trước `MealLog saved = mealLogRepository.save(mealLog);`).

Lý do: bản ghi mới đã được builder đặt `.status(SUGGESTED)`. Bản ghi cũ (đã có) thì giữ nguyên trạng thái hiện tại — nếu trước đó user đã đánh dấu FOLLOWED/CUSTOM/SKIPPED, regenerate + confirm lại sẽ không làm mất dấu đó.

**5d. Thêm 3 method mới** (đặt trong class, ví dụ sau `getHistoryDishes`):

```java
    /** Lay ten mon theo dishId de dien vao history (MealLogDish chi luu dishId). */
    @Transactional(readOnly = true)
    public Map<String, String> getDishNames(Collection<String> dishIds) {
        if (dishIds.isEmpty()) {
            return Map.of();
        }
        return dishRepository.findAllById(dishIds).stream()
                .collect(Collectors.toMap(Dish::getId, Dish::getName));
    }

    /** Lich su bua an trong khoang [from, to] (cho dieu huong tuan/thang). */
    @Transactional(readOnly = true)
    public List<MealLog> getHistoryRange(String userId, LocalDate from, LocalDate to) {
        return mealLogRepository.findByUserIdAndMealDateBetweenOrderByMealDateDescMealTypeAsc(
                userId, from, to);
    }

    /** Cap nhat trang thai mot bua da ton tai (theo id). Khong dung toi danh sach mon. */
    @Transactional
    public MealLog updateStatus(String userId, String id, MealStatus status, String customNote) {
        MealLog log = mealLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MEAL_LOG_NOT_FOUND, "Khong tim thay ban ghi bua an"));
        if (!log.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Khong co quyen sua ban ghi nay");
        }
        log.setStatus(status);
        // Chi giu ghi chu khi an khac; cac trang thai khac thi xoa ghi chu.
        log.setCustomNote(status == MealStatus.CUSTOM ? customNote : null);
        return mealLogRepository.save(log); // updatedAt tu cap nhat (@UpdateTimestamp)
    }
```

## Bước 6 — `RecommendationController`: helper + mapping + 2 endpoint

File `controller/RecommendationController.java`.

**6a. Thêm imports:**

```java
import org.example.nutritionservice.dto.request.UpdateMealStatusRequest;
import org.example.nutritionservice.entity.meallog.MealStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Set;
```

**6b. Thêm helper gom logic build history** (đặt cạnh `toHistoryResponse`). Helper này gộp dishes + resolve tên món một lần, dùng chung cho cả history / range / confirm:

```java
    private List<MealLogHistoryResponse> toHistoryResponses(List<MealLog> logs) {
        Map<String, List<MealLogDish>> dishesByLog = mealLogService.getHistoryDishes(logs).stream()
                .collect(Collectors.groupingBy(MealLogDish::getMealLogId));
        Set<String> dishIds = dishesByLog.values().stream()
                .flatMap(List::stream)
                .map(MealLogDish::getDishId)
                .collect(Collectors.toSet());
        Map<String, String> dishNames = mealLogService.getDishNames(dishIds);
        return logs.stream()
                .map(log -> toHistoryResponse(
                        log,
                        dishesByLog.getOrDefault(log.getId(), List.of()),
                        dishNames))
                .toList();
    }
```

**6c. Sửa `toHistoryResponse`** — thêm tham số `dishNames`, thêm `.customNote(...)`, và truyền `dishNames` xuống `toDishResponse`:

```java
    private MealLogHistoryResponse toHistoryResponse(
            MealLog log, List<MealLogDish> dishes, Map<String, String> dishNames) {
        return MealLogHistoryResponse.builder()
                .id(log.getId())
                .mealDate(log.getMealDate())
                .mealType(log.getMealType())
                .planType(log.getPlanType())
                .goalCode(log.getGoalCode())
                .mealKcalTarget(log.getMealKcalTarget())
                .totalKcalActual(log.getTotalKcalActual())
                .totalProtein(log.getTotalProteinG())
                .totalFat(log.getTotalFatG())
                .totalCarb(log.getTotalCarbG())
                .finalScore(log.getFinalScore())
                .status(log.getStatus())
                .customNote(log.getCustomNote())
                .dishes(dishes.stream()
                        .map(d -> toDishResponse(d, dishNames))
                        .toList())
                .build();
    }
```

**6d. Sửa `toDishResponse`** — thêm tham số `dishNames`, set `.dishName(...)`:

```java
    private DishSuggestionResponse toDishResponse(MealLogDish dish, Map<String, String> dishNames) {
        return DishSuggestionResponse.builder()
                .dishId(dish.getDishId())
                .dishName(dishNames.get(dish.getDishId()))
                .slotCode(dish.getSlotCode())
                .foodGroupCode(dish.getFoodGroupCode())
                .servingMultiplier(dish.getServingMultiplier())
                .actualGrams(dish.getActualGrams())
                .dishKcal(dish.getDishKcal())
                .favorite(false)
                .build();
    }
```

**6e. Sửa `confirmMeal`** — đổi phần build response sang dùng helper mới:

Thay đoạn:
```java
        MealLog saved = mealLogService.confirmMeal(resolveUserId(userId, legacyUserId), request);
        return DataResponse.success(toHistoryResponse(
                saved,
                mealLogService.getHistoryDishes(List.of(saved))
        ));
```
bằng:
```java
        MealLog saved = mealLogService.confirmMeal(resolveUserId(userId, legacyUserId), request);
        return DataResponse.success(toHistoryResponses(List.of(saved)).get(0));
```

**6f. Sửa `getHistory`** — đổi phần build response sang helper mới. Thay đoạn:
```java
        List<MealLog> logs = mealLogService.getHistory(resolveUserId(userId, legacyUserId), days, LocalDate.now());
        Map<String, List<MealLogDish>> dishesByLog = mealLogService.getHistoryDishes(logs).stream()
                .collect(Collectors.groupingBy(MealLogDish::getMealLogId));
        return DataResponse.success(logs.stream()
                .map(log -> toHistoryResponse(log, dishesByLog.getOrDefault(log.getId(), List.of())))
                .toList());
```
bằng:
```java
        List<MealLog> logs = mealLogService.getHistory(resolveUserId(userId, legacyUserId), days, LocalDate.now());
        return DataResponse.success(toHistoryResponses(logs));
```

**6g. Thêm 2 endpoint mới** (đặt sau `getHistory`):

```java
    @GetMapping("/api/meal-log/history-range")
    public DataResponse<List<MealLogHistoryResponse>> getHistoryRange(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException(ErrorCode.CONFIG_VALIDATION_FAILED, "from phai <= to");
        }
        if (from.plusDays(366).isBefore(to)) {
            throw new BusinessException(ErrorCode.CONFIG_VALIDATION_FAILED, "Khoang thoi gian toi da 366 ngay");
        }
        List<MealLog> logs = mealLogService.getHistoryRange(resolveUserId(userId, legacyUserId), from, to);
        return DataResponse.success(toHistoryResponses(logs));
    }

    @PatchMapping("/api/meal-log/{id}/status")
    public DataResponse<MealLogHistoryResponse> updateMealStatus(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId,
            @PathVariable String id,
            @RequestBody @Valid UpdateMealStatusRequest request) {
        MealLog updated = mealLogService.updateStatus(
                resolveUserId(userId, legacyUserId), id, request.getStatus(), request.getCustomNote());
        return DataResponse.success(toHistoryResponses(List.of(updated)).get(0));
    }
```

---

## Bước 7 — Build

Vì sửa cả module `common`:

```bash
# Tu thu muc goc repo BE
mvn -q -pl common install
mvn -q -pl nutrition-service spring-boot:run   # hoac build/run theo cach ban van dung
```

Cột `custom_note` được `ddl-auto:update` thêm tự động khi nutrition-service khởi động — kiểm tra log không có lỗi schema.

## Bước 8 — Smoke test (curl, qua health-data... thực ra qua nutrition-service cổng của nó)

Nutrition-service chạy sau gateway; test nhanh có thể gọi thẳng service (header `userId` trực tiếp). Lấy một `user_id` + một meal_log id có sẵn:

```sql
-- trong MySQL container, DB cua nutrition-service
SELECT id, user_id, meal_date, meal_type, status FROM meal_log ORDER BY meal_date DESC LIMIT 5;
```

```bash
# Range (thay cong <PORT> cua nutrition-service, vd 8083; va <USER_ID>)
curl "http://localhost:<PORT>/api/meal-log/history-range?from=2026-06-01&to=2026-06-30" \
  -H "userId: <USER_ID>"

# PATCH trang thai (thay <MEAL_LOG_ID>)
curl -X PATCH "http://localhost:<PORT>/api/meal-log/<MEAL_LOG_ID>/status" \
  -H "userId: <USER_ID>" -H "Content-Type: application/json" \
  -d '{"status":"FOLLOWED"}'

curl -X PATCH "http://localhost:<PORT>/api/meal-log/<MEAL_LOG_ID>/status" \
  -H "userId: <USER_ID>" -H "Content-Type: application/json" \
  -d '{"status":"CUSTOM","customNote":"bún bò Huế"}'
```

Kỳ vọng:
- Range trả `data` là mảng các bữa, mỗi món trong `dishes` **có `dishName` (không còn null)**.
- PATCH trả về bản ghi đã cập nhật với `status`/`customNote` mới. Sai id → 404 `MEALLOG-001`. id của user khác → 403.
- Gọi `/api/meal-log/confirm` lại trên một bữa đã FOLLOWED → status vẫn FOLLOWED (không bị reset).

## Contract cho FE (tóm tắt)

- `GET /api/meal-log/history-range?from=YYYY-MM-DD&to=YYYY-MM-DD` → `DataResponse<List<MealLogHistoryResponse>>`. Bắt buộc cả `from` và `to`.
- `PATCH /api/meal-log/{id}/status` body `{ "status": "FOLLOWED"|"CUSTOM"|"SKIPPED"|"SUGGESTED", "customNote"?: string }` → `DataResponse<MealLogHistoryResponse>`.
- `MealLogHistoryResponse` nay có thêm `customNote: string|null`; mỗi dish có `dishName: string`.
- Trạng thái lưu ở DB vẫn có thể là `MODIFIED` (cũ) — FE map `MODIFIED → CUSTOM`.
