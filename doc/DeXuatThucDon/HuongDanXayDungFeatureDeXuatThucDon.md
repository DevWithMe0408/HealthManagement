# HƯỚNG DẪN XÂY DỰNG FEATURE "ĐỀ XUẤT THỰC ĐƠN"

> Tài liệu này dành cho **agent** thực hiện feature "Đề xuất thực đơn"
> cho project HealthManagement (graduation thesis).
>
> Agent: ĐỌC TOÀN BỘ tài liệu này TRƯỚC khi viết bất kỳ dòng code nào.
>
> Tham chiếu bắt buộc:
> - `doc/nghiep_vu_de_xuat_thuc_don_v3.md` — spec nghiệp vụ (single source of truth)
> - `CLAUDE.md` — quy ước project chung
> - Branch `feature-DeXuatThucDon` — code đã có sẵn (entity catalog, config, admin API)

---

## 0. CONTEXT VÀ NGUYÊN TẮC

### 0.1. Tổng quan project

- **Backend:** Spring Boot 3.4.3 microservices, Java 21, Spring Cloud (Eureka, Gateway), RabbitMQ, MySQL 8.
- **Service liên quan:** `nutrition-service` (chứa toàn bộ logic đề xuất). Port chưa fix (chưa run được vì là minimal stub trước đó, nay đang xây).
- **DB:** `nutrition_db` riêng (database-per-service). `ddl-auto=update`, KHÔNG dùng Flyway.
- **Seed data:** đã có 142 dishes + 203 ingredients + 757 dish_ingredients + 6 bảng config seeded (`data.sql`, `dish_seed.sql`).
- **Auth:** API Gateway validate JWT, inject `X-User-Id`, `X-Username`, `X-Roles` vào header. Các endpoint user-facing đọc `X-User-Id` để xác định user.

### 0.2. Quy tắc làm việc cho agent

- **KHÔNG tự rewrite code đã có** trên branch `feature-DeXuatThucDon`. Code đó đã review xong và OK.
- **KHÔNG đổi schema 9 bảng đã seeded** (goal_configs, meal_ratio_configs, slot_configs, penalty_configs, surplus_penalty_configs, system_config, dishes, ingredients, dish_ingredients). Nếu phát hiện thiếu gì → BÁO TRƯỚC, không tự sửa.
- **KHÔNG over-engineer**: không thêm WebFlux, không thêm Redis cache, không thêm CQRS pattern. Đây là thesis MVP, ưu tiên "đủ tốt để bảo vệ".
- **KHÔNG tự ý thêm dependency mới** trừ những lib đã có trong `pom.xml` parent. Nếu cần thêm → hỏi trước.
- Mỗi step trong tài liệu này phải làm **xong hoàn toàn + chờ user review** trước khi chuyển step tiếp theo.
- Sau mỗi step, agent **chạy `./mvnw clean compile -pl nutrition-service`** để verify code build được trước khi báo "done".

### 0.3. Quy ước code

- **Tên class/method/biến:** tiếng Anh, camelCase.
- **Tên DB column/table:** snake_case.
- **Code comment:** tiếng Việt (vì user là người đọc/maintain chính, ưu tiên dễ hiểu cho thesis defense).
- **Commit message:** tiếng Việt, prefix theo convention `feat:`, `fix:`, `refactor:`, `docs:`.
- **Package convention:** `org.example.nutritionservice.<layer>.<domain>` (ví dụ: `entity.meallog`, `service.recommendation`, `repository.meallog`).
- **DTO suffix:** `Request`, `Response` (đã có sẵn convention trong `dto/request/`, `dto/response/`).
- **Lombok:** dùng `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` cho entity. Service dùng `@RequiredArgsConstructor` để inject final fields.
- **Logging:** `@Slf4j`, dùng `log.debug()` cho recommendation flow để debug penalty/score (KHÔNG `log.info()` mỗi tổ hợp — sẽ flood log với 11M tổ hợp).
- **Validation:** dùng `@Valid` + Jakarta Bean Validation (`@NotNull`, `@Positive`, `@DecimalMin`, etc.).
- **Exception:** dùng `org.example.web.exception.*` đã có sẵn ở module `common` (xem cách AdminConfigController đang dùng).

### 0.4. Decisions đã chốt với user (KHÔNG hỏi lại)

| Quyết định | Lý do |
|---|---|
| `meal_log` và `favorite_dishes` đặt trong `nutrition-service` / `nutrition_db` | Cần JOIN với dishes để tính penalty, cùng DB tránh cross-service call |
| `MealStatus` enum có sẵn 5 giá trị (SUGGESTED, FOLLOWED, MODIFIED, CUSTOM, SKIPPED) | MVP chỉ dùng SUGGESTED, mở rộng v2.0 không cần migration |
| Lưu macro trực tiếp trên `dishes` (Cách A) — KHÔNG tính từ ingredient | Đã chốt trước, dish_ingredient chỉ để hiển thị |
| Snapshot `food_group_code` và `slot_code` vào `meal_log` | Chống admin sửa dish làm hỏng lịch sử penalty |
| Thể trạng (GẦY/CÂN_ĐỐI/THỪA_CÂN/BÉO_PHÌ) **TẠM compute ở FE** từ PBF | Tránh phụ thuộc ML pipeline chưa wired |

