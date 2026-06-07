# Hướng dẫn BE — Trang Tổng quan Admin (Tầng 1)

> File hướng dẫn cho Claude Code thực thi trong terminal. Tự chứa, không cần đọc thêm tài liệu khác.
> **Repo:** `HealthManagement` — **Nhánh làm việc:** `TichHopModelML`
> **Ngôn ngữ:** giải thích tiếng Việt; code/định danh tiếng Anh; comment trong code dùng tiếng Việt **không dấu** (theo convention dự án).

---

## 1. Mục tiêu

Trang Tổng quan admin (Tầng 1) cần 3 nhóm số liệu: 4 thẻ thống kê, biểu đồ độ phủ kho món theo slot, và panel chất lượng dữ liệu. Để cấp dữ liệu cho FE, ta thêm **2 endpoint thống kê** (read-only) và **1 route gateway**:

1. `GET /api/admin/dashboard/catalog-stats` (nutrition-service) — số liệu kho món/nguyên liệu/meal-log.
2. `GET /api/admin/users/stats` (user-service) — số người dùng + số đã hoàn thiện hồ sơ.

**Nguyên tắc quan trọng (chống over-engineering):** KHÔNG tạo service tổng hợp (aggregator) gọi chéo giữa các service. Mỗi service tự trả phần số liệu của mình; FE gọi 2 endpoint và ghép lại. Đây là endpoint **thống kê đọc-thuần**, dùng `COUNT`/`GROUP BY`, không cần phân trang, không cần ghi.

---

## 2. Bối cảnh code đã có (KHÔNG cần sửa, chỉ để bám đúng)

- `DataResponse<T>` ở package `org.example.web.dto.response` (module `common`). Dùng `DataResponse.success(data)` → trả `{code:null, message:"Success", data}`. Đây là pattern chuẩn của mọi controller.
- nutrition-service đã bật method security: `@EnableMethodSecurity(prePostEnabled = true)` trong `config/SecurityConfig.java`, rule `.anyRequest().authenticated()`. Controller `AdminConfigController` đã chạy với `@PreAuthorize("hasRole('ADMIN')")` → xác thực ADMIN end-to-end qua gateway **đã hoạt động**, ta chỉ việc dùng lại annotation đó.
- user-service: `AdminUserController` (`/api/admin/users`) đã có `@PreAuthorize("hasRole('ADMIN')")` ở cấp class.
- Entity (package `org.example.nutritionservice.entity.catalog`):
  - `Dish`: có `Boolean isActive`, `SlotCode slotCode`. `SlotCode` enum = `CHINH, RAU, TINH_BOT, COMBO, BUA_PHU`.
  - `Ingredient`: có `BigDecimal kcalPer100g` (**nullable** — 52/203 dòng đang NULL).
- Repository đã có:
  - `repository/catalog/DishRepository` extends `JpaRepository<Dish, String>` → `count()` sẵn có.
  - `repository/meallog/MealLogRepository` extends `JpaRepository<MealLog, String>` → `count()` sẵn có.
  - **CHƯA có** `IngredientRepository` → cần tạo mới (read/count thuần, sẽ tái dùng cho CRUD nguyên liệu sau này).
- user-service: `repository/UserRepository` extends `JpaRepository<User, String>`. `User` có `birthDate`, `gender` (enum `Gender = MALE/FEMALE/OTHER`). Định nghĩa "đã hoàn thiện hồ sơ" dùng **giống màn Danh sách người dùng hiện tại**: `birthDate != null AND gender != null` (để số liệu nhất quán giữa 2 màn).
- Gateway (`api-gateway/src/main/resources/application.yml`) định tuyến theo path cụ thể, đã có `admin-configs` (→nutrition) và `admin-users` (→user). Mỗi route gắn filter `JwtFilter`. **Chưa có** route cho `/api/admin/dashboard/**`.

**Số liệu mong đợi sau khi seed (dùng để verify):** 142 món, 203 nguyên liệu (→ 151 có macro, 52 thiếu), 757 liên kết. `dishActive` = tổng các slot trong `dishCountBySlot`.

---

## 3. Hợp đồng API (response mẫu)

**3.1.** `GET /api/admin/dashboard/catalog-stats`
```json
{
  "code": null,
  "message": "Success",
  "data": {
    "dishTotal": 142,
    "dishActive": 138,
    "ingredientTotal": 203,
    "ingredientWithMacro": 151,
    "mealLogTotal": 1284,
    "dishCountBySlot": { "CHINH": 45, "RAU": 33, "TINH_BOT": 20, "COMBO": 32, "BUA_PHU": 12 }
  }
}
```
> `dishCountBySlot` **chỉ đếm món đang active**, và **luôn đủ 5 key** (slot không có món = 0).

