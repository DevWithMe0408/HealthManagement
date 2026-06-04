# Hướng Dẫn Xây Dựng Tính Năng Người Dùng Xây Dựng Thực Đơn — Backend

> **Đọc trước khi code:** file này tự đủ context. Không cần đọc lịch sử chat khác.

---

## 1. Mục tiêu tính năng

Phase 1 đã có: trang `/nutrition-plan` đề xuất tổ hợp tốt nhất + swap đơn slot (đổi 1 món, giữ nguyên các món khác).

Phase 2 cần thêm vào **luồng swap hiện có** (KHÔNG tạo endpoint mới):
- **Search món theo tên** trong drawer (cùng `slotCode` với slot đang đổi).
- **Override khẩu phần (serving)** khi user pin món qua stepper.
- **Đa pin tích lũy**: user có thể ghim nhiều món trong cùng 1 bữa.
- **Cảnh báo carb-bomb**: khi bữa quá nhiều tinh bột → suggestion để user chủ động điều chỉnh.

**Mô hình UX đã chốt là mô hình C: hệ thống GIỮ NGUYÊN các món ở slot khác, CHỈ tối ưu lại serving. Engine KHÔNG tự đổi món** — chỉ gợi ý qua suggestion banner để user chủ động chọn.

---

## 2. Quyết định kiến trúc đã chốt

1. **Mở rộng endpoint `POST /api/recommendation/swap-dish` hiện có** thay vì tạo `/repick-meal` mới. Lý do: code `swapDish()` đã pin TẤT CẢ slot + chỉ optimize serving (đúng tinh thần mô hình C). Chỉ cần thêm `overrideGrams` vào `PinnedDish` và logic skip-enumerate cho slot có fixed serving.
2. **Tạo endpoint mới `GET /api/nutrition/dishes/search`** cho search box trong drawer.
3. **Cảnh báo carb-bomb** dùng threshold từ `system_config` (key `warn.carb_ratio_threshold`, default `0.70`), nhất quán với pattern config-driven hiện tại.
4. **Bỏ flag `outOfRange`** trong DishOptionResponse — đơn giản hóa, không render visual chip này.

---

## 3. Tóm tắt thay đổi (checklist)

**Database:**
- [ ] Insert 1 row vào `system_config`: `warn.carb_ratio_threshold = 0.70`

**Entity / DTO (5 files):**
- [ ] `PinnedDish` — thêm `overrideGrams: BigDecimal` (nullable)
- [ ] `DishSuggestionResponse` — thêm `unit: String` + `baseServingG: Integer`
- [ ] `DishOptionResponse` — thêm `unit: String` + `baseServingG: Integer` + đổi `expectedScore` thành nullable
- [ ] `SwapResultResponse` — thêm `warnings: List<WarningResponse>`
- [ ] DTO mới `WarningResponse` (record hoặc class với `type` + `message`)

**Repository:**
- [ ] `DishRepository` — thêm method `searchByName`

**Engine:**
- [ ] `BruteForceEngine.findBestServingCombo` — thêm param `fixedServingByIndex` (Map<Integer, BigDecimal>)
- [ ] Logic enumerate serving: skip enumerate cho index có fixed serving + skip `violatesWeightConstraint` cho slot fixed

**Service:**
- [ ] `RecommendationApiService.swapDish` — build map fixedServing từ `request.pinnedDishes`, truyền vào engine
- [ ] `RecommendationApiService` — compute carb-bomb warning sau khi có `bestCombo`
- [ ] `RecommendationApiService.findBestSwapSuggestion` — viết lại message tiếng Việt có dấu
- [ ] `RecommendationApiService.toDishResponse` + `toDishOptionResponse` — populate field `unit` + `baseServingG`
- [ ] Service mới `DishSearchService` cho logic search

**Controller:**
- [ ] Controller mới `DishController` với endpoint `GET /api/nutrition/dishes/search`

**Gateway:** không cần đụng — route `/api/nutrition/**` đã có sẵn trong `application.yml`.

---

## 4. Database — system_config