---

## PHẦN 1 — BACKEND: ENTITY + CORE LOGIC + API

> Phần này gồm 4 step. Hoàn thành tuần tự, KHÔNG nhảy step.

---

### STEP 1: Tạo entity `MealLog`

#### 1.1. Mục tiêu
Tạo bảng `meal_log` lưu lịch sử mỗi bữa ăn được đề xuất. Đây là input cho 3-tier penalty (cùng dish_id, cùng food_group) khi đề xuất bữa kế tiếp.

#### 1.2. Files cần tạo

```
nutrition-service/src/main/java/org/example/nutritionservice/
├── entity/meallog/
│   ├── MealLog.java
│   ├── MealStatus.java          (enum 5 giá trị)
│   ├── MealType.java             (enum: SANG, PHU_SANG, TRUA, PHU_CHIEU, TOI)
│   └── MealLogDish.java          (entity con: 1 bữa có thể có nhiều dish)
├── repository/meallog/
│   └── MealLogRepository.java
│   └── MealLogDishRepository.java
```

#### 1.3. Schema chi tiết

**Table `meal_log`** (1 row = 1 bữa của user trong 1 ngày):

| Column | Type | Constraint | Ghi chú |
|---|---|---|---|
| `id` | VARCHAR(36) | PK, `@UuidGenerator` | UUID giống pattern Dish/Ingredient |
| `user_id` | VARCHAR(36) | NOT NULL | Lấy từ `X-User-Id` header |
| `meal_date` | DATE | NOT NULL | Ngày lịch của bữa ăn (LocalDate Java) |
| `meal_type` | VARCHAR(20) | NOT NULL, `@Enumerated(STRING)` | SANG / PHU_SANG / TRUA / PHU_CHIEU / TOI |
| `plan_type` | VARCHAR(10) | NOT NULL | "3_BUA" / "5_BUA" (snapshot user chọn lúc đó) |
| `goal_code` | VARCHAR(20) | NOT NULL | GIAM / DUY_TRI / TANG (snapshot) |
| `meal_kcal_target` | DECIMAL(7,2) | NOT NULL | Snapshot target kcal lúc đề xuất (debug + UI) |
| `total_kcal_actual` | DECIMAL(7,2) | NOT NULL | Tổng kcal thực tế của tổ hợp được chọn |
| `total_protein_g` | DECIMAL(6,2) | NOT NULL | Tổng protein |
| `total_fat_g` | DECIMAL(6,2) | NOT NULL | Tổng fat |
| `total_carb_g` | DECIMAL(6,2) | NOT NULL | Tổng carb |
| `final_score` | DECIMAL(5,2) | NOT NULL | Final Score của tổ hợp (debug + UI hiển thị) |
| `status` | VARCHAR(20) | NOT NULL, default 'SUGGESTED' | Enum MealStatus 5 giá trị |
| `created_at` | TIMESTAMP | NOT NULL, `@CreationTimestamp` | |
| `updated_at` | TIMESTAMP | NOT NULL, `@UpdateTimestamp` | |

**Indexes:**
- `idx_meal_log_user_date` ON `(user_id, meal_date)` — query lịch sử 3 ngày
- `idx_meal_log_user_date_type` ON `(user_id, meal_date, meal_type)` — query check bữa đã ăn chưa
- UNIQUE `(user_id, meal_date, meal_type)` — 1 user 1 ngày 1 bữa chỉ 1 record (nếu re-suggest → DELETE-INSERT hoặc UPDATE)

**Table `meal_log_dish`** (1 row = 1 dish trong bữa):

| Column | Type | Constraint | Ghi chú |
|---|---|---|---|
| `id` | VARCHAR(36) | PK, `@UuidGenerator` | |
| `meal_log_id` | VARCHAR(36) | NOT NULL | FK reference meal_log.id (KHÔNG dùng JPA `@ManyToOne` để giữ database-per-service mindset, dùng raw column như `DishIngredient.dishId`) |
| `dish_id` | VARCHAR(36) | NOT NULL | Reference dishes.id |
| `food_group_code` | VARCHAR(20) | NOT NULL, `@Enumerated(STRING)` | **SNAPSHOT** từ dish lúc tạo log |
| `slot_code` | VARCHAR(20) | NOT NULL, `@Enumerated(STRING)` | **SNAPSHOT** từ dish lúc tạo log |
| `serving_multiplier` | DECIMAL(3,2) | NOT NULL | 0.50 - 2.00 |
| `actual_grams` | DECIMAL(6,2) | NOT NULL | base_serving × serving_multiplier (compute và lưu sẵn) |
| `dish_kcal` | DECIMAL(6,2) | NOT NULL | kcal của riêng dish này trong bữa |
| `sort_order` | SMALLINT | NOT NULL | Thứ tự hiển thị trong bữa |
| `created_at` | TIMESTAMP | NOT NULL, `@CreationTimestamp` | |

