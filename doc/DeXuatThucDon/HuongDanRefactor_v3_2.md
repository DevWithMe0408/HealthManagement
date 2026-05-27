# HƯỚNG DẪN REFACTOR FEATURE "ĐỀ XUẤT THỰC ĐƠN" — v3.2

> Tài liệu này dành cho **agent** thực hiện refactor feature theo spec v3.2.
>
> Agent: ĐỌC TOÀN BỘ tài liệu này TRƯỚC khi viết code.
>
> Tham chiếu bắt buộc:
> - `doc/nghiep_vu_de_xuat_thuc_don_v3.md` (v3.2) — đặc biệt **§0 Update Notes**
> - `HuongDanXayDungFeatureDeXuatThucDon.md` — quy ước project (đã có)
> - Code hiện tại trên branch `feature-DeXuatThucDon` commit `c8bd20a`

---

## 0. CONTEXT VÀ NGUYÊN TẮC

### 0.1. Tình trạng hiện tại

Branch `feature-DeXuatThucDon` đã có:
- ✅ Entity catalog (Dish, Ingredient, DishIngredient) + config (6 bảng) + meal_log + favorite_dishes
- ✅ 8 service core trong `service/recommendation/`
- ✅ 8 endpoint user-facing đã test OK qua Postman
- ❌ **Vấn đề 1:** Alternatives bị trùng món chính (UX kém)
- ❌ **Vấn đề 2:** Swap dish khiến score sụt 80 điểm (97→16) do không re-optimize serving
- ❌ **Vấn đề 3:** `nMain=2` throw `RecommendationTooComplexException` (927M tổ hợp > 50M cap)

### 0.2. Mục tiêu refactor

Thực hiện 3 nhóm thay đổi (theo thứ tự ưu tiên):

| Phần | Mục tiêu | Effort | Blocker? |
|---|---|---|---|
| **A** | Performance — Filter chặt + Early prune + Giảm serving steps | ~50 dòng | ✅ (nMain=2 chưa chạy được) |
| **B** | Slot Alternatives + Diversity — Refactor response structure | ~150 dòng | API breaking |
| **C** | Swap logic mới — Pin + serving-only optimize + suggestion mới | ~120 dòng | API breaking |

### 0.3. Quy tắc làm việc (KHÔNG BỎ QUA)

- **KHÔNG đổi schema 9 bảng config/catalog/meal_log/favorite_dishes** đã có. Nếu cần thêm column → BÁO USER trước.
- **KHÔNG đổi 7 service core hiện tại** trừ những file được liệt kê rõ trong tài liệu này.
- **CÓ THỂ delete/đổi tên DTO** vì API đang breaking (FE chưa build).
- **KHÔNG over-engineer**: không thêm Redis cache, không thêm CQRS, không tách service thêm.
- **Mỗi step phải compile được** trước khi chuyển step tiếp theo. Chạy `./mvnw clean compile -pl nutrition-service`.
- Sau mỗi PHẦN (A/B/C), agent BÁO USER review, KHÔNG tự nhảy phần.

### 0.4. Quy ước code (giữ nguyên từ tài liệu cũ)

- **Tên class/method/biến:** tiếng Anh, camelCase. **DB column/table:** snake_case.
- **Code comment + commit message:** tiếng Việt.
- **Comment tại mỗi hàm:** ngắn gọn bằng tiếng Việt để dễ hiểu, chú ý không xóa các comment bằng tiếng việt của tôi.
- **BigDecimal scale:** intermediate=4, final=2, dùng `RoundingMode.HALF_UP`.
- **Logging:** `@Slf4j`, debug cho recommendation flow. KHÔNG `log.info()` mỗi tổ hợp.
- **Exception:** dùng `org.example.web.exception.BusinessException` (đã có).

---

## PHẦN A — PERFORMANCE OPTIMIZATION

> 3 step. Hoàn thành cả 3 rồi chạy lại test request `nMain=2` để verify không còn throw 422.

### STEP A1: Filter dish chặt hơn khi nMain ≥ 2

#### A1.1. Files thay đổi

```
nutrition-service/src/main/java/org/example/nutritionservice/
├── service/recommendation/DishFilterService.java   (UPDATE signature + logic)
└── service/recommendation/RecommendationApiService.java  (UPDATE call sites)
```

#### A1.2. Đổi signature

**Hiện tại** (giả sử — agent xem file thực tế):
```java
public List<DishCandidate> filterCandidatesForSlot(
    SlotCode slot,
    BigDecimal slotKcalTarget,
    LoadedConfigs configs,
    List<Dish> allDishesInSlot
);
```

**Mới — thêm param `dishesNeededInSlot`:**
```java
public List<DishCandidate> filterCandidatesForSlot(
    SlotCode slot,
    BigDecimal slotKcalTarget,
    int dishesNeededInSlot,         // <-- MỚI: nMain (cho slot CHINH), nRau, nCarb
    LoadedConfigs configs,
    List<Dish> allDishesInSlot
);
```

#### A1.3. Logic mới

```java
public List<DishCandidate> filterCandidatesForSlot(
    SlotCode slot,
    BigDecimal slotKcalTarget,
    int dishesNeededInSlot,
    LoadedConfigs configs,
    List<Dish> allDishesInSlot) {

    // Tinh target cho moi dish trong slot (chia deu)
    BigDecimal targetPerDish = slotKcalTarget.divide(
        BigDecimal.valueOf(dishesNeededInSlot),
        4, RoundingMode.HALF_UP
    );

    // Bien noi rong: [×0.5, ×1.5] thay vi [×0.85, ×1.15]
    // Ly do: cho phep cap 2 mon lech nhau (vd 0.5x + 1.5x = 2x target)
    BigDecimal lowerBound = targetPerDish.multiply(new BigDecimal("0.5"));
    BigDecimal upperBound = targetPerDish.multiply(new BigDecimal("1.5"));

    BigDecimal servingMin = configs.getDecimal("filter.serving_min");
    BigDecimal servingMax = configs.getDecimal("filter.serving_max");

    return allDishesInSlot.stream()
        .filter(dish -> {
            BigDecimal baseKcal = dish.getKcalPer100g()
                .multiply(BigDecimal.valueOf(dish.getBaseServingG()))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            BigDecimal kcalAtMin = baseKcal.multiply(servingMin);
            BigDecimal kcalAtMax = baseKcal.multiply(servingMax);

            // Pass neu ton tai serving nao do trong [min, max] cho kcal vao [lower, upper]
            return kcalAtMin.compareTo(upperBound) <= 0
                && kcalAtMax.compareTo(lowerBound) >= 0;
        })
        .map(dish -> buildCandidate(dish))
        .toList();
}
```