Chạy lệnh SQL sau (insert thủ công qua MySQL Workbench, vì pattern của project là deploy seed thủ công, không Flyway):

```sql
-- Ngưỡng cảnh báo carb-bomb (tỉ lệ kcal từ carb so với tổng kcal bữa)
INSERT INTO system_config (config_key, config_value, value_type, description, editable, created_at, updated_at, created_by, updated_by)
VALUES ('warn.carb_ratio_threshold', '0.70', 'DECIMAL',
        'Ngưỡng tỉ lệ carb (0.0–1.0) — nếu vượt sẽ hiển thị cảnh báo carb-bomb cho user',
        TRUE, NOW(), NOW(), 'SYSTEM', 'SYSTEM');
```

Verify sau insert: `SELECT * FROM system_config WHERE config_key LIKE 'warn.%';` → phải có 1 row.

---

## 5. DTO changes — chi tiết từng file

### 5.1 `dto/request/PinnedDish.java`

Thêm field `overrideGrams` (nullable). Khi present → engine pin cả dish lẫn serving; khi null → pin dish, engine tự enumerate serving.

```java
package org.example.nutritionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinnedDish {

    @NotBlank
    private String slotKey;

    @NotBlank
    private String dishId;

    /**
     * Khẩu phần ép buộc tính bằng gram. Null = engine tự tối ưu serving (mô hình swap cũ).
     * Có giá trị = user chủ động override qua stepper, engine giữ đúng serving này.
     */
    @Positive
    private BigDecimal overrideGrams;
}
```

### 5.2 `dto/response/DishSuggestionResponse.java`

Thêm `unit` + `baseServingG` để FE render `"{serving} {unit} ({grams}g)"`.

```java
// Thêm 2 field này vào class hiện tại (giữ nguyên các field cũ):
private String unit;            // 'bát', 'tô', 'đĩa', 'phần', ...
private Integer baseServingG;   // gram base của 1 đơn vị
```

### 5.3 `dto/response/DishOptionResponse.java`

Thêm `unit` + `baseServingG`. **Đổi `expectedScore` thành `BigDecimal` nullable** (hiện đã là BigDecimal, Java tự cho nullable — chỉ cần đảm bảo response không crash khi null).

```java
// Thêm 2 field này vào class hiện tại:
private String unit;
private Integer baseServingG;
```

**Lưu ý:** `expectedScore` sẽ null khi DTO này trả ra từ endpoint search (vì search không có ngữ cảnh đầy đủ để tính score). FE đã được hướng dẫn check null trước khi render.

### 5.4 `dto/response/SwapResultResponse.java`

Thêm field `warnings`:

```java
// Thêm field:
private List<WarningResponse> warnings;
```

Đảm bảo `@Builder` và `@Data` đã có sẵn — chỉ cần thêm 1 dòng.

### 5.5 DTO mới: `dto/response/WarningResponse.java`

```java
package org.example.nutritionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarningResponse {

    /**
     * Loại cảnh báo. Hiện có:
     * - CARB_BOMB: bữa quá nhiều tinh bột (carb > threshold)
     * Tương lai có thể thêm: LOW_PROTEIN, HIGH_FAT, ...
     */
    private String type;

    /**
     * Thông điệp tiếng Việt sẵn để FE render trực tiếp.
     */
    private String message;
}
```

---

## 6. Repository — DishRepository

File: `repository/catalog/DishRepository.java`

Thêm method search by name (case-insensitive, fold dấu tự động nếu collation là `utf8mb4_0900_ai_ci` hoặc `utf8mb4_unicode_ci`):

```java
@Query("SELECT d FROM Dish d " +
       "WHERE d.slotCode = :slotCode " +
       "AND d.isActive = TRUE " +
       "AND LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
       "ORDER BY d.name")
List<Dish> searchByName(@Param("slotCode") SlotCode slotCode,
                        @Param("name") String name);
```

Nhớ import `org.springframework.data.jpa.repository.Query` + `org.springframework.data.repository.query.Param`.