**Indexes:**
- `idx_meal_log_dish_meal_log_id` ON `(meal_log_id)` — load dishes của 1 meal_log
- `idx_meal_log_dish_dish_id` ON `(dish_id)` — query "user đã ăn món X bao giờ chưa"

#### 1.4. Enum `MealStatus`

```java
public enum MealStatus {
    SUGGESTED,   // MVP: mặc định khi gen
    FOLLOWED,    // v2.0: user xác nhận đã ăn đúng đề xuất
    MODIFIED,    // v2.0: user ăn nhưng sửa serving
    CUSTOM,      // v2.0: user tự nhập món khác hoàn toàn
    SKIPPED      // v2.0: user bỏ bữa
}
```

#### 1.5. Enum `MealType`

```java
public enum MealType {
    SANG,
    PHU_SANG,
    TRUA,
    PHU_CHIEU,
    TOI
}
```

#### 1.6. Repository interface

`MealLogRepository extends JpaRepository<MealLog, String>` — cần các method:

```java
// Query lịch sử N ngày gần nhất (cho penalty)
List<MealLog> findByUserIdAndMealDateBetweenOrderByMealDateDescMealTypeAsc(
    String userId, LocalDate from, LocalDate to);

// Check bữa nào trong ngày đã có record (cho determine_remaining_meals)
List<MealLog> findByUserIdAndMealDate(String userId, LocalDate mealDate);

// Optional: query 1 bữa cụ thể
Optional<MealLog> findByUserIdAndMealDateAndMealType(
    String userId, LocalDate mealDate, MealType mealType);
```

`MealLogDishRepository extends JpaRepository<MealLogDish, String>` — cần:

```java
List<MealLogDish> findByMealLogIdIn(Collection<String> mealLogIds);
```

#### 1.7. Acceptance criteria

- [ ] 2 entity + 1 enum MealStatus + 1 enum MealType + 2 repository tạo đầy đủ
- [ ] `./mvnw clean compile -pl nutrition-service` thành công
- [ ] Khi start `nutrition-service` lần đầu, Hibernate tự tạo 2 bảng `meal_log` và `meal_log_dish` với index đúng
- [ ] Verify schema bằng MySQL: `DESCRIBE meal_log;` và `SHOW INDEX FROM meal_log;`
- [ ] KHÔNG seed data mẫu vào file `data.sql` cho 2 bảng này (lịch sử user phải do user thực sinh ra)

#### 1.8. Lưu ý kỹ thuật

- **Không dùng `@OneToMany`** giữa MealLog → MealLogDish. Lý do: hibernate cascade có thể gây N+1 hoặc lỗi khó debug khi save. Trong service layer, lưu MealLog xong rồi save danh sách MealLogDish riêng — pattern này giống `dish_ingredients` đã có.
- **Lưu macro tổng (kcal/P/F/C) vào meal_log thay vì compute lại từ meal_log_dish** mỗi lần query. Lý do: penalty cần kcal/macro tổng → query 1 lần là đủ thay vì JOIN + SUM.
- Nếu thấy tốn dung lượng vì denormalization → đây là tradeoff đã cân nhắc, KHÔNG tự ý normalize lại.

---

### STEP 2: Tạo entity `FavoriteDish`

#### 2.1. Mục tiêu
Cho phép user đánh dấu món yêu thích. Khi tính penalty, món yêu thích được nhân với `penalty.fav_discount` (default 0.5) → bớt bị phạt.

#### 2.2. Files cần tạo

```
nutrition-service/src/main/java/org/example/nutritionservice/
├── entity/favorite/
│   ├── FavoriteDish.java
│   └── FavoriteDishId.java       (composite key class)
├── repository/favorite/
│   └── FavoriteDishRepository.java
```

#### 2.3. Schema

**Table `favorite_dishes`:**

| Column | Type | Constraint |
|---|---|---|
| `user_id` | VARCHAR(36) | PK part 1 |
| `dish_id` | VARCHAR(36) | PK part 2 |
| `created_at` | TIMESTAMP | NOT NULL, `@CreationTimestamp` |

**Index:**
- PK composite `(user_id, dish_id)` — đã đủ, không cần index riêng

#### 2.4. Composite key (`FavoriteDishId`)

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FavoriteDishId implements Serializable {
    private String userId;
    private String dishId;
}
```

Dùng `@IdClass(FavoriteDishId.class)` trong entity. Pattern này đã có ở `MealRatioConfig` + `MealRatioConfigId`.

#### 2.5. Repository

```java
public interface FavoriteDishRepository extends JpaRepository<FavoriteDish, FavoriteDishId> {

    // Lấy tất cả favorite của user (để in-memory check khi tính penalty)
    List<FavoriteDish> findByUserId(String userId);

    // Check 1 dish cụ thể có favorite không
    boolean existsByUserIdAndDishId(String userId, String dishId);