#### A1.4. Update call sites

Trong `RecommendationApiService` (hoặc nơi gọi `filterCandidatesForSlot`):

```java
// Truoc:
filterCandidatesForSlot(SlotCode.CHINH, slotKcalTargets.get(SlotCode.CHINH), configs, allChinhDishes)

// Sau:
filterCandidatesForSlot(
    SlotCode.CHINH,
    slotKcalTargets.get(SlotCode.CHINH),
    perMealConfig.countForSlot(SlotCode.CHINH),  // nMain
    configs,
    allChinhDishes
)
```

Apply tương tự cho RAU, TINH_BOT, COMBO.

#### A1.5. Acceptance

- [ ] Compile OK
- [ ] Test request `nMain=2, nRau=1, nCarb=1` → log debug print số ứng viên CHINH giảm so với trước
- [ ] Test request `nMain=1` cũ vẫn cho kết quả tương tự v3.1 (không regression)

---

### STEP A2: Early prune trong BruteForceEngine

#### A2.1. Files thay đổi

```
nutrition-service/src/main/java/org/example/nutritionservice/
└── service/recommendation/BruteForceEngine.java   (UPDATE method enumerateServings)
```

#### A2.2. Logic mới

Hiện tại `enumerateServings` recursion check kcal deviation ở CUỐI (sau khi đã add đủ N dish). Refactor để check sau mỗi level dish:

```java
private void enumerateServings(
    List<DishCandidate> dishCombo,
    int dishIndex,
    List<DishWithServing> current,
    MealTarget mealTarget,
    LoadedConfigs configs,
    BigDecimal penalty,
    int topK,
    PriorityQueue<MealCombination> topCombinations) {

    if (dishIndex == dishCombo.size()) {
        scoreServingCombination(current, mealTarget, configs, penalty, topK, topCombinations);
        return;
    }

    // === EARLY PRUNE ===
    if (dishIndex > 0 && shouldPrune(current, dishCombo, dishIndex, mealTarget, configs)) {
        return;
    }

    DishCandidate candidate = dishCombo.get(dishIndex);
    for (BigDecimal serving : servingSteps(candidate.getSlotCode(), configs)) {
        DishWithServing dishWithServing = withServing(candidate, serving);
        if (violatesWeightConstraint(dishWithServing, configs)) {
            continue;
        }
        current.add(dishWithServing);
        enumerateServings(
            dishCombo, dishIndex + 1, current,
            mealTarget, configs, penalty, topK, topCombinations
        );
        current.remove(current.size() - 1);
    }
}

private boolean shouldPrune(
    List<DishWithServing> current,
    List<DishCandidate> dishCombo,
    int dishIndex,
    MealTarget mealTarget,
    LoadedConfigs configs) {

    // Kcal cua cac mon da chon
    BigDecimal kcalSoFar = current.stream()
        .map(DishWithServing::getKcal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Tinh min/max kcal cua cac mon CHUA chon
    BigDecimal servingMin = configs.getDecimal("filter.serving_min");
    BigDecimal servingMax = configs.getDecimal("filter.serving_max");
    BigDecimal kcalRemainingMin = BigDecimal.ZERO;
    BigDecimal kcalRemainingMax = BigDecimal.ZERO;
    for (int i = dishIndex; i < dishCombo.size(); i++) {
        BigDecimal baseKcal = dishCombo.get(i).getBaseKcal();
        kcalRemainingMin = kcalRemainingMin.add(baseKcal.multiply(servingMin));
        kcalRemainingMax = kcalRemainingMax.add(baseKcal.multiply(servingMax));
    }

    BigDecimal mealKcal = mealTarget.getMealKcal();
    BigDecimal lowerBound = mealKcal.multiply(new BigDecimal("0.75"));
    BigDecimal upperBound = mealKcal.multiply(new BigDecimal("1.25"));

    // Prune: chac chan thieu kcal (max possible < lower bound)
    if (kcalSoFar.add(kcalRemainingMax).compareTo(lowerBound) < 0) {
        return true;
    }
    // Prune: chac chan thua kcal (min possible > upper bound)
    if (kcalSoFar.add(kcalRemainingMin).compareTo(upperBound) > 0) {
        return true;
    }
    return false;
}
```

#### A2.3. Acceptance

- [ ] Compile OK
- [ ] Test `nMain=2`: response trả về (không còn 422). Latency có thể tăng nhẹ nhưng < 10s
- [ ] Test `nMain=1` không regression (score giống v3.1)
- [ ] Bật `logging.level.org.example.nutritionservice.service.recommendation: DEBUG` và verify thấy prune happen (vd log "Pruned at dishIndex=2")

---

### STEP A3: Giảm serving steps qua config

#### A3.1. Update config qua SQL

Chạy SQL update (NẾU DB hiện tại đã có giá trị cũ):

```sql
USE nutrition_db;

UPDATE system_config
SET config_value = '[0.5, 0.75, 1.0, 1.5, 2.0]',
    updated_at = NOW(),
    updated_by = 'refactor_v3_2'
WHERE config_key = 'filter.serving_steps';

-- Verify
SELECT config_key, config_value FROM system_config
WHERE config_key IN ('filter.serving_steps', 'filter.combo_serving_steps');
```