**Kiểm tra collation trước khi triển khai:** chạy `SHOW FULL COLUMNS FROM dishes WHERE Field='name';` xem `Collation`. Nếu là `*_ai_ci` → "com trang" sẽ match "Cơm trắng". Nếu là `*_bin` thì không match dấu, cần ALTER. Hiện schema dùng utf8mb4 mặc định nên 99% là `*_ai_ci` rồi.

---

## 7. Engine — BruteForceEngine

File: `service/recommendation/BruteForceEngine.java`

Method `findBestServingCombo` hiện tại:
```java
public MealCombination findBestServingCombo(
        List<DishCandidate> pinnedDishes,
        MealTarget mealTarget,
        LoadedConfigs configs,
        BigDecimal penalty)
```

**Thay đổi:** thêm param `fixedServingByIndex` để biết slot nào có override:

```java
public MealCombination findBestServingCombo(
        List<DishCandidate> pinnedDishes,
        Map<Integer, BigDecimal> fixedServingByIndex,  // NEW
        MealTarget mealTarget,
        LoadedConfigs configs,
        BigDecimal penalty)
```

Logic mới trong vòng enumerate: nếu `fixedServingByIndex.containsKey(dishIndex)`, dùng đúng serving đó (chỉ 1 nhánh), **bỏ qua `violatesWeightConstraint`** vì user chủ động override:

Sửa method `enumerateServings` để hỗ trợ trường hợp này. Cách đơn giản nhất là thêm tham số map đi xuyên qua các lần đệ quy:

```java
private void enumerateServings(
        List<DishCandidate> dishCombo,
        int dishIndex,
        List<DishWithServing> current,
        MealTarget mealTarget,
        LoadedConfigs configs,
        BigDecimal penalty,
        int topK,
        PriorityQueue<MealCombination> topCombinations,
        Map<Integer, BigDecimal> fixedServingByIndex) {  // NEW

    if (dishIndex == dishCombo.size()) {
        scoreServingCombination(current, mealTarget, configs, penalty, topK, topCombinations);
        return;
    }

    if (dishIndex > 0 && shouldPrune(current, dishCombo, dishIndex, mealTarget, configs)) {
        return;
    }

    DishCandidate candidate = dishCombo.get(dishIndex);

    // NEW BRANCH: slot có override → chỉ 1 nhánh, skip violatesWeightConstraint
    if (fixedServingByIndex != null && fixedServingByIndex.containsKey(dishIndex)) {
        BigDecimal fixedGrams = fixedServingByIndex.get(dishIndex);
        BigDecimal serving = fixedGrams.divide(
                candidate.getBaseServingG(), CALC_SCALE, RoundingMode.HALF_UP);
        DishWithServing dishWithServing = withServing(candidate, serving);
        // KHÔNG check violatesWeightConstraint — user chủ động override
        current.add(dishWithServing);
        enumerateServings(dishCombo, dishIndex + 1, current, mealTarget, configs,
                penalty, topK, topCombinations, fixedServingByIndex);
        current.remove(current.size() - 1);
        return;
    }

    // Logic enumerate cũ (giữ nguyên)
    for (BigDecimal serving : servingSteps(candidate.getSlotCode(), configs)) {
        DishWithServing dishWithServing = withServing(candidate, serving);
        if (violatesWeightConstraint(dishWithServing, configs)) {
            continue;
        }
        current.add(dishWithServing);
        enumerateServings(dishCombo, dishIndex + 1, current, mealTarget, configs,
                penalty, topK, topCombinations, fixedServingByIndex);
        current.remove(current.size() - 1);
    }
}
```

Update `findBestServingCombo` để gọi với map mới:

```java
public MealCombination findBestServingCombo(
        List<DishCandidate> pinnedDishes,
        Map<Integer, BigDecimal> fixedServingByIndex,
        MealTarget mealTarget,
        LoadedConfigs configs,
        BigDecimal penalty) {
    PriorityQueue<MealCombination> topCombinations = new PriorityQueue<>(
            Comparator.comparing(MealCombination::getFinalScore));
    enumerateServings(pinnedDishes, 0, new ArrayList<>(),
            mealTarget, configs, penalty, 1, topCombinations,
            fixedServingByIndex);
    return topCombinations.peek();
}
```