    // Delete cho toggle off
    void deleteByUserIdAndDishId(String userId, String dishId);
}
```

#### 2.6. Acceptance criteria

- [ ] Entity + IdClass + Repository tạo đầy đủ
- [ ] Compile OK
- [ ] Hibernate tạo bảng `favorite_dishes` với composite PK
- [ ] **CHƯA cần** tạo controller / service cho favorite ở step này (sẽ làm trong Step 4 cùng API)

---

### STEP 3: Build core domain services (logic đề xuất)

#### 3.1. Mục tiêu
Xây dựng các service tính toán theo đúng spec §3, §4 của `nghiep_vu_de_xuat_thuc_don_v3.md`. KHÔNG dính controller, KHÔNG dính DB write — chỉ pure logic + read config.

#### 3.2. Files cần tạo

```
nutrition-service/src/main/java/org/example/nutritionservice/
├── service/recommendation/
│   ├── ConfigLoaderService.java       (load các config về POJO, in-memory)
│   ├── MacroCalculator.java           (tính daily_kcal, meal_kcal, macro_target, slot_target)
│   ├── DishFilterService.java         (filter ứng viên theo slot + kcal_tolerance + weight constraint)
│   ├── ScoringService.java            (compute Macro Score)
│   ├── PenaltyService.java            (compute 3-tier penalty với fav_discount)
│   ├── BruteForceEngine.java          (sinh tổ hợp + serving grid + sort top K)
│   └── RecommendationOrchestrator.java (orchestrate luồng full-day theo §4)
├── domain/recommendation/             (POJO domain models — KHÔNG phải entity)
│   ├── UserContext.java               (input của 1 lần đề xuất: userId, TDEE, goal, planType, perMealConfig)
│   ├── MealTarget.java                (kcal/macro/slot target cho 1 bữa)
│   ├── DishCandidate.java             (ứng viên + base macro, đã filter)
│   ├── DishWithServing.java           (DishCandidate + serving_multiplier)
│   ├── MealCombination.java           (tổ hợp dishes + servings + scores)
│   └── HistoryEntry.java              (1 dish trong lịch sử, dùng cho penalty)
```

#### 3.3. Spec chi tiết từng service

##### 3.3.1. `ConfigLoaderService`

**Trách nhiệm:** load 1 lần (in-memory hoặc cache) các config dùng nhiều lần trong 1 request recommendation.

```java
public class LoadedConfigs {
    GoalConfig goalConfig;                              // 1 goal
    List<MealRatioConfig> mealRatios;                   // ratios cho planType được chọn
    Map<SlotCode, SlotConfig> slotConfigs;              // 4 slot (CHINH/RAU/TINH_BOT/COMBO)
    Map<Integer, Map<Integer, Integer>> penaltyConfigs; // layer -> distance -> penalty_value
    Map<String, BigDecimal> surplusPenalty;             // PROTEIN/FAT/CARB/KCAL -> factor
    Map<String, String> systemConfigs;                  // key-value của system_config
}

public interface ConfigLoaderService {
    LoadedConfigs loadForRecommendation(String goalCode, String planType);

    // Helper convert system_config String value → typed
    BigDecimal getDecimal(String key);
    Integer getInt(String key);
    List<BigDecimal> getDecimalArray(String key);  // parse JSON_ARRAY
}
```

**Lưu ý:**
- KHÔNG cache cross-request (chỉ 1 request 1 LoadedConfigs). Lý do: admin có thể đổi config bất kỳ lúc nào, không muốn stale.
- `getDecimalArray` parse JSON_ARRAY (lib `com.fasterxml.jackson.databind.ObjectMapper` đã có sẵn vì Spring Boot include).

##### 3.3.2. `MacroCalculator`

```java
public class MacroCalculator {

    // §3.2: daily_kcal = TDEE × cal_multiplier
    BigDecimal calculateDailyKcal(BigDecimal tdee, GoalConfig goalConfig);

    // §3.3: meal_kcal = daily_kcal × meal_ratio[planType][mealType]
    BigDecimal calculateMealKcal(BigDecimal dailyKcal, MealType mealType, List<MealRatioConfig> ratios);

    // §3.4: macro_target = meal_kcal × macro_ratio / kcal_per_gram
    MacroTarget calculateMacroTarget(BigDecimal mealKcal, GoalConfig goalConfig);
    // → {protein_g, fat_g, carb_g, kcal}

    // §3.5: chia target theo slot, có xử lý redistribution
    Map<SlotCode, BigDecimal> calculateSlotKcalTargets(
        BigDecimal mealKcal,
        GoalConfig goalConfig,
        PerMealConfig perMeal);
    // perMeal: type=COMBO → 1 slot duy nhất COMBO=100%
    //         type=NHIEU_MON, hasMain=true/n_main, hasRau=0/1/2, hasCarb=0/1
}
```

**Constants:** Trong class này declare:
```java
private static final BigDecimal KCAL_PER_G_PROTEIN = new BigDecimal("4");
private static final BigDecimal KCAL_PER_G_FAT     = new BigDecimal("9");
private static final BigDecimal KCAL_PER_G_CARB    = new BigDecimal("4");
```

##### 3.3.3. `DishFilterService`

```java
public class DishFilterService {