#### A3.2. Update default trong `data.sql`

Mở `nutrition-service/src/main/resources/db/data.sql`, tìm dòng INSERT cho `filter.serving_steps`, đổi giá trị mặc định thành `'[0.5, 0.75, 1.0, 1.5, 2.0]'`. Lý do: lần seed sau cho DB mới sẽ dùng giá trị này.

`filter.combo_serving_steps` giữ nguyên `[0.75, 1.0, 1.25, 1.5]` (4 mức cho COMBO).

#### A3.3. Acceptance

- [ ] DB hiện tại đã update giá trị mới (verify qua query SELECT)
- [ ] `data.sql` đã update default mới
- [ ] Test `nMain=2`: estimated work < 50M (xem log debug)

---

### CHECKPOINT SAU PHẦN A

**Báo USER với report:**

1. Test request `{tdee: 2000, goal: GIAM, nMain=2, nRau=1, nCarb=1}` → trả về 200 OK, không còn 422
2. Latency response: bao nhiêu ms (chấp nhận < 10s cho nMain=2)
3. Số ứng viên CHINH thực tế sau filter (log debug)
4. Có request `nMain=1` cũ vẫn cho kết quả tương tự v3.1 không?

User confirm OK → chuyển Phần B.

---

## PHẦN B — SLOT ALTERNATIVES + DIVERSITY

> 4 step. Đây là refactor lớn nhất, ảnh hưởng response structure. FE chưa build nên free hand đổi.

### STEP B1: Tạo POJO + DTO mới

#### B1.1. Files cần tạo MỚI

```
nutrition-service/src/main/java/org/example/nutritionservice/
├── domain/recommendation/
│   └── SlotAlternative.java       (POJO internal — không serialize)
└── dto/response/
    └── DishOptionResponse.java    (DTO trả về cho FE)
```

#### B1.2. `SlotAlternative.java`

```java
package org.example.nutritionservice.domain.recommendation;

import lombok.*;
import org.example.nutritionservice.entity.catalog.FoodGroup;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotAlternative {
    private DishCandidate candidate;
    private BigDecimal expectedScore;
    private BigDecimal expectedServing;
    private BigDecimal expectedActualGrams;

    public FoodGroup getFoodGroupCode() {
        return candidate.getFoodGroupCode();
    }
    public String getDishId() {
        return candidate.getDishId();
    }
}
```

#### B1.3. `DishOptionResponse.java`

```java
package org.example.nutritionservice.dto.response;

import lombok.*;
import org.example.nutritionservice.entity.catalog.FoodGroup;
import org.example.nutritionservice.entity.catalog.SlotCode;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishOptionResponse {
    private String dishId;
    private String dishName;
    private SlotCode slotCode;
    private FoodGroup foodGroupCode;
    private BigDecimal expectedScore;
    private BigDecimal expectedServing;
    private BigDecimal expectedActualGrams;
    private boolean favorite;
}
```

#### B1.4. Cập nhật `DishSuggestionResponse.java`

Thêm field `slotKey`:

```java
public class DishSuggestionResponse {
    private String slotKey;        // <-- MỚI: "CHINH_0", "RAU_0"...
    private String dishId;
    private String dishName;
    private SlotCode slotCode;
    private FoodGroup foodGroupCode;
    private BigDecimal servingMultiplier;
    private BigDecimal actualGrams;
    private BigDecimal dishKcal;
    private boolean favorite;
}
```

#### B1.5. Cập nhật `MealSuggestionResponse.java`

**Bỏ** field `alternativeCombinations`. **Thêm** `slotAlternatives`:

```java
public class MealSuggestionResponse {
    private MealType mealType;
    private BigDecimal mealKcalTarget;
    private MealCombinationResponse topCombination;

    // CŨ — XÓA: private List<MealCombinationResponse> alternativeCombinations;

    // MỚI:
    private Map<String, List<DishOptionResponse>> slotAlternatives;
    // Key: slotKey ("CHINH_0", "RAU_0",...)
    // Value: list alternatives cho slot đó
}
```

#### B1.6. Acceptance

- [ ] 2 file mới + 2 file cũ update compile OK
- [ ] Chưa cần wire vào logic, chỉ define data structure

---

### STEP B2: Refactor `BruteForceEngine` với group-by main key

#### B2.1. Thêm method utility extract main key

Trong `BruteForceEngine`:

```java
/**
 * Trich xuat "main key" tu combo de phuc vu diversity grouping (xem §0.2).
 * - COMBO: dish_id cua slot COMBO
 * - nMain = 1: dish_id cua slot CHINH duy nhat
 * - nMain >= 2: join sorted dish_id cua tat ca slot CHINH
 */
private String extractMainKey(MealCombination combo) {
    List<String> mainDishIds = combo.getDishes().stream()
        .filter(d -> d.getCandidate().getSlotCode() == SlotCode.CHINH
                  || d.getCandidate().getSlotCode() == SlotCode.COMBO)
        .map(d -> d.getCandidate().getDishId())
        .sorted()
        .toList();
    return String.join(",", mainDishIds);
}
```

#### B2.2. Update `findTopK` để apply diversity

Hiện tại logic cuối:
```java
return topCombinations.stream()
    .sorted(Comparator.comparing(MealCombination::getFinalScore).reversed())
    .toList();
```

**Đổi thành:**

```java
// Sort theo finalScore desc
List<MealCombination> sorted = topCombinations.stream()
    .sorted(Comparator.comparing(MealCombination::getFinalScore).reversed())
    .toList();

// Apply diversity: moi main key chi lay 1 dai dien
Set<String> seenKeys = new HashSet<>();
List<MealCombination> diverseResults = new ArrayList<>();
for (MealCombination combo : sorted) {
    String mainKey = extractMainKey(combo);
    if (seenKeys.contains(mainKey)) continue;
    seenKeys.add(mainKey);
    diverseResults.add(combo);
    if (diverseResults.size() >= topK) break;
}
return diverseResults;
```