**Lưu ý:** method `findTopK` cũng gọi `enumerateServings` — phải truyền `null` cho `fixedServingByIndex` để giữ behavior cũ. Đây là backward-compatible change.

---

## 8. Service — RecommendationApiService

File: `service/recommendation/RecommendationApiService.java`

### 8.1 Sửa `swapDish` để build fixedServingByIndex

Khoảng dòng 130-175 (sau khi build `pinnedMap` và `pinnedCandidates`), thêm logic build map fixed serving:

```java
// Build map fixedServingByIndex: index trong pinnedCandidates -> overrideGrams
// Chỉ chứa slot có overrideGrams != null (user chủ động override khẩu phần)
Map<Integer, BigDecimal> fixedServingByIndex = new HashMap<>();
if (request.getPinnedDishes() != null) {
    for (PinnedDish pin : request.getPinnedDishes()) {
        if (pin.getOverrideGrams() != null) {
            // Tìm index trong currentDishes (cùng thứ tự với pinnedCandidates)
            for (int i = 0; i < currentDishes.size(); i++) {
                if (slotKeyOf(currentDishes, i).equals(pin.getSlotKey())) {
                    fixedServingByIndex.put(i, pin.getOverrideGrams());
                    break;
                }
            }
        }
    }
}
```

Đổi lời gọi `findBestServingCombo`:

```java
MealCombination bestCombo = bruteForceEngine.findBestServingCombo(
        pinnedCandidates,
        fixedServingByIndex,    // NEW
        mealTarget,
        configs,
        penalty);
```

### 8.2 Compute carb-bomb warning sau khi có `bestCombo`

Thêm logic sau khi check `bestCombo != null`, trước khi build `updatedMeal`:

```java
// Compute warnings (carb-bomb): tỉ lệ kcal từ carb so với tổng kcal
List<WarningResponse> warnings = new ArrayList<>();
BigDecimal carbRatioThreshold = configs.getDecimal("warn.carb_ratio_threshold");
BigDecimal carbKcal = bestCombo.getActual().getCarbG().multiply(KCAL_PER_G_CARB);
BigDecimal totalKcal = bestCombo.getActual().getKcal();
if (totalKcal.signum() > 0) {
    BigDecimal carbRatio = carbKcal.divide(totalKcal, CALC_SCALE, RoundingMode.HALF_UP);
    if (carbRatio.compareTo(carbRatioThreshold) > 0) {
        int pct = carbRatio.multiply(ONE_HUNDRED).setScale(0, RoundingMode.HALF_UP).intValue();
        warnings.add(WarningResponse.builder()
                .type("CARB_BOMB")
                .message("Bữa này khá nặng tinh bột (" + pct
                        + "% kcal từ carb). Cân nhắc giảm khẩu phần tinh bột hoặc "
                        + "đổi sang lựa chọn cân bằng hơn.")
                .build());
    }
}
```

Đảm bảo `ConfigLoaderService.getDecimal("warn.carb_ratio_threshold")` đọc được. Pattern này đã có sẵn (`configs.getDecimal("filter.serving_min")` ở nơi khác).

### 8.3 Build response với warnings

Sửa `SwapResultResponse.builder()`:

```java
return SwapResultResponse.builder()
        .updatedMeal(updatedMeal)
        .newFinalScore(updatedCombination.getFinalScore())
        .originalFinalScore(originalFinalScore)
        .scoreDropTriggered(triggered)
        .suggestion(triggered ? findBestSwapSuggestion(
                slotAlternatives,
                explicitPinnedSlots,
                updatedCombination.getFinalScore()) : null)
        .warnings(warnings)    // NEW
        .build();
```

### 8.4 Sửa `findBestSwapSuggestion` để message tiếng Việt có dấu

Sửa dòng `.message("Doi mon o [...] sang [...]")` thành:

```java
.message("Đổi món ở slot " + bestSlotKey + " sang " + bestAlternative.getCandidate().getDishName()
        + " để tăng điểm lên " + bestAlternative.getExpectedScore()
        .setScale(1, RoundingMode.HALF_UP) + ".")
```