    // §3.6: filter ứng viên cho 1 slot
    List<DishCandidate> filterCandidatesForSlot(
        SlotCode slot,
        BigDecimal slotKcalTarget,
        LoadedConfigs configs,
        List<Dish> allActiveDishesInSlot);  // pre-loaded từ DB

    // Logic filter cho mỗi dish D:
    // 1. base_kcal_at_serving = D.kcal_per_100g × D.base_serving_g / 100
    // 2. kcal_at_min = base_kcal_at_serving × serving_min_multiplier
    // 3. kcal_at_max = base_kcal_at_serving × serving_max_multiplier
    // 4. pass = (kcal_at_min ≤ target × 1.15) AND (kcal_at_max ≥ target × 0.85)
    // 5. weight constraint (§3.6 tuyệt đối): check ở scoring phase, không ở đây
    //    (vì filter ở đây chỉ loại dish KHÔNG THỂ vừa, sau brute force mới kiểm serving cụ thể)
}
```

##### 3.3.4. `ScoringService`

```java
public class ScoringService {

    // §3.7.2: tính Macro Score
    BigDecimal computeMacroScore(
        MealActual actual,        // totals sau khi nhân serving
        MacroTarget target,
        GoalConfig goalConfig,
        LoadedConfigs configs);

    // Pseudocode:
    // for each macro in [P, F, C, KCAL]:
    //     if target[macro] < 2g → score[macro] = 1.0, mark skip
    //     raw_dev = |actual[macro] - target[macro]| / target[macro]
    //     dev = (actual > target) ? raw_dev × surplus_pf[macro] : raw_dev
    //     score[macro] = max(0, 1 - dev / threshold)
    // Re-normalize weights nếu có macro bị skip:
    //     skipped_weight = sum(w của macro skipped)
    //     for non-skipped: w' = w + skipped_weight / count(non-skipped)
    // Macro Score = sum(w' × score) × 100
}
```

**Lưu ý quan trọng:**
- Dùng `BigDecimal` xuyên suốt, **KHÔNG** dùng `double/float` (sai số tích lũy qua 11M tổ hợp sẽ lệch ranking).
- `RoundingMode.HALF_UP` với scale=4 cho intermediate, scale=2 cho final score.

##### 3.3.5. `PenaltyService`

```java
public class PenaltyService {

    // §3.7.3: 3-tier penalty
    BigDecimal computePenalty(
        List<DishCandidate> combo,
        List<HistoryEntry> history,
        Set<String> favoriteDishIds,
        LoadedConfigs configs,
        LocalDate targetMealDate);

    // Pseudocode chi tiết trong nghiep_vu_de_xuat_thuc_don_v3.md §3.7.3
    // Đảm bảo:
    //   - distance = |target_date - history_date| (số ngày lịch)
    //   - distance > 2 → penalty = 0 cho entry đó
    //   - Layer 2 (food_group) CHỈ áp khi dish.slot = CHINH (theo công thức ở §3.7.3)
    //     → đọc kỹ pseudocode: `elif dish_i.food_group == history_h.food_group AND dish_i.slot == 'CHÍNH'`
    //   - fav_factor = 0.5 nếu dish in favoriteDishIds, else 1.0
    //   - total = min(total, penalty.cap) — default 40
}
```

##### 3.3.6. `BruteForceEngine`

```java
public class BruteForceEngine {

    List<MealCombination> findTopK(
        UserContext userCtx,
        MealTarget mealTarget,
        Map<SlotCode, List<DishCandidate>> candidatesPerSlot,
        List<HistoryEntry> history,
        Set<String> favoriteIds,
        LoadedConfigs configs,
        int topK);  // default 10 từ display.top_k
}
```

**Cấu trúc giải thuật:**
```
1. Cartesian product candidates theo slot → list of combos (dish combinations)
2. For each combo:
     a. Compute penalty ONCE (penalty độc lập với serving)
     b. Memoize, vì nhiều serving sẽ dùng lại cùng penalty
3. For each combo, for each serving combination (7^N hoặc 4^N cho combo):
     a. Compute totals (kcal/P/F/C)
     b. Early exit: |totals.kcal - meal_kcal| / meal_kcal > 0.25 → skip
     c. Weight constraint: for each dish, check actual_grams ∈ [min_g, max_g] của slot → skip nếu vi phạm
     d. Compute macro_score
     e. final_score = max(0, macro_score - penalty)
     f. Maintain min-heap size K (priority queue) — top K theo final_score
4. Return heap sorted desc

Serving steps:
- Slot khác COMBO: dùng filter.serving_steps (7 mức: 0.5...2.0)
- Slot COMBO: dùng filter.combo_serving_steps (4 mức: 0.75...1.5)
```

**Performance guard:**
- Trước khi enter Cartesian product, log số tổ hợp ước tính. Nếu `> 50M` → throw `RecommendationTooComplexException` (sẽ tạo class này trong step 4).
- Lý do: 158M tổ hợp (3+2+1) ăn quá nhiều CPU cho thesis demo. MVP nên giới hạn ở 5 món/bữa.
- Override: user request có thể truyền `forceCompute=true` để bypass guard (cho admin debug).

##### 3.3.7. `RecommendationOrchestrator`

```java
public class RecommendationOrchestrator {