**Lưu ý:** Vì sau diversity có thể chỉ còn ít hơn topK, **cần tăng `topK` ban đầu của PriorityQueue** lên cao hơn (ví dụ topK × 3) để có nhiều combination chờ pick. Nếu PriorityQueue chỉ giữ topK = 10 thì sau diversity có thể chỉ còn 2-3.

Đổi line setup PriorityQueue:
```java
int internalK = topK * 5;  // Buffer cho diversity filter
PriorityQueue<MealCombination> topCombinations = new PriorityQueue<>(
    Comparator.comparing(MealCombination::getFinalScore)
);
// ... khi check pop:
if (topCombinations.size() < internalK) { ... }
else if (...) { ... }
```

#### B2.3. Cấm cặp same food_group cho CHINH (apply §0.7)

Trong method `buildDishCombinations` của `BruteForceEngine`:

```java
// Sau khi gen Cartesian product cua cap CHINH, filter cac cap same food_group
boolean forbidSameGroup = "true".equalsIgnoreCase(
    configs.getSystemConfigs().getOrDefault("filter.forbid_same_food_group_in_main", "true")
);

if (forbidSameGroup) {
    allCombinations = allCombinations.stream()
        .filter(combo -> {
            List<FoodGroup> mainGroups = combo.stream()
                .filter(d -> d.getSlotCode() == SlotCode.CHINH)
                .map(DishCandidate::getFoodGroupCode)
                .toList();
            // Khong duplicate
            return mainGroups.size() == Set.copyOf(mainGroups).size();
        })
        .toList();
}
```

#### B2.4. Acceptance

- [ ] Test `nMain=1, recommend full day` → 10 alternatives KHÁC món chính của top
- [ ] Test `nMain=2` → các cặp món chính KHÁC nhau giữa các tổ hợp, KHÔNG có cặp [Gà, Gà]
- [ ] Verify config flag: set `filter.forbid_same_food_group_in_main = "false"` qua admin API → cặp same group quay lại

---

### STEP B3: Compute slot alternatives

#### B3.1. Tạo service mới hoặc method trong RecommendationApiService

**Khuyến nghị:** Tạo method `computeSlotAlternatives` trong `BruteForceEngine` hoặc `RecommendationApiService`. Mình chọn `BruteForceEngine` vì cần access logic scoring.

```java
/**
 * Compute alternatives cho moi slot trong topCombination.
 * Voi moi slot S, tim ra cac mon khac (cung slot_code) co the thay the,
 * tinh expected_score khi thay the (giu nguyen cac slot khac, optimize chi serving cua mon moi).
 *
 * Apply diversity:
 * - Slot CHINH: cac options phai khac food_group VOI NHAU (config-controlled)
 * - Slot khac (RAU, TINH_BOT, COMBO): chi can khac dish_id
 */
public Map<String, List<SlotAlternative>> computeSlotAlternatives(
    MealCombination topCombination,
    Map<SlotCode, List<DishCandidate>> candidatesPerSlot,
    MealTarget mealTarget,
    LoadedConfigs configs) {

    Map<String, List<SlotAlternative>> result = new LinkedHashMap<>();
    int maxPerSlot = configs.getInt("display.slot_alternatives_count");  // default 10
    boolean forbidSameGroup = "true".equalsIgnoreCase(
        configs.getSystemConfigs().getOrDefault("filter.forbid_same_food_group_in_main", "true")
    );

    // Index hien tai cua moi slot (CHINH_0, CHINH_1, RAU_0, ...)
    Map<SlotCode, Integer> slotCounter = new EnumMap<>(SlotCode.class);

    for (int dishIdx = 0; dishIdx < topCombination.getDishes().size(); dishIdx++) {
        DishWithServing currentDish = topCombination.getDishes().get(dishIdx);
        SlotCode slot = currentDish.getCandidate().getSlotCode();
        int counter = slotCounter.getOrDefault(slot, 0);
        String slotKey = slot.name() + "_" + counter;
        slotCounter.put(slot, counter + 1);

        // Lay candidates KHAC mon hien tai va KHAC cac slot CHINH khac trong combo (neu nMain>=2)
        Set<String> excludeDishIds = topCombination.getDishes().stream()
            .filter(d -> d.getCandidate().getSlotCode() == slot)
            .map(d -> d.getCandidate().getDishId())
            .collect(Collectors.toSet());

        List<DishCandidate> candidates = candidatesPerSlot.getOrDefault(slot, List.of()).stream()
            .filter(c -> !excludeDishIds.contains(c.getDishId()))
            .toList();

        // Tinh expected_score cho moi candidate
        List<SlotAlternative> alternatives = new ArrayList<>();
        for (DishCandidate candidate : candidates) {
            SlotAlternative alt = computeOneAlternative(
                topCombination, dishIdx, candidate, mealTarget, configs
            );
            if (alt != null) {  // Co the null neu khong tim duoc serving thoa weight constraint
                alternatives.add(alt);
            }
        }

        // Sort desc by expected_score
        alternatives.sort(Comparator.comparing(SlotAlternative::getExpectedScore).reversed());

        // Apply diversity filter cho slot CHINH (khac food_group)
        List<SlotAlternative> diverseAlts;
        if (slot == SlotCode.CHINH && forbidSameGroup) {
            Set<FoodGroup> seenGroups = new HashSet<>();
            // Loai cac group da co trong combo (vd top dang co GIA_CAM o CHINH_0)
            for (DishWithServing d : topCombination.getDishes()) {
                if (d.getCandidate().getSlotCode() == SlotCode.CHINH) {
                    seenGroups.add(d.getCandidate().getFoodGroupCode());
                }
            }
            diverseAlts = new ArrayList<>();
            for (SlotAlternative alt : alternatives) {
                if (seenGroups.contains(alt.getFoodGroupCode())) continue;
                seenGroups.add(alt.getFoodGroupCode());
                diverseAlts.add(alt);
                if (diverseAlts.size() >= maxPerSlot) break;
            }
        } else {
            // Slot khac: khong diversity food_group, chi take top N
            diverseAlts = alternatives.stream().limit(maxPerSlot).toList();
        }

        result.put(slotKey, diverseAlts);
    }

    return result;
}

private SlotAlternative computeOneAlternative(
    MealCombination topCombination,
    int swappedDishIdx,
    DishCandidate newCandidate,
    MealTarget mealTarget,
    LoadedConfigs configs) {

    BigDecimal bestScore = BigDecimal.valueOf(-1);
    DishWithServing bestServing = null;

    // Thu cac muc serving cho new candidate
    for (BigDecimal serving : servingSteps(newCandidate.getSlotCode(), configs)) {
        DishWithServing newDishWithServing = withServing(newCandidate, serving);
        if (violatesWeightConstraint(newDishWithServing, configs)) continue;

        // Tao combo moi: thay 1 slot bang new candidate, giu nguyen cac slot khac
        List<DishWithServing> testCombo = new ArrayList<>(topCombination.getDishes());
        testCombo.set(swappedDishIdx, newDishWithServing);

        // Compute kcal deviation
        MealActual actual = toActual(testCombo);
        BigDecimal kcalDev = actual.getKcal().subtract(mealTarget.getMealKcal()).abs()
            .divide(mealTarget.getMealKcal(), 4, RoundingMode.HALF_UP);
        if (kcalDev.compareTo(new BigDecimal("0.25")) > 0) continue;

        BigDecimal macroScore = scoringService.computeMacroScore(
            actual, mealTarget.getMacroTarget(), configs.getGoalConfig(), configs
        );
        // Reuse penalty cua topCombination (penalty thay doi rat it khi swap 1 mon)
        BigDecimal score = macroScore.subtract(topCombination.getPenalty()).max(BigDecimal.ZERO);

        if (score.compareTo(bestScore) > 0) {
            bestScore = score;
            bestServing = newDishWithServing;
        }
    }

    if (bestServing == null) return null;
    return SlotAlternative.builder()
        .candidate(newCandidate)
        .expectedScore(bestScore.setScale(2, RoundingMode.HALF_UP))
        .expectedServing(bestServing.getServingMultiplier())
        .expectedActualGrams(bestServing.getActualGrams())
        .build();
}
```

