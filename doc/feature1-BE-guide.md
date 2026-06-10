# Feature 1 — Xem thực đơn "Ngày mai" (BE)

> **Repo:** `HealthManagement`, service `nutrition-service`. Chạy trong session Agent Code mở tại repo BE.
> **Làm BE TRƯỚC FE** (FE phụ thuộc tham số mới).
> **Phạm vi (Phương án 1):** chỉ thêm khả năng xem **trọn ngày mai**. Hành vi **"Hôm nay" GIỮ NGUYÊN** (buổi tối vẫn chỉ trả bữa còn lại theo giờ, 21h–5h vẫn tự nhảy sang ngày mai). KHÔNG làm consolidation / không đụng scoring.

## Ý tưởng

Thêm 1 tham số optional `planDay` (`TODAY` | `TOMORROW`) vào request đề xuất. Khi `TOMORROW` → bỏ qua bộ lọc theo-giờ + lọc-bữa-đã-log, trả **trọn ngày mai** (tất cả các bữa theo planType). Khi vắng/`TODAY` → hành vi cũ y nguyên.

| File | Hành động |
|---|---|
| `dto/request/RecommendFullDayRequest.java` | Thêm field `planDay` (String, `@Pattern`) |
| `domain/recommendation/UserContext.java` | Thêm field `planDay` |
| `service/recommendation/RecommendationApiService.java` | Truyền `planDay` vào UserContext + sửa `planDate` của nhánh cảnh báo |
| `service/recommendation/RecommendationOrchestrator.java` | Thêm nhánh TOMORROW vào `determineRemainingMeals` |

---

## Bước 1 — `RecommendFullDayRequest`: thêm `planDay`

File `dto/request/RecommendFullDayRequest.java`. Thêm field sau (đặt cạnh `forceCompute`). **Optional** — `@Pattern` chỉ validate khi có giá trị, null sẽ được hiểu là TODAY:

```java
    @Pattern(regexp = "TODAY|TOMORROW")
    private String planDay; // null/TODAY = hanh vi hien tai; TOMORROW = tron ngay mai
```

`@Pattern` đã được import sẵn trong file này.

## Bước 2 — `UserContext`: thêm `planDay`

File `domain/recommendation/UserContext.java`. Thêm field (đặt cạnh `forceCompute`). Lombok tự sinh `getPlanDay()`:

```java
    private String planDay;
```

## Bước 3 — `RecommendationApiService.recommendFullDay`: truyền `planDay`

File `service/recommendation/RecommendationApiService.java`, trong method `recommendFullDay`.

**3a.** Thêm `.planDay(...)` vào builder `UserContext` (đặt cạnh `.forceCompute(...)`):

```java
        UserContext userContext = UserContext.builder()
                .userId(userId)
                .tdee(request.getTdee())
                .goalCode(request.getGoalCode())
                .planType(request.getPlanType())
                .perMealConfigs(toPerMealConfigs(request))
                .requestTime(LocalDateTime.now())
                .forceCompute(request.isForceCompute())
                .planDay(request.getPlanDay())
                .build();
```

**3b.** (Sửa nhỏ cho đúng ngày) Nhánh cảnh báo thể trạng (early-return) đang hardcode `planDate(LocalDate.now())`. Khi xem ngày mai, ngày hiển thị nên là ngày mai. Thay đoạn early-return:

```java
        DailyPlanResponse.WarningResponse warning = warningFor(request.getConstitution(), request.getGoalCode());
        if (warning != null && warning.isRequireConfirm() && !request.isConstitutionConfirmed()) {
            LocalDate planDate = "TOMORROW".equalsIgnoreCase(request.getPlanDay())
                    ? LocalDate.now().plusDays(1)
                    : LocalDate.now();
            return DailyPlanResponse.builder()
                    .planDate(planDate)
                    .goalCode(request.getGoalCode())
                    .planType(request.getPlanType())
                    .warning(warning)
                    .meals(List.of())
                    .build();
        }
```