    // §4.2: gen full day, tuần tự
    DailyPlan recommendFullDay(UserContext userCtx);

    // Logic:
    // 1. determine_remaining_meals(userCtx, currentTime) theo bảng §4.1
    //    - Query meal_log theo (userId, today) xem bữa nào đã có record
    //    - Trả về list MealType cần đề xuất + ngày đề xuất (today hoặc tomorrow)
    // 2. Load history 3 ngày từ meal_log + meal_log_dish
    // 3. For each meal_type in remaining (theo thứ tự thời gian):
    //      a. Tính meal_kcal, macro_target, slot_targets
    //      b. Load + filter candidates per slot
    //      c. BruteForce → top K combos
    //      d. Lấy combo[0] làm "current suggestion"
    //      e. Append vào history (in-memory) để bữa sau biết
    //      f. Append vào DailyPlan
    // 4. Return DailyPlan
    //
    // QUAN TRỌNG: KHÔNG ghi meal_log ở đây. Chỉ ghi khi user explicit confirm
    //              (sẽ làm ở step 4 controller).
    //
    // Method cho swap (§6): recommendSingleMeal(userCtx, mealType, history, swappedDishIfAny)

    DailyPlan recommendSingleMeal(...);
}
```

#### 3.4. Acceptance criteria

- [ ] 7 service class + 6 domain POJO compile được
- [ ] Có **unit test cơ bản** cho:
  - [ ] `MacroCalculator.calculateMacroTarget` với GIAM_CAN goal: input TDEE=2000, output kcal/P/F/C đúng theo §3.2-3.4
  - [ ] `ScoringService.computeMacroScore` edge case target < 2g (skip + re-normalize)
  - [ ] `PenaltyService.computePenalty` 3-tier: lịch sử có 1 món chính trùng cùng ngày → penalty = 12 × 1.0 (slot_factor) × 1.0 (no fav) = 12
- [ ] Sử dụng `BigDecimal` xuyên suốt cho macro/kcal/score
- [ ] KHÔNG có dependency ngược (entity catalog/config KHÔNG import domain/recommendation, và ngược lại OK)

---

### STEP 4: Build API endpoints

#### 4.1. Mục tiêu
Expose 4 endpoint user-facing + 2 endpoint favorite. Wire orchestrator vào controller. Handle persist meal_log khi user confirm.

#### 4.2. Files cần tạo

```
nutrition-service/src/main/java/org/example/nutritionservice/
├── controller/
│   ├── RecommendationController.java
│   └── FavoriteDishController.java
├── dto/request/
│   ├── RecommendFullDayRequest.java
│   ├── SwapDishRequest.java
│   └── ConfirmMealRequest.java
├── dto/response/
│   ├── DailyPlanResponse.java
│   ├── MealSuggestionResponse.java
│   ├── DishSuggestionResponse.java
│   └── SwapResultResponse.java
├── service/meallog/
│   └── MealLogService.java        (persist logic)
├── service/favorite/
│   └── FavoriteDishService.java
└── exception/
    └── RecommendationTooComplexException.java