**Lưu ý quan trọng:**
- Reuse penalty của topCombination, không re-compute penalty cho mỗi alternative (vì penalty phụ thuộc combo, không phụ thuộc serving).
- Vẫn check weight constraint + kcal deviation 25%.
- Nếu không có alternative nào pass → return empty list (frontend hiển thị "Không có gợi ý thay thế").

#### B3.2. Wire vào `RecommendationApiService.toMealResponse`

Tìm method tương tự `toMealResponse(RecommendedMeal meal, ...)`. Update để gen `slotAlternatives` thay vì `alternativeCombinations`:

```java
private MealSuggestionResponse toMealResponse(
    RecommendedMeal meal,
    Map<SlotCode, List<DishCandidate>> candidatesPerSlot,  // <-- thêm param này
    LoadedConfigs configs,                                  // <-- thêm
    Set<String> favorites) {

    if (meal.getCombinations().isEmpty()) {
        return MealSuggestionResponse.builder()
            .mealType(meal.getMealTarget().getMealType())
            .mealKcalTarget(meal.getMealTarget().getMealKcal())
            .topCombination(null)
            .slotAlternatives(Map.of())
            .build();
    }

    MealCombination topCombo = meal.getCombinations().get(0);
    MealCombinationResponse topResponse = toCombinationResponse(topCombo, favorites);

    Map<String, List<SlotAlternative>> alternatives = bruteForceEngine.computeSlotAlternatives(
        topCombo, candidatesPerSlot, meal.getMealTarget(), configs
    );

    Map<String, List<DishOptionResponse>> alternativesResponse = alternatives.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> e.getValue().stream().map(alt -> toDishOptionResponse(alt, favorites)).toList(),
            (a, b) -> a,
            LinkedHashMap::new
        ));

    return MealSuggestionResponse.builder()
        .mealType(meal.getMealTarget().getMealType())
        .mealKcalTarget(meal.getMealTarget().getMealKcal())
        .topCombination(topResponse)
        .slotAlternatives(alternativesResponse)
        .build();
}

private DishOptionResponse toDishOptionResponse(SlotAlternative alt, Set<String> favorites) {
    return DishOptionResponse.builder()
        .dishId(alt.getDishId())
        .dishName(alt.getCandidate().getDishName())
        .slotCode(alt.getCandidate().getSlotCode())
        .foodGroupCode(alt.getFoodGroupCode())
        .expectedScore(alt.getExpectedScore())
        .expectedServing(alt.getExpectedServing())
        .expectedActualGrams(alt.getExpectedActualGrams())
        .favorite(favorites.contains(alt.getDishId()))
        .build();
}
```

#### B3.3. Update `toCombinationResponse` để thêm `slotKey` cho mỗi dish

```java
private MealCombinationResponse toCombinationResponse(MealCombination combo, Set<String> favorites) {
    // Tinh slotKey cho moi dish
    Map<SlotCode, Integer> slotCounter = new EnumMap<>(SlotCode.class);
    List<DishSuggestionResponse> dishes = new ArrayList<>();
    for (DishWithServing dish : combo.getDishes()) {
        SlotCode slot = dish.getCandidate().getSlotCode();
        int idx = slotCounter.getOrDefault(slot, 0);
        String slotKey = slot.name() + "_" + idx;
        slotCounter.put(slot, idx + 1);

        dishes.add(DishSuggestionResponse.builder()
            .slotKey(slotKey)
            .dishId(dish.getCandidate().getDishId())
            // ... cac field khac giu nguyen
            .build());
    }

    return MealCombinationResponse.builder()
        // ... existing fields
        .dishes(dishes)
        .build();
}
```

#### B3.4. Acceptance