### 8.5 Populate `unit` + `baseServingG` trong response mapping

Tìm method `toDishResponse(DishWithServing dish, ...)` (có nhiều overload). Thêm 2 field vào builder. Phải đọc từ `dish.getCandidate().getDish()` (entity Dish):

```java
private DishSuggestionResponse toDishResponse(DishWithServing dish, String slotKey, Set<String> favorites) {
    Dish dishEntity = dish.getCandidate().getDish();
    return DishSuggestionResponse.builder()
            .slotKey(slotKey)
            .dishId(dishEntity.getId())
            .dishName(dishEntity.getName())
            .slotCode(dishEntity.getSlotCode())
            .foodGroupCode(dishEntity.getFoodGroupCode())
            .servingMultiplier(dish.getServingMultiplier())
            .actualGrams(dish.getActualGrams())
            .dishKcal(dish.getKcal())
            .unit(dishEntity.getUnit())                  // NEW
            .baseServingG(dishEntity.getBaseServingG())  // NEW
            .favorite(favorites.contains(dishEntity.getId()))
            .build();
}
```

Tương tự cho `toDishOptionResponse(SlotAlternative alternative, ...)`:

```java
private DishOptionResponse toDishOptionResponse(SlotAlternative alt, Set<String> favorites) {
    Dish dishEntity = alt.getCandidate().getDish();
    return DishOptionResponse.builder()
            .dishId(dishEntity.getId())
            .dishName(dishEntity.getName())
            .slotCode(dishEntity.getSlotCode())
            .foodGroupCode(dishEntity.getFoodGroupCode())
            .expectedScore(alt.getExpectedScore())
            .expectedServing(alt.getExpectedServing())
            .expectedActualGrams(alt.getExpectedActualGrams())
            .unit(dishEntity.getUnit())                  // NEW
            .baseServingG(dishEntity.getBaseServingG())  // NEW
            .favorite(favorites.contains(dishEntity.getId()))
            .build();
}
```

Cũng phải đụng `RecommendationController.toDishResponse(MealLogDish dish)` (mapping cho meal-log history). Vì `MealLogDish` không có `unit` (chỉ lưu dishId + serving), phải query Dish từ repository hoặc cache. **Giải pháp đơn giản nhất:** thêm 2 column `unit` + `base_serving_g` vào bảng `meal_log_dishes` để snapshot tại thời điểm save → tránh n+1 query. NHƯNG đây là feature nhỏ, scope thesis có thể bỏ qua history mapping → để `unit = null, baseServingG = null` trong response của history (FE check null, fallback dùng grams).

**Khuyến nghị scope thesis:** chỉ cập nhật path `swap-dish` + `recommend-full-day`, để mapping history null. Note vào TODO post-thesis.

---

## 9. Service mới — DishSearchService

File mới: `service/recommendation/DishSearchService.java`