```

#### 4.3. Endpoints

##### `POST /api/recommendation/full-day`

Đề xuất full day (3 hoặc 5 bữa) từ thời điểm hiện tại.

**Request body:**
```json
{
  "tdee": 2000.0,
  "goalCode": "GIAM",
  "planType": "3_BUA",
  "constitution": "THUA_CAN",
  "constitutionConfirmed": false,
  "perMealConfig": {
    "SANG":     { "mealKind": "COMBO" },
    "TRUA":     { "mealKind": "NHIEU_MON", "nMain": 1, "nRau": 1, "nCarb": 1 },
    "TOI":      { "mealKind": "NHIEU_MON", "nMain": 2, "nRau": 1, "nCarb": 1 }
  }
}
```

**Validation:**
- `tdee` ∈ [1000, 5000]
- `goalCode` ∈ {GIAM, DUY_TRI, TANG}
- `planType` ∈ {3_BUA, 5_BUA}
- `constitution` ∈ {GAY, CAN_DOI, THUA_CAN, BEO_PHI}
- `perMealConfig` key phải match planType (3_BUA → SANG/TRUA/TOI; 5_BUA → +PHU_SANG/PHU_CHIEU)
- Nếu `mealKind=NHIEU_MON`: nMain ∈ [1,3], nRau ∈ [0,2], nCarb ∈ [0,1]
- Nếu `mealKind=COMBO`: bỏ qua nMain/nRau/nCarb

**Response (`DailyPlanResponse`):**
```json
{
  "code": null,
  "message": "OK",
  "data": {
    "planDate": "2026-05-22",
    "warning": {
      "level": "WARNING",
      "code": "OBESE_BUT_GAIN_WEIGHT",
      "message": "...",
      "requireConfirm": true
    },
    "meals": [
      {
        "mealType": "SANG",
        "mealKcalTarget": 500.0,
        "topCombination": {
          "totalKcal": 495.0,
          "totalProtein": 28.0,
          "totalFat": 18.5,
          "totalCarb": 50.0,
          "macroScore": 88.5,
          "penalty": 0.0,
          "finalScore": 88.5,
          "dishes": [
            {
              "dishId": "uuid...",
              "dishName": "Phở bò",
              "slotCode": "COMBO",
              "foodGroupCode": "COMBO",
              "servingMultiplier": 1.0,
              "actualGrams": 500.0,
              "dishKcal": 495.0,
              "isFavorite": false
            }
          ]
        },
        "alternativeCombinations": [ /* 9 tổ hợp còn lại, structure giống */ ]
      }
    ]
  }
}
```

**Logic:**
1. Check warning constitution × goal (§3.1). Nếu cần confirm và `constitutionConfirmed=false` → return chỉ warning, không gen.
2. Load history 3 ngày từ DB.
3. Call `orchestrator.recommendFullDay`.
4. Return response. **KHÔNG persist meal_log ở step này** — user phải explicit confirm qua endpoint riêng.

##### `POST /api/recommendation/swap-dish`

Re-optimize khi user swap 1 món (§6).

**Request:**
```json
{
  "currentPlan": { /* DailyPlanResponse */ },
  "mealType": "TRUA",
  "swappedSlot": "CHINH_0",
  "newDishId": "uuid..."
}
```

**Response (`SwapResultResponse`):**
```json
{
  "code": null,
  "message": "OK",
  "data": {
    "updatedMeal": { /* MealSuggestionResponse */ },
    "newFinalScore": 67.5,
    "originalFinalScore": 82.0,
    "scoreDropTriggered": true,
    "suggestion": {
      "message": "Điều chỉnh [Rau muống xào] từ 100g xuống 75g sẽ tăng score lên 75/100",
      "targetSlotIdInMeal": "RAU_0",
      "suggestedServingMultiplier": 0.75,
      "suggestedNewScore": 75.0
    }
  }
}
```

**Logic:** giống §6.1-6.3. Suggestion `null` nếu không thoả điều kiện trigger.

##### `POST /api/meal-log/confirm`

Persist 1 meal vào meal_log với `status=SUGGESTED`.

**Request:**
```json
{
  "mealDate": "2026-05-22",
  "mealType": "TRUA",
  "planType": "3_BUA",
  "goalCode": "GIAM",
  "mealKcalTarget": 800.0,
  "selectedCombination": { /* topCombination object */ }
}
```

**Logic:**
- Read `X-User-Id` từ header
- Upsert: nếu đã có record `(user_id, meal_date, meal_type)` → DELETE meal_log_dish cũ + UPDATE meal_log + INSERT meal_log_dish mới (trong 1 `@Transactional`)
- Nếu chưa có → INSERT
- Status mặc định = SUGGESTED

##### `GET /api/meal-log/history?days=3`

Trả về lịch sử N ngày gần nhất (cho UI hiển thị "thực đơn hôm qua").

##### `POST /api/favorite-dishes/{dishId}` — toggle on
##### `DELETE /api/favorite-dishes/{dishId}` — toggle off
##### `GET /api/favorite-dishes` — list favorite của user

#### 4.4. Exception handling

`RecommendationTooComplexException` (tổ hợp > 50M):
- HTTP 422 (Unprocessable Entity)
- Message: "Cấu hình bữa quá phức tạp, vui lòng giảm số món/slot. Ước tính N tổ hợp."

Các exception khác dùng `GlobalExceptionHandler` đã có sẵn ở module `common`.

#### 4.5. Security

- 4 endpoint recommendation/meal-log: yêu cầu authenticated user (default Spring Security đã handle qua gateway JWT)
- `userId` lấy từ `@RequestHeader("X-User-Id") String userId` (KHÔNG lấy từ request body — security risk)
- 3 endpoint favorite: tương tự

#### 4.6. Acceptance criteria

- [ ] Khởi động `nutrition-service` thành công, register vào Eureka
- [ ] Postman/curl test được `POST /api/recommendation/full-day` trả response đúng schema
- [ ] Toggle favorite hoạt động (POST → GET trả về có dish; DELETE → GET không còn)
- [ ] Confirm meal → DB có record meal_log + meal_log_dish; gọi lại confirm cùng `(date, type)` → record bị update, không insert thêm
- [ ] Lần gen tiếp theo, penalty được tính đúng (verify bằng log debug)
- [ ] Test edge case constitution warning: BEO_PHI + TANG_CAN + confirmed=false → response trả warning, KHÔNG có meals

---

## CHECKPOINT SAU PHẦN 1

Sau khi 4 step trên hoàn thành, agent BÁO USER review trước khi bắt đầu Phần 2.

User sẽ verify:
1. Postman collection test full flow đề xuất → swap → confirm → gen ngày kế tiếp có penalty đúng
2. Đọc code, review từng service, comment nếu cần refactor
3. Approve để bắt đầu Phần 2 (Frontend UI)

---

## PHẦN 2, 3 — SẼ ĐƯỢC BỔ SUNG SAU

(Placeholder — user sẽ bổ sung tài liệu này khi Phần 1 hoàn thành)

- **Phần 2:** Frontend UI (`/nutrition-plan` page), form input, hiển thị daily plan, tương tác swap, favorite toggle.
- **Phần 3:** Edge cases (§5), tối ưu performance (early exit, prune memoize), warning matrix UI, polish error message.

---

## PHỤ LỤC

### A. Mapping tài liệu §X → code

| Spec section | Service/Method chịu trách nhiệm |
|---|---|
| §3.1 Validation thể trạng × goal | `RecommendationController` (trước khi gọi orchestrator) |
| §3.2 calories_per_day | `MacroCalculator.calculateDailyKcal` |
| §3.3 chia kcal cho bữa | `MacroCalculator.calculateMealKcal` |
| §3.4 macro target | `MacroCalculator.calculateMacroTarget` |
| §3.5 slot target + redistribution | `MacroCalculator.calculateSlotKcalTargets` |
| §3.6 filter candidates | `DishFilterService.filterCandidatesForSlot` |
| §3.6 weight constraint | `BruteForceEngine` (check khi enumerate serving) |
| §3.7.1 brute force | `BruteForceEngine.findTopK` |
| §3.7.2 macro score | `ScoringService.computeMacroScore` |
| §3.7.3 penalty 3-tier | `PenaltyService.computePenalty` |
| §3.7.4 final score | `BruteForceEngine` (composite trong loop) |
| §4.1 determine remaining meals | `RecommendationOrchestrator` (private method) |
| §4.2 sequential gen | `RecommendationOrchestrator.recommendFullDay` |
| §4.3 meal logging MVP | `MealLogService.confirmMeal` |
| §6 re-optimize on swap | `RecommendationOrchestrator.recommendSingleMeal` + suggestion logic |

### B. Convention test data cho dev

Để test penalty 3-tier mà chưa có user thực, agent có thể tạo script insert manual vào MySQL (KHÔNG cho vào `data.sql`):

```sql
-- Test fixture: user-test-1 đã ăn 3 ngày gần nhất
INSERT INTO meal_log (id, user_id, meal_date, meal_type, plan_type, goal_code,
    meal_kcal_target, total_kcal_actual, total_protein_g, total_fat_g, total_carb_g,
    final_score, status, created_at, updated_at) VALUES