- [ ] Test request full-day → response có `slotAlternatives`, KHÔNG còn `alternativeCombinations`
- [ ] Mỗi slot có ≤10 options (config `display.slot_alternatives_count`)
- [ ] Slot CHINH alternatives có food_group ĐA DẠNG (không 2 món cùng food_group, không cùng food_group với top)
- [ ] Mỗi DishOption có `expectedScore` > 0 và `expectedServing` ∈ [0.5, 2.0]
- [ ] Mỗi dish trong topCombination có `slotKey` đúng format (`CHINH_0`, `RAU_0`, ...)

---

### STEP B4: Update config seed + admin endpoint

#### B4.1. Update `data.sql`

Thêm 2 config mới:

```sql
INSERT INTO system_config (config_key, config_value, value_type, description, updated_at, updated_by)
VALUES
('display.slot_alternatives_count', '10', 'INTEGER', 'So luong alternatives moi slot trong response', NOW(), 'system'),
('filter.forbid_same_food_group_in_main', 'true', 'BOOLEAN', 'Cam cap 2 mon chinh cung food_group khi nMain>=2', NOW(), 'system')
ON DUPLICATE KEY UPDATE config_value = config_value;  -- Khong overwrite neu da co
```

#### B4.2. Verify qua MySQL

```sql
SELECT config_key, config_value, value_type 
FROM system_config
WHERE config_key IN ('display.slot_alternatives_count', 'filter.forbid_same_food_group_in_main',
                     'display.top_k', 'filter.serving_steps');
```

Expected: 4 row hợp lệ.

#### B4.3. Update Admin Config endpoint (nếu cần)

Admin endpoint `/api/admin/configs/system` hiện tại GET/PUT toàn bộ system config. Thêm 2 key mới sẽ tự xuất hiện trong response GET. PUT cũng tự handle.

→ KHÔNG cần đổi controller. Chỉ cần verify FE admin biết về 2 key mới (nhưng FE chưa build, để sau).

#### B4.4. Acceptance

- [ ] DB có 2 row config mới
- [ ] GET `/api/admin/configs/system` return 2 key mới
- [ ] PUT `/api/admin/configs/system` với 2 key đổi value → save OK

---

### CHECKPOINT SAU PHẦN B

**Báo USER với report:**

1. Response full-day có `slotAlternatives` thay vì `alternativeCombinations`
2. Bữa TRƯA (nMain=1): list `CHINH_0` alternatives có 8-10 món, food_group đa dạng
3. Bữa TRƯA (nMain=2): có cả `CHINH_0` và `CHINH_1` alternatives, mỗi slot diverse food_group
4. Latency: bao nhiêu ms (chấp nhận < 15s cho nMain=2 + compute alternatives)

User confirm → chuyển Phần C.

---

## PHẦN C — SWAP LOGIC REFACTOR

### STEP C1: Refactor `SwapDishRequest` schema

#### C1.1. Files thay đổi

```
nutrition-service/src/main/java/org/example/nutritionservice/
├── dto/request/SwapDishRequest.java               (UPDATE — thêm pinnedDishes)
└── dto/response/SwapResultResponse.java           (UPDATE — bỏ alternativeCombinations, đổi suggestion structure)
```

#### C1.2. `SwapDishRequest.java` mới

```java
package org.example.nutritionservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.example.nutritionservice.dto.response.DailyPlanResponse;
import org.example.nutritionservice.entity.meallog.MealType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwapDishRequest {

    @NotNull
    @Valid
    private DailyPlanResponse currentPlan;

    @NotNull
    private MealType mealType;

    @NotBlank
    private String swappedSlot;     // slotKey: "CHINH_0", "RAU_0",...

    @NotBlank
    private String newDishId;

    /**
     * Cac slot user da pin tu cac lan swap truoc (xem §0.5).
     * Optional, mac dinh empty.
     */
    @Valid
    private List<PinnedDish> pinnedDishes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PinnedDish {
        @NotBlank
        private String slotKey;
        @NotBlank
        private String dishId;
    }
}
```

#### C1.3. `SwapResultResponse.java` mới

```java
package org.example.nutritionservice.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwapResultResponse {

    private MealSuggestionResponse updatedMeal;   // structure giong PhaB
    private BigDecimal newFinalScore;
    private BigDecimal originalFinalScore;
    private boolean scoreDropTriggered;
    private SwapSuggestion suggestion;            // null neu score sau swap OK

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SwapSuggestion {
        private String message;                   // "Doi [Banh pho] sang [Com trang] tang score len 75"
        private String targetSlotKey;             // "TINH_BOT_0"
        private String suggestedDishId;
        private BigDecimal suggestedScore;
    }
}
```

#### C1.4. Acceptance

- [ ] Compile OK
- [ ] Schema cũ (`swappedSlot`, `newDishId`) vẫn hoạt động khi `pinnedDishes` = null hoặc empty (backward compat trong scope schema)

---

### STEP C2: Logic swap mới — pin + serving-only optimize

#### C2.1. Files thay đổi

```
nutrition-service/src/main/java/org/example/nutritionservice/
└── service/recommendation/RecommendationApiService.java   (REWRITE method swapDish)
```

#### C2.2. Method `swapDish` mới