**3.2.** `GET /api/admin/users/stats`
```json
{ "code": null, "message": "Success", "data": { "totalUsers": 48, "usersWithProfile": 31 } }
```

---

## 4. Thay đổi chi tiết — nutrition-service

### 4.1. Tạo `IngredientRepository` (mới)

File: `nutrition-service/src/main/java/org/example/nutritionservice/repository/catalog/IngredientRepository.java`

```java
package org.example.nutritionservice.repository.catalog;

import org.example.nutritionservice.entity.catalog.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, String> {

    // Dem nguyen lieu da co du lieu dinh duong (coi kcal NULL la chua co macro)
    long countByKcalPer100gIsNotNull();
}
```

### 4.2. Bổ sung `DishRepository` (sửa file có sẵn)

Thêm 2 method vào interface `repository/catalog/DishRepository.java` (giữ nguyên các method cũ). Bảo đảm đã có sẵn các import `org.springframework.data.jpa.repository.Query` và `java.util.List` (file hiện đã import cả hai).

```java
    // Dem mon dang active (cho stat card + panel chat luong)
    long countByIsActiveTrue();

    // Dem so mon active theo tung slot (cho bieu do do phu kho mon)
    // Tra ve list cac cap [SlotCode, Long]
    @Query("SELECT d.slotCode, COUNT(d) FROM Dish d WHERE d.isActive = TRUE GROUP BY d.slotCode")
    List<Object[]> countActiveBySlot();
```

### 4.3. Tạo DTO `CatalogStatsResponse` (mới)

File: `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/CatalogStatsResponse.java`

```java
package org.example.nutritionservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CatalogStatsResponse {
    private long dishTotal;
    private long dishActive;
    private long ingredientTotal;
    private long ingredientWithMacro;
    private long mealLogTotal;
    // key = SlotCode.name(); luon du 5 slot (slot khong co mon = 0)
    private Map<String, Long> dishCountBySlot;
}
```

### 4.4. Tạo service `DashboardStatsService` (mới)

> Cố ý dùng **một class @Service cụ thể, không tách interface/impl**: đây là tổng hợp số liệu đọc-thuần, interface không thêm giá trị. Đây là lựa chọn có chủ đích để gọn, không phải thiếu sót.

File: `nutrition-service/src/main/java/org/example/nutritionservice/service/dashboard/DashboardStatsService.java`

```java
package org.example.nutritionservice.service.dashboard;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.response.CatalogStatsResponse;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.example.nutritionservice.repository.catalog.DishRepository;
import org.example.nutritionservice.repository.catalog.IngredientRepository;
import org.example.nutritionservice.repository.meallog.MealLogRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private final DishRepository dishRepository;
    private final IngredientRepository ingredientRepository;
    private final MealLogRepository mealLogRepository;

    public CatalogStatsResponse getCatalogStats() {
        // Khoi tao map dam bao du 5 slot theo thu tu enum, slot khong co mon = 0
        Map<String, Long> bySlot = new LinkedHashMap<>();
        for (SlotCode s : SlotCode.values()) {
            bySlot.put(s.name(), 0L);
        }
        for (Object[] row : dishRepository.countActiveBySlot()) {
            SlotCode slot = (SlotCode) row[0];
            long count = ((Number) row[1]).longValue(); // dung Number cho an toan kieu
            bySlot.put(slot.name(), count);
        }

        return CatalogStatsResponse.builder()
                .dishTotal(dishRepository.count())
                .dishActive(dishRepository.countByIsActiveTrue())
                .ingredientTotal(ingredientRepository.count())
                .ingredientWithMacro(ingredientRepository.countByKcalPer100gIsNotNull())
                .mealLogTotal(mealLogRepository.count())
                .dishCountBySlot(bySlot)
                .build();
    }
}
```

### 4.5. Tạo controller `AdminDashboardController` (mới)

File: `nutrition-service/src/main/java/org/example/nutritionservice/controller/admin/AdminDashboardController.java`

```java
package org.example.nutritionservice.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.nutritionservice.dto.response.CatalogStatsResponse;
import org.example.nutritionservice.service.dashboard.DashboardStatsService;
import org.example.web.dto.response.DataResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardStatsService dashboardStatsService;

    @GetMapping("/catalog-stats")
    public DataResponse<CatalogStatsResponse> getCatalogStats() {
        return DataResponse.success(dashboardStatsService.getCatalogStats());
    }
}
```

---

## 5. Thay đổi chi tiết — user-service

### 5.1. Bổ sung `UserRepository` (sửa file có sẵn)

Thêm vào interface `repository/UserRepository.java` (giữ nguyên các method cũ; `count()` đã có sẵn từ `JpaRepository`):