```java
package org.example.nutritionservice.service.recommendation;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.response.DishOptionResponse;
import org.example.nutritionservice.entity.catalog.Dish;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.repository.catalog.DishRepository;
import org.example.nutritionservice.repository.favorite.FavoriteDishRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishSearchService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int CALC_SCALE = 4;
    private static final int FINAL_SCALE = 2;

    // Lưới serving cho stepper FE: 0.5 / 0.75 / 1.0 / 1.25 / 1.5
    // Snap expectedServing về bội 0.5 gần nhất, min 0.5, max 1.5
    private static final BigDecimal SERVING_MIN = new BigDecimal("0.5");
    private static final BigDecimal SERVING_MAX = new BigDecimal("1.5");
    private static final BigDecimal SERVING_STEP = new BigDecimal("0.5");

    private final DishRepository dishRepository;
    private final FavoriteDishRepository favoriteDishRepository;

    /**
     * Tìm món theo tên trong cùng slotCode, compute expectedServing để fit slotKcalTarget.
     * expectedScore trả null vì search không có ngữ cảnh đầy đủ để chấm điểm.
     */
    public List<DishOptionResponse> searchDishes(
            String userId,
            SlotCode slotCode,
            String query,
            BigDecimal slotKcalTarget) {
        List<Dish> matches = dishRepository.searchByName(slotCode, query.trim());
        Set<String> favorites = favoriteDishRepository.findDishIdsByUserId(userId);

        return matches.stream()
                .map(dish -> toOption(dish, slotKcalTarget, favorites))
                .collect(Collectors.toList());
    }

    private DishOptionResponse toOption(Dish dish, BigDecimal slotKcalTarget, Set<String> favorites) {
        // Tính kcal cho 1 base serving
        BigDecimal baseServingRatio = BigDecimal.valueOf(dish.getBaseServingG())
                .divide(ONE_HUNDRED, CALC_SCALE, RoundingMode.HALF_UP);
        BigDecimal baseKcal = dish.getKcalPer100g().multiply(baseServingRatio);

        // expectedServing = slotKcalTarget / baseKcal, snap về bội 0.5, clamp [0.5, 1.5]
        BigDecimal rawServing = slotKcalTarget.divide(baseKcal, CALC_SCALE, RoundingMode.HALF_UP);
        BigDecimal snapped = snapToHalf(rawServing).max(SERVING_MIN).min(SERVING_MAX);
        BigDecimal expectedActualGrams = BigDecimal.valueOf(dish.getBaseServingG())
                .multiply(snapped).setScale(FINAL_SCALE, RoundingMode.HALF_UP);

        return DishOptionResponse.builder()
                .dishId(dish.getId())
                .dishName(dish.getName())
                .slotCode(dish.getSlotCode())
                .foodGroupCode(dish.getFoodGroupCode())
                .expectedScore(null)  // Search không tính score
                .expectedServing(snapped)
                .expectedActualGrams(expectedActualGrams)
                .unit(dish.getUnit())
                .baseServingG(dish.getBaseServingG())
                .favorite(favorites.contains(dish.getId()))
                .build();
    }

    /** Snap về bội 0.5 gần nhất. Ví dụ: 1.3 → 1.5, 1.2 → 1.0. */
    private BigDecimal snapToHalf(BigDecimal value) {
        BigDecimal multiplied = value.divide(SERVING_STEP, 0, RoundingMode.HALF_UP);
        return multiplied.multiply(SERVING_STEP);
    }
}
```

**Lưu ý:** kiểm tra `FavoriteDishRepository.findDishIdsByUserId` đã có chưa. Nếu chưa, query tự viết hoặc đọc từ pattern code `favoriteIds()` của `RecommendationApiService`.

---

## 10. Controller mới — DishController

File mới: `controller/DishController.java`

```java
package org.example.nutritionservice.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.response.DishOptionResponse;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.service.recommendation.DishSearchService;
import org.example.web.dto.response.DataResponse;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
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

    /**
     * Tìm món theo tên trong cùng slotCode.
     *
     * @param slotCode       slot code (CHINH/RAU/TINH_BOT/COMBO/BUA_PHU)
     * @param q              từ khóa tìm kiếm (min 1 ký tự)
     * @param slotKcalTarget kcal mục tiêu của slot (để compute expectedServing)
     */
    @GetMapping("/search")
    public DataResponse<List<DishOptionResponse>> searchDishes(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "userId", required = false) String legacyUserId,
            @RequestParam @NotNull SlotCode slotCode,
            @RequestParam @NotBlank String q,
            @RequestParam @NotNull @Positive BigDecimal slotKcalTarget) {

        if (q.trim().length() < 1) {
            throw new BusinessException(ErrorCode.CONFIG_VALIDATION_FAILED,
                    "Từ khóa tìm kiếm không được rỗng");
        }
        if (q.trim().length() > 50) {
            throw new BusinessException(ErrorCode.CONFIG_VALIDATION_FAILED,
                    "Từ khóa tìm kiếm tối đa 50 ký tự");
        }

        String resolvedUserId = userId != null ? userId : legacyUserId;
        return DataResponse.success(
                dishSearchService.searchDishes(resolvedUserId, slotCode, q, slotKcalTarget));
    }
}
```

---

## 11. Testing checklist