```java
public SwapResultResponse swapDish(String userId, SwapDishRequest request) {
    // === Validation ===
    MealSuggestionResponse currentMeal = findMeal(request.getCurrentPlan(), request.getMealType());
    if (currentMeal.getTopCombination() == null) {
        throw invalid("Bua khong co to hop top");
    }
    if (request.getCurrentPlan().getGoalCode() == null
        || request.getCurrentPlan().getPlanType() == null) {
        throw invalid("currentPlan thieu goalCode/planType");
    }

    LoadedConfigs configs = configLoaderService.loadForRecommendation(
        request.getCurrentPlan().getGoalCode(),
        request.getCurrentPlan().getPlanType()
    );
    Set<String> favorites = favoriteIds(userId);

    // === Build pinned map ===
    // Tat ca slot trong top combination phai duoc pin
    Map<String, String> pinnedMap = new LinkedHashMap<>();

    // Step 1: pin tat ca slot voi mon hien tai trong topCombination
    for (DishSuggestionResponse dish : currentMeal.getTopCombination().getDishes()) {
        pinnedMap.put(dish.getSlotKey(), dish.getDishId());
    }

    // Step 2: override voi cac slot trong pinnedDishes (user da swap truoc do)
    if (request.getPinnedDishes() != null) {
        for (SwapDishRequest.PinnedDish pin : request.getPinnedDishes()) {
            pinnedMap.put(pin.getSlotKey(), pin.getDishId());
        }
    }

    // Step 3: override slot user vua swap
    pinnedMap.put(request.getSwappedSlot(), request.getNewDishId());

    // === Build dish list theo thu tu slot ===
    List<DishCandidate> pinnedCandidates = new ArrayList<>();
    for (DishSuggestionResponse dish : currentMeal.getTopCombination().getDishes()) {
        String slotKey = dish.getSlotKey();
        String pinnedDishId = pinnedMap.get(slotKey);
        Dish entity = dishRepository.findById(pinnedDishId)
            .orElseThrow(() -> invalid("Mon trong slot " + slotKey + " khong ton tai"));
        if (entity.getSlotCode() != dish.getSlotCode()) {
            throw invalid("Mon pin tai " + slotKey + " sai slot code");
        }
        pinnedCandidates.add(dishFilterService.toCandidate(entity));
    }

    // === Brute force chi tren serving ===
    MacroTarget macroTarget = macroCalculator.calculateMacroTarget(
        currentMeal.getMealKcalTarget(), configs.getGoalConfig()
    );
    MealTarget mealTarget = MealTarget.builder()
        .mealType(currentMeal.getMealType())
        .mealKcal(currentMeal.getMealKcalTarget())
        .macroTarget(macroTarget)
        .mealDate(request.getCurrentPlan().getPlanDate())
        // .perMealConfig khong can vi da pin het mon roi
        .build();

    List<HistoryEntry> history = loadHistory(userId, request.getCurrentPlan().getPlanDate(), configs);
    history.addAll(planHistoryWithoutMeal(request.getCurrentPlan(), request.getMealType()));

    BigDecimal penalty = penaltyService.computePenalty(
        pinnedCandidates, history, favorites, configs, mealTarget.getMealDate()
    );

    // Goi brute force chi enumerate serving (mon da pin)
    MealCombination bestCombo = bruteForceEngine.findBestServingCombo(
        pinnedCandidates, mealTarget, configs, penalty
    );

    if (bestCombo == null) {
        throw invalid("Khong tim duoc serving thoa mac (kcal deviation hoac weight constraint)");
    }

    // === Compute slot alternatives moi cho top combo moi ===
    // Load lai full candidates per slot
    Map<SlotCode, List<DishCandidate>> candidatesPerSlot = loadCandidatesPerSlot(
        currentMeal, configs
    );
    Map<String, List<SlotAlternative>> slotAlts = bruteForceEngine.computeSlotAlternatives(
        bestCombo, candidatesPerSlot, mealTarget, configs
    );

    // === Build response ===
    MealCombinationResponse newTopCombo = toCombinationResponse(bestCombo, favorites);
    Map<String, List<DishOptionResponse>> alternativesResponse = slotAlts.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> e.getValue().stream().map(alt -> toDishOptionResponse(alt, favorites)).toList(),
            (a, b) -> a, LinkedHashMap::new
        ));

    MealSuggestionResponse updatedMeal = MealSuggestionResponse.builder()
        .mealType(currentMeal.getMealType())
        .mealKcalTarget(currentMeal.getMealKcalTarget())
        .topCombination(newTopCombo)
        .slotAlternatives(alternativesResponse)
        .build();

    // === Tinh suggestion (xem §0.8) ===
    BigDecimal originalFinalScore = currentMeal.getTopCombination().getFinalScore();
    BigDecimal newFinalScore = bestCombo.getFinalScore();
    BigDecimal scoreThreshold = BigDecimal.valueOf(configs.getInt("reopt.score_threshold"));
    BigDecimal scoreDropLimit = BigDecimal.valueOf(configs.getInt("reopt.score_drop"));

    boolean triggered = newFinalScore.compareTo(scoreThreshold) < 0
        || originalFinalScore.subtract(newFinalScore).compareTo(scoreDropLimit) > 0;

    SwapResultResponse.SwapSuggestion suggestion = null;
    if (triggered) {
        suggestion = findBestSwapSuggestion(slotAlts, pinnedMap, newFinalScore);
    }

    return SwapResultResponse.builder()
        .updatedMeal(updatedMeal)
        .newFinalScore(newFinalScore)
        .originalFinalScore(originalFinalScore)
        .scoreDropTriggered(triggered)
        .suggestion(suggestion)
        .build();
}

/**
 * Tim slot nao co alternative voi expected_score cao nhat → suggest user doi (§0.8).
 * Khong suggest cac slot da pin (vi user explicit muon giu).
 */
private SwapResultResponse.SwapSuggestion findBestSwapSuggestion(
    Map<String, List<SlotAlternative>> slotAlts,
    Map<String, String> pinnedMap,
    BigDecimal currentScore) {

    BigDecimal bestGain = BigDecimal.ZERO;
    SwapResultResponse.SwapSuggestion best = null;

    for (Map.Entry<String, List<SlotAlternative>> e : slotAlts.entrySet()) {
        if (e.getValue().isEmpty()) continue;
        SlotAlternative topAlt = e.getValue().get(0);
        BigDecimal gain = topAlt.getExpectedScore().subtract(currentScore);
        if (gain.compareTo(bestGain) > 0) {
            bestGain = gain;
            best = SwapResultResponse.SwapSuggestion.builder()
                .message(String.format("Doi mon o [%s] sang [%s] co the tang score len %s",
                    e.getKey(),
                    topAlt.getCandidate().getDishName(),
                    topAlt.getExpectedScore()))
                .targetSlotKey(e.getKey())
                .suggestedDishId(topAlt.getDishId())
                .suggestedScore(topAlt.getExpectedScore())
                .build();
        }
    }
    return best;
}
```