('test-log-1', 'user-test-1', CURDATE() - INTERVAL 1 DAY, 'TRUA', '3_BUA', 'GIAM',
 800, 790, 50, 25, 80, 85.0, 'SUGGESTED', NOW(), NOW());

-- Thêm dish vào bữa đó: Gà luộc + Rau muống xào + Cơm trắng
INSERT INTO meal_log_dish (id, meal_log_id, dish_id, food_group_code, slot_code,
    serving_multiplier, actual_grams, dish_kcal, sort_order, created_at) VALUES
('test-dish-1', 'test-log-1', '28cc58b1-0675-45eb-b663-0adb726c93b0', 'GIA_CAM', 'CHINH',
 1.0, 100, 215, 1, NOW());
```

→ Khi gen bữa trưa hôm nay, nếu thuật toán đề xuất "Gà luộc" thì penalty Layer 1 (cùng ngày? KHÔNG, đây là 1 ngày trước → penalty = 6) phải xuất hiện. Log debug nên in `dish_id=... penalty=6.0 distance=1day`.

### C. Câu hỏi thường gặp

**Q: Tại sao không dùng PostgreSQL CTE / window function cho top-K?**
A: MVP. Brute force trong app layer đủ nhanh cho ≤5 món, code dễ đọc cho thesis defense, không lock-in PostgreSQL.

**Q: Có nên cache LoadedConfigs với `@Cacheable`?**
A: KHÔNG ở MVP. Lý do: thêm Spring Cache + Redis dependency, phức tạp. Mỗi request load configs ~6 query đơn giản (<5ms tổng). Khi nào throughput thực >100 req/s mới optimize.

**Q: BigDecimal scale cố định bao nhiêu?**
A: 
- Macro grams: scale=2 (vd 25.50g)
- Kcal: scale=2 (vd 495.00)
- Ratios: scale=2 (vd 0.35)
- Serving multiplier: scale=2 (vd 1.25)
- Intermediate computation: scale=4, sau đó round final HALF_UP.
- Score: scale=2.

---

## VERSION HISTORY

| Ngày | Phiên bản | Thay đổi |
|---|---|---|
| 22/05/2026 | v1.0 | Phần 1 (Backend: entity + core logic + API) |
| (TBD) | v1.1 | Bổ sung Phần 2 (Frontend) |
| (TBD) | v1.2 | Bổ sung Phần 3 (Edge cases + polish) |