```java
    // Dem user da hoan thien ho so (dinh nghia giong man Danh sach: co birthDate VA gender)
    @Query("SELECT COUNT(u) FROM User u WHERE u.birthDate IS NOT NULL AND u.gender IS NOT NULL")
    long countWithProfile();
```

### 5.2. Tạo DTO `UserStatsResponse` (mới)

File: `user-service/src/main/java/org/example/userservice/dto/response/UserStatsResponse.java`

```java
package org.example.userservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatsResponse {
    private long totalUsers;
    private long usersWithProfile;
}
```

### 5.3. Bổ sung endpoint vào `AdminUserController` (sửa file có sẵn)

Thêm import:
```java
import org.example.userservice.dto.response.UserStatsResponse;
```

Thêm method sau (nên đặt **phía trên** `getUserDetail(...)` cho dễ đọc):
```java
    @GetMapping("/stats")
    public DataResponse<UserStatsResponse> getUserStats() {
        return DataResponse.success(UserStatsResponse.builder()
                .totalUsers(userRepository.count())
                .usersWithProfile(userRepository.countWithProfile())
                .build());
    }
```

> **Lưu ý định tuyến nội bộ:** `AdminUserController` đã có `@GetMapping("/{userId}")`. Spring (PathPatternParser) ưu tiên path **literal** `/stats` hơn path biến `/{userId}`, nên `/api/admin/users/stats` được map đúng vào `getUserStats()`, KHÔNG bị nuốt thành `userId="stats"`. Không cần lo thứ tự, nhưng đặt trên cho dễ nhìn.

---

## 6. Thay đổi gateway — thêm route cho dashboard

`api-gateway/src/main/resources/application.yml`: thêm route mới **ngay sau** route `admin-users`. Endpoint user-stats đã được route `admin-users` (`/api/admin/users/**`) phủ sẵn nên KHÔNG cần route riêng; chỉ catalog-stats cần route mới:

```yaml
        - id: admin-dashboard
          uri: lb://nutrition-service
          predicates:
            - Path=/api/admin/dashboard/**
          filters:
            - name: JwtFilter
              args: { }
```

> Bắt buộc giữ filter `JwtFilter` giống các route admin khác để token được xử lý đồng nhất.

---

## 7. Build & verify

1. Compile (chạy ở thư mục gốc repo):
   ```bash
   ./mvnw -q -pl common,nutrition-service,user-service,api-gateway -am compile
   ```
   Yêu cầu: build sạch, không lỗi.

2. Khởi động theo thứ tự: `discovery-server` → `api-gateway` → `nutrition-service` → `user-service` (qua IDE hoặc `./mvnw spring-boot:run` từng module). MySQL container phải đang chạy và đã seed `dish_seed.sql`.

3. Lấy access token của tài khoản ADMIN (đăng nhập qua `/api/auth/...`), rồi gọi (thay `$TOKEN`, `$GW` = `http://localhost:8080`):
   ```bash
   curl -s -H "Authorization: Bearer $TOKEN" $GW/api/admin/dashboard/catalog-stats | jq
   curl -s -H "Authorization: Bearer $TOKEN" $GW/api/admin/users/stats | jq
   ```

4. Tiêu chí PASS:
   - `catalog-stats`: `ingredientTotal == 203`, `ingredientWithMacro == 151`, `dishTotal == 142`; `dishCountBySlot` đủ 5 key; **tổng các giá trị trong `dishCountBySlot` == `dishActive`** (sanity check quan trọng).
   - `users/stats`: `totalUsers >= usersWithProfile >= 0`.
   - Gọi 2 endpoint **không kèm token ADMIN** → trả 401/403 (xác nhận được bảo vệ).

---

## 8. KHÔNG được làm (tránh sai hướng)

- KHÔNG tạo service/endpoint tổng hợp gọi chéo user-service ↔ nutrition-service. Hai endpoint độc lập, FE tự ghép.
- KHÔNG đổi `ddl-auto`, KHÔNG thêm Flyway, KHÔNG đụng các bảng config đang ổn định.
- `dishCountBySlot` chỉ đếm món **active** — không đếm món đã ẩn.
- KHÔNG thêm phân trang/lọc cho 2 endpoint này; chúng là số tổng hợp.
- Phạm vi guide này CHỈ là dashboard Tầng 1. KHÔNG động tới CRUD món/nguyên liệu (làm ở guide khác).

---

## 9. Commit gợi ý (tiếng Việt)

```
feat(nutrition): them endpoint catalog-stats cho dashboard admin
feat(user): them endpoint user stats cho dashboard admin
feat(gateway): them route /api/admin/dashboard cho nutrition-service
```