#### C2.3. Method mới trong `BruteForceEngine` — `findBestServingCombo`

```java
/**
 * Brute force chi tren serving khi tat ca mon da pin (cho swap).
 * Khac voi findTopK la khong enumerate dish combinations, chi enumerate serving.
 */
public MealCombination findBestServingCombo(
    List<DishCandidate> pinnedDishes,
    MealTarget mealTarget,
    LoadedConfigs configs,
    BigDecimal penalty) {

    PriorityQueue<MealCombination> queue = new PriorityQueue<>(
        Comparator.comparing(MealCombination::getFinalScore)
    );
    enumerateServings(
        pinnedDishes, 0, new ArrayList<>(),
        mealTarget, configs, penalty, 1, queue
    );
    return queue.isEmpty() ? null : queue.peek();
}
```

Note: `enumerateServings` đã có sẵn từ Phần A. Chỉ cần gọi với `topK=1` để lấy combo tốt nhất.

#### C2.4. Helper `loadCandidatesPerSlot`

Có thể đã có method tương tự trong service. Nếu chưa, viết:

```java
private Map<SlotCode, List<DishCandidate>> loadCandidatesPerSlot(
    MealSuggestionResponse meal,
    LoadedConfigs configs) {

    // Xac dinh slot can dung tu top combination
    Map<SlotCode, Integer> slotCounts = new EnumMap<>(SlotCode.class);
    for (DishSuggestionResponse d : meal.getTopCombination().getDishes()) {
        slotCounts.merge(d.getSlotCode(), 1, Integer::sum);
    }

    BigDecimal dailyKcal = ...; // tinh tu currentPlan (hoac compute lai tu request goal)
    // Tinh slot kcal targets...
    Map<SlotCode, BigDecimal> slotTargets = ...;

    Map<SlotCode, List<DishCandidate>> result = new EnumMap<>(SlotCode.class);
    for (Map.Entry<SlotCode, Integer> e : slotCounts.entrySet()) {
        List<Dish> all = dishRepository.findBySlotCodeAndIsActiveTrue(e.getKey());
        result.put(e.getKey(), dishFilterService.filterCandidatesForSlot(
            e.getKey(),
            slotTargets.get(e.getKey()),
            e.getValue(),
            configs,
            all
        ));
    }
    return result;
}
```

#### C2.5. Acceptance

- [ ] Test swap CHINH = Mực xào → score sau swap ≥ 70 (KHÔNG còn 16.63)
- [ ] Test swap 2 lần liên tiếp (CHINH_0 → A, sau đó CHINH_1 → B với pinnedDishes có CHINH_0=A) → cả 2 món A và B đều xuất hiện trong response cuối
- [ ] `suggestion` = null khi score sau swap > threshold
- [ ] `suggestion` ≠ null khi score sau swap < threshold (test bằng cách swap CHINH thành món có macro lệch nhiều)

---

### CHECKPOINT SAU PHẦN C

**Báo USER với report:**

1. Test swap đơn giản: response có topCombination mới + slotAlternatives mới, score > 70
2. Test swap kép (2 lần liên tiếp với pinnedDishes): cả 2 món user chọn đều được giữ
3. Test trường hợp score sụt: suggestion xuất hiện với gợi ý đổi slot có gain cao nhất
4. Test với nMain=2: tất cả 4 slot (CHINH_0, CHINH_1, RAU_0, TINH_BOT_0) đều có thể swap riêng

User confirm → DONE refactor v3.2.

---

## PHỤ LỤC

### Mapping spec v3.2 → code

| Spec section | File chịu trách nhiệm |
|---|---|
| §0.2 Diversity main key | `BruteForceEngine.extractMainKey`, `findTopK` |
| §0.3 Slot Alternatives | `BruteForceEngine.computeSlotAlternatives`, `RecommendationApiService.toMealResponse` |
| §0.4 Swap pin + serving-only | `RecommendationApiService.swapDish` (full rewrite) |
| §0.5 Pinned Dishes API | `SwapDishRequest.pinnedDishes` |
| §0.6.1 Filter chặt theo nMain | `DishFilterService.filterCandidatesForSlot` |
| §0.6.2 Early prune | `BruteForceEngine.shouldPrune` |
| §0.6.3 Giảm serving steps | Config `filter.serving_steps` |
| §0.7 Cấm same food_group | `BruteForceEngine.buildDishCombinations` |
| §0.8 Suggestion semantic mới | `RecommendationApiService.findBestSwapSuggestion` |
| §0.9 Config bổ sung | `data.sql` insert + admin endpoint tự handle |

### Test data fixture

Để test diversity, có thể chạy SQL deactivate bớt dish CHINH trong 1 food_group:

```sql
-- Tam thoi disable 80% dish GIA_CAM de buoc diversity phai chon food_group khac
UPDATE dishes SET is_active = false 
WHERE food_group_code = 'GIA_CAM' AND slot_code = 'CHINH'
ORDER BY name LIMIT 12;

-- Sau khi test xong, restore:
UPDATE dishes SET is_active = true 
WHERE food_group_code = 'GIA_CAM' AND slot_code = 'CHINH';
```

### Postman update cần làm sau khi refactor xong

Khi Phần B/C xong, mình sẽ gửi user file Postman v3 mới với:
- Bỏ field `alternativeCombinations` trong response examples
- Thêm field `slotAlternatives` + `pinnedDishes`
- Thêm 2 test case mới: "Swap with pinned" và "Swap with score drop suggestion"

---

## VERSION HISTORY

| Ngày | Phiên bản | Thay đổi |
|---|---|---|
| 24/05/2026 | v1.0 | Refactor 3 phần A/B/C theo spec nghiệp vụ v3.2 |