> `planDate` của luồng bình thường đã đúng sẵn — `toDailyPlanResponse` dùng `dailyPlan.getPlanDate()` (= ngày mai khi TOMORROW), không cần sửa.

## Bước 4 — `RecommendationOrchestrator.determineRemainingMeals`: nhánh TOMORROW

File `service/recommendation/RecommendationOrchestrator.java`. Thêm nhánh TOMORROW vào **đầu** method (ngay sau dòng tính `requestTime`, trước nhánh `if (requestTime.getHour() >= 21 ...)`):

```java
    private PlanWindow determineRemainingMeals(UserContext userCtx, LoadedConfigs configs) {
        LocalDateTime requestTime = requestTime(userCtx);

        // Xem ngay mai: tron ngay, bo qua loc theo gio va loc bua da log (ngay mai chua co log).
        if ("TOMORROW".equalsIgnoreCase(userCtx.getPlanDay())) {
            return new PlanWindow(requestTime.toLocalDate().plusDays(1), orderedMealTypes(configs));
        }

        if (requestTime.getHour() >= 21 || requestTime.getHour() < 5) {
            return new PlanWindow(requestTime.toLocalDate().plusDays(1), orderedMealTypes(configs));
        }

        LocalDate today = requestTime.toLocalDate();
        Set<MealType> loggedMeals = mealLogRepository.findByUserIdAndMealDate(userCtx.getUserId(), today)
                .stream()
                .map(MealLog::getMealType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(MealType.class)));
        Set<MealType> eligibleMeals = eligibleMealsFromHour(requestTime.getHour());
        List<MealType> remaining = orderedMealTypes(configs).stream()
                .filter(eligibleMeals::contains)
                .filter(mealType -> !loggedMeals.contains(mealType))
                .toList();
        return new PlanWindow(today, remaining);
    }
```

Phần còn lại của `recommendFullDay` không đổi: nó vẫn dùng `planWindow.planDate()` cho `DailyPlan.planDate` và lịch sử phạt (`loadHistory(..., planWindow.planDate(), lookback)`), nên khi TOMORROW, cửa sổ phạt sẽ bao gồm hôm nay → ngày mai tự tránh lặp món hôm nay.

---

## Bước 5 — Build & smoke test

```bash
mvn -q -pl nutrition-service spring-boot:run   # hoac cach ban van chay
```

Lấy một `user_id`, gọi thẳng nutrition-service (thay `<PORT>`, `<USER_ID>`). Body tối thiểu giống request hiện tại, chỉ thêm `planDay`:

```bash
# TOMORROW -> planDate = ngay mai, tra DU cac bua theo planType, bat ke gio hien tai
curl -X POST "http://localhost:<PORT>/api/recommendation/full-day" \
  -H "userId: <USER_ID>" -H "Content-Type: application/json" \
  -d '{
    "tdee": 2100, "goalCode": "GIAM", "planType": "3_BUA", "constitution": "CAN_DOI",
    "constitutionConfirmed": true,
    "perMealConfig": {
      "SANG": {"mealKind":"SIMPLE","nMain":1,"nRau":0,"nCarb":1},
      "TRUA": {"mealKind":"FULL","nMain":1,"nRau":1,"nCarb":1},
      "TOI":  {"mealKind":"FULL","nMain":1,"nRau":1,"nCarb":1}
    },
    "planDay": "TOMORROW"
  }'
```

Kỳ vọng:
- `data.planDate` = **ngày mai**.
- `data.meals` chứa **đủ các bữa** của planType (vd 3 bữa cho `3_BUA`), không bị cắt theo giờ.
- Gọi lại **không có** `planDay` (hoặc `"TODAY"`) vào buổi tối → vẫn chỉ trả bữa còn lại theo giờ (hành vi cũ y nguyên).

## Contract cho FE

- `POST /api/recommendation/full-day` nhận thêm field optional `planDay`: `"TODAY"` | `"TOMORROW"`. Vắng = như `"TODAY"`.
- `"TOMORROW"` → response `planDate` là ngày mai, `meals` là trọn ngày.
- Không đổi gì khác trong response.