Sau khi code xong, verify thủ công qua Postman hoặc cURL:

### 11.1 Test endpoint search

```bash
curl -X GET "http://localhost:8080/api/nutrition/dishes/search?slotCode=TINH_BOT&q=cơm&slotKcalTarget=400" \
  -H "X-User-Id: <userId-thật>"
```

Expected: 200 OK, response data là array các món có "cơm" trong tên, mỗi item có `unit` + `baseServingG` + `expectedServing` (bội 0.5), `expectedScore = null`.

### 11.2 Test swap-dish với overrideGrams

Gọi `POST /api/recommendation/full-day` trước để lấy `currentPlan`. Sau đó:

```json
POST /api/recommendation/swap-dish
{
  "currentPlan": { ... full plan từ bước trên ... },
  "mealType": "TRUA",
  "swappedSlot": "TINH_BOT_0",
  "newDishId": "<id-cơm-trắng>",
  "pinnedDishes": [
    {
      "slotKey": "TINH_BOT_0",
      "dishId": "<id-cơm-trắng>",
      "overrideGrams": 300
    }
  ]
}
```

Expected: response `updatedMeal.topCombination.dishes` có cơm trắng ở slot TINH_BOT_0 với `actualGrams = 300`. Hai món còn lại giữ nguyên `dishId` (so với currentPlan), có thể đổi `actualGrams` (engine tự cân đối).

### 11.3 Test carb-bomb warning

Tăng override grams lên cao (vd `overrideGrams = 500` cho cơm). Response phải có:
```json
{
  "warnings": [
    { "type": "CARB_BOMB", "message": "Bữa này khá nặng tinh bột (XX% kcal từ carb)..." }
  ]
}
```

### 11.4 Test edge case bất khả thi

Đặt `overrideGrams = 2000` (siêu nhiều cơm) → engine không tìm được serving thỏa kcal ±25% → throw `BusinessException` với message "Không tìm được serving thỏa mãn sau khi đổi món". FE nhận 4xx, hiển thị warning mode B.

---

## 12. Lưu ý quan trọng

**(1) Backward compatibility:** Tất cả thay đổi đều giữ tương thích ngược. Request swap-dish cũ (không có `overrideGrams` và `pinnedDishes`) vẫn chạy đúng — `fixedServingByIndex` rỗng → engine enumerate serving như cũ.

**(2) Comment + commit message tiếng Việt:** Theo convention project. Ví dụ commit: `feat(nutrition): them search mon va override khau phan cho luong swap` / `feat(nutrition): canh bao carb-bomb khi user override khau phan tinh bot`.

**(3) Performance:** `findBestServingCombo` với 3 slot, mỗi slot 5 serving step = 125 tổ hợp. Với override 2/3 slot → còn 5 tổ hợp. Rất nhanh, không cần thêm cache.

**(4) Don't refactor:** không đụng `findTopK`, `computeSlotAlternatives`, `PenaltyService`, `ScoringService`. Chúng đã verify đúng cho mô hình C (penalty per-dish per-slot, lớp 2 chỉ áp slot CHINH).

**(5) Không đụng MealLogDish history mapping:** scope thesis. `unit` + `baseServingG` trong response history có thể null. FE fallback dùng `grams`.

**(6) Validate cuối:** chạy `mvn test` (nếu có test) + smoke test toàn bộ luồng FE-BE trước khi commit lớn.

---

## 13. Câu hỏi để tự kiểm tra trước khi pull request

- [ ] `system_config.warn.carb_ratio_threshold` đã insert chưa? `SELECT` ra phải có row.
- [ ] `PinnedDish.overrideGrams` nullable, không break request cũ?
- [ ] `findBestServingCombo` 2 overload (có/không map) hoặc chỉ 1 overload + `null`?
- [ ] Khi `overrideGrams != null`, engine có skip `violatesWeightConstraint` không?
- [ ] Search endpoint trả `expectedScore = null` đúng không?
- [ ] Carb-bomb warning trigger đúng ngưỡng từ config, không phải hardcode?
- [ ] Tất cả comment Java mới đều tiếng Việt?
