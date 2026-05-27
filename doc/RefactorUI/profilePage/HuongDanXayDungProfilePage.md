# HƯỚNG DẪN XÂY DỰNG TRANG USER PROFILE

> **Mục tiêu:** Build trang `/profile` cho user view/edit thông tin sau khi đã onboard. Bao gồm cả BE (Change Password endpoint + startWeight migration) và FE (6 section theo design Claude Design).
>
> **Audience:** Code agent (cần hiểu cả BE Java + FE React).
>
> **Tổng workload:** ~8-10h (BE ~2-3h, FE ~6-7h).
>
> **Reference design:**
> - `ProfileV1.html` (entry, 21 artboards)
> - `profile-sharedV1.jsx` (shared atoms)
> - `profile-sectionsV1.jsx` (6 sections)
> - `profile-modalsV1.jsx` (modals + toast + skeleton)
> - `profile-pageV1.jsx` (page composer)

---

## 0. CONTEXT & SCOPE QUYẾT ĐỊNH

### 0.1. Scope MVP đã chốt (user confirm)

| Section | Quyết định |
|---|---|
| §1 Header | ✅ Build đầy đủ (Avatar + name + email + joined + ConstitutionPill) |
| §2 Personal Info | ✅ Build view + edit mode (4 field: name, birthDate, gender, phone) |
| §3 Goal | ✅ Build + **bonus** progress bar (cần BE add `start_weight_kg`) + history collapsible |
| §4 Health Settings | ✅ Build toggle PBF method (FORMULA/MODEL_1) |
| §5 Security (Change Password) | ✅ **BUILD MỚI** — BE + FE |
| §6 Danger Zone (Delete Account) | ⏸ **DEFER** — UI placeholder, button bấm chỉ toast "Tính năng đang phát triển, vui lòng liên hệ admin" |

### 0.2. Phương án startWeight: **A** — BE migration

- Thêm column `start_weight_kg DECIMAL(5,2)` vào `user_goals` table
- Khi user set goal mới qua `PUT /api/user-goals/current`, snapshot cân nặng hiện tại từ health-data-service
- Implement bằng **RestTemplate sync call** với fallback null nếu service down

### 0.3. State hiện tại (đã verify clone repo)

**BE (`HealthManagement` branch `feature/onboarding-dashboard-be`):**
- ✅ `BCryptPasswordEncoder` configured trong `SecurityConfig.java`
- ✅ `AuthServiceImpl` dùng pattern `passwordEncoder.encode()` + `passwordEncoder.matches()`
- ✅ `AuthRepository.findByUsername()` returning `Optional<Auth>`
- ✅ ErrorCode `AUTH-001..010` đã có
- ✅ Pattern controller: `@RequestHeader("userId")` từ gateway
- ✅ `UserGoal` entity với `targetWeightKg` đã có, chỉ thiếu `startWeightKg`
- ❌ Cross-service call mechanism CHƯA CÓ — phải build mới

**FE (`Health-management-frontend` branch `feature/onboarding-dashboard-fe`):**
- ✅ `apiClient` (axios) auto-attach JWT từ `services/axios.ts`
- ✅ `AuthContext` có `refreshUser()` method
- ✅ `useAuth().user` có fields: `userId, username, name, birthDate, gender, phone, profileCompleted`
- ✅ Tailwind v4 đã work với brand tokens
- ✅ Existing services: `auth.service.ts`, `userGoals.service.ts`, `userPreferences.service.ts`, `constitution.service.ts`, `dashboard.service.ts`, `mealLog.service.ts`
- ❌ Chưa có route `/profile` trong App.tsx
- ❌ Chưa có `profile.service.ts`, `password.service.ts`

### 0.4. Convention bắt buộc

1. **Code comment + commit message:** tiếng Anh (industry standard)
2. **UI copy + toast message:** tiếng Việt
3. **Header userId:** `@RequestHeader("userId")` cho user-service (KHÔNG phải `X-User-Id`)
4. **Header userId:** `X-User-Id` cho nutrition-service (đã có cross-service)
5. **Password security:** KHÔNG log password ra console, KHÔNG return password trong response
6. **Sample data trong design source:** KHÔNG copy nguyên — bind với API thực

---

# PHẦN A — BACKEND (~2-3h)

## §A1 — Migration: Add `start_weight_kg` to `user_goals` (~30 phút)

### A1.1. Update UserGoal entity

**File:** `user-service/src/main/java/org/example/userservice/entity/UserGoal.java`

**Thêm field sau `targetWeightKg`:**

```java
@Column(name = "target_weight_kg", precision = 5, scale = 2)
private BigDecimal targetWeightKg;

// === ADD THIS ===
@Column(name = "start_weight_kg", precision = 5, scale = 2)
private BigDecimal startWeightKg;
// === END ADD ===

@Column(name = "target_duration_months")
private Integer targetDurationMonths;
```

**Lý do:** Cần snapshot cân nặng tại thời điểm bắt đầu goal để tính progress bar. Hibernate `ddl-auto=update` sẽ tự ADD COLUMN. Field nullable cho row cũ.

### A1.2. Update UserGoalResponse DTO

**File:** `user-service/src/main/java/org/example/userservice/dto/response/UserGoalResponse.java`

**Thêm field tương ứng:**

```java
private BigDecimal targetWeightKg;
private BigDecimal startWeightKg;  // ← ADD
private Integer targetDurationMonths;
```

### A1.3. Update `toResponse()` mapping trong UserGoalServiceImpl

**File:** `user-service/src/main/java/org/example/userservice/service/UserGoalServiceImpl.java`

**Tìm method `toResponse(UserGoal goal)`, thêm dòng:**

```java
private UserGoalResponse toResponse(UserGoal goal) {
    return UserGoalResponse.builder()
            .id(goal.getId())
            .userId(goal.getUserId())
            .goalCode(goal.getGoalCode())
            .startDate(goal.getStartDate())
            .endDate(goal.getEndDate())
            .isActive(goal.getIsActive())
            .targetWeightKg(goal.getTargetWeightKg())
            .startWeightKg(goal.getStartWeightKg())   // ← ADD
            .targetDurationMonths(goal.getTargetDurationMonths())
            .note(goal.getNote())
            .createdAt(goal.getCreatedAt())
            .build();
}
```

---

## §A2 — Cross-service call: snapshot weight (~1h)

### A2.1. Tạo RestTemplate config

**File mới:** `user-service/src/main/java/org/example/userservice/config/RestTemplateConfig.java`

```java
package org.example.userservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate bean for cross-service sync calls.
 *
 * Timeouts kept short (2s connect / 3s read) so that failures in downstream
 * services do not block user-facing requests for long. Callers MUST handle
 * exceptions gracefully (return null / fallback value).
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate healthDataRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }
}
```

### A2.2. Tạo HealthDataClient service

**File mới:** `user-service/src/main/java/org/example/userservice/service/HealthDataClient.java`

```java
package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight HTTP client to call health-data-service.
 *
 * MVP scope: only need to fetch current weight to snapshot start_weight_kg
 * when user sets a new goal. All failures are logged and return null so the
 * goal update still succeeds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthDataClient {

    private final RestTemplate healthDataRestTemplate;

    @Value("${app.services.health-data.url:http://health-data-service:8082}")
    private String healthDataBaseUrl;

    /**
     * Fetch current weight (kg) for the given userId. Returns null on any
     * failure (network, 404, missing data) so callers can fall back gracefully.
     */
    @SuppressWarnings("unchecked")
    public BigDecimal fetchCurrentWeightKg(String userId) {
        String url = healthDataBaseUrl + "/api/health-data/dashboard-metrics";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("userId", userId);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = healthDataRestTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class
            );

            if (response.getBody() == null) return null;

            Map<String, Object> body = response.getBody();

            // Response shape: { code, message, data: { weight: { value, unit, ... } } }
            Object dataObj = body.get("data");
            if (!(dataObj instanceof Map)) return null;

            Map<String, Object> data = (Map<String, Object>) dataObj;
            Object weightObj = data.get("weight");
            if (!(weightObj instanceof Map)) return null;

            Map<String, Object> weight = (Map<String, Object>) weightObj;
            Object valueObj = weight.get("value");
            if (valueObj == null) return null;

            return new BigDecimal(valueObj.toString());
        } catch (Exception e) {
            log.warn("Failed to fetch current weight for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }
}
```

### A2.3. Wire snapshot into `UserGoalServiceImpl.updateCurrent()`

**File:** `user-service/src/main/java/org/example/userservice/service/UserGoalServiceImpl.java`

**Thêm dependency:**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserGoalServiceImpl implements UserGoalService {

    private final UserGoalRepository repo;
    private final HealthDataClient healthDataClient;  // ← ADD

    // ...
}
```

**Update `updateCurrent()`:**

```java
@Override
@Transactional
public UserGoalResponse updateCurrent(String userId, UpdateGoalRequest req) {
    LocalDate today = LocalDate.now();
    repo.deactivateCurrentGoal(userId, today);

    // Snapshot current weight for progress tracking (null-safe).
    BigDecimal startWeight = healthDataClient.fetchCurrentWeightKg(userId);
    if (startWeight == null) {
        log.info("No current weight available for userId={}, startWeightKg will be null", userId);
    }

    UserGoal newGoal = UserGoal.builder()
            .id(UuidV7Generator.generate())
            .userId(userId)
            .goalCode(req.getGoalCode())
            .startDate(today)
            .endDate(null)
            .isActive(true)
            .targetWeightKg(req.getTargetWeightKg())
            .startWeightKg(startWeight)   // ← ADD
            .targetDurationMonths(req.getTargetDurationMonths() != null ? req.getTargetDurationMonths() : 6)
            .note(req.getNote())
            .build();

    UserGoal saved = repo.save(newGoal);
    log.info("Updated goal for userId {}: {}, startWeight={}", userId, req.getGoalCode(), startWeight);
    return toResponse(saved);
}
```

**Lưu ý:** `startWeight` có thể null nếu:
- Health-data-service đang down
- User chưa submit cân nặng nào (edge case — không nên xảy ra vì onboarding bắt nhập)
- Network timeout

FE phải handle null case (hiển thị "Chưa có dữ liệu khởi điểm" trong progress bar).

---

## §A3 — Change Password endpoint (~1h)

### A3.1. Add ErrorCode

**File:** `common/src/main/java/org/example/web/exception/ErrorCode.java`

**Thêm sau dòng `JWT_EXPIRED`:**

```java
JWT_EXPIRED             ("AUTH-010", HttpStatus.UNAUTHORIZED, "JWT da het han"),

// === ADD ===
CHANGE_PASSWORD_WRONG_CURRENT ("AUTH-011", HttpStatus.BAD_REQUEST, "Mat khau hien tai khong dung"),
CHANGE_PASSWORD_SAME          ("AUTH-012", HttpStatus.BAD_REQUEST, "Mat khau moi khong duoc trung mat khau cu"),
// === END ADD ===

EVENT_PUBLISH_FAILED    ("AUTH-500", HttpStatus.INTERNAL_SERVER_ERROR, "Khong the publish event"),
```

### A3.2. Tạo ChangePasswordRequest DTO

**File mới:** `user-service/src/main/java/org/example/userservice/dto/request/ChangePasswordRequest.java`

```java
package org.example.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Mat khau hien tai khong duoc bo trong")
    private String currentPassword;

    @NotBlank(message = "Mat khau moi khong duoc bo trong")
    @Size(min = 8, max = 100, message = "Mat khau moi phai tu 8 den 100 ky tu")
    private String newPassword;
}
```

### A3.3. Update AuthService interface

**File:** `user-service/src/main/java/org/example/userservice/service/AuthService.java`

**Thêm method:**

```java
public interface AuthService {
    // ... existing methods ...

    void changePassword(String userId, ChangePasswordRequest request);
}
```

### A3.4. Implement changePassword in AuthServiceImpl

**File:** `user-service/src/main/java/org/example/userservice/service/AuthServiceImpl.java`

**Thêm method (cuối class):**

```java
@Override
@Transactional
public void changePassword(String userId, ChangePasswordRequest request) {
    // 1. Find user's auth record via User -> Auth relation.
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    Auth auth = user.getAuth();
    if (auth == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    // 2. Verify current password.
    if (!passwordEncoder.matches(request.getCurrentPassword(), auth.getPassword())) {
        throw new BusinessException(ErrorCode.CHANGE_PASSWORD_WRONG_CURRENT);
    }

    // 3. Reject same password.
    if (passwordEncoder.matches(request.getNewPassword(), auth.getPassword())) {
        throw new BusinessException(ErrorCode.CHANGE_PASSWORD_SAME);
    }

    // 4. Encode and save.
    auth.setPassword(passwordEncoder.encode(request.getNewPassword()));
    authRepository.save(auth);

    log.info("Password changed successfully for userId={}", userId);
    // NOTE: do NOT log raw passwords.
}
```

**Verify trước:** `User` entity phải có quan hệ tới `Auth`. Nếu chưa có, mở `User.java` thêm:

```java
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "auth_id")
private Auth auth;
```

Nếu pattern hiện tại khác (vd Auth là owning side), agent điều chỉnh logic findByUser sao cho phù hợp.

### A3.5. Add endpoint to AuthController

**File:** `user-service/src/main/java/org/example/userservice/controller/AuthController.java`

**Thêm imports:**

```java
import org.example.userservice.dto.request.ChangePasswordRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
```

**Thêm endpoint:**

```java
@PutMapping("/change-password")
public ResponseEntity<DataResponse<Void>> changePassword(
        @RequestHeader("userId") String userId,
        @Valid @RequestBody ChangePasswordRequest request
) {
    authService.changePassword(userId, request);
    return ResponseEntity.ok(DataResponse.success());
}
```

### A3.6. Verify gateway routing

**File:** `api-gateway/src/main/resources/application.yml` (hoặc tương đương)

Verify route `/api/auth/**` đã forward tới user-service và JWT filter PASS tất cả endpoint trong `/api/auth/**` (vì login/register cần không-auth, change-password cần auth).

Pattern khuyến nghị:
- `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh-token` → bypass JWT filter
- `/api/auth/change-password` → require JWT filter

Nếu gateway hiện tại bypass cả `/api/auth/**`, agent BE cần update logic JwtFilter để cho phép change-password đi qua auth check. Cụ thể: thêm exception cho `change-password` path.

Tham khảo file `api-gateway/src/main/java/.../filter/JwtFilter.java` hoặc tương đương.

---

## §A4 — Test BE (~30 phút)

### A4.1. Build + run

```bash
cd HealthManagement
.\mvnw clean install -DskipTests
docker-compose up -d --build user-service
```

### A4.2. Postman test cases

**Test 1: Change password thành công**

```http
PUT http://localhost:8080/api/auth/change-password
Authorization: Bearer <valid_jwt>
Content-Type: application/json

{
  "currentPassword": "OldPass123",
  "newPassword": "NewPass456"
}

Expected: 200 OK { code: null, data: null, message: "..." }
```

**Test 2: Sai mật khẩu hiện tại**

```http
{
  "currentPassword": "WrongPass",
  "newPassword": "NewPass456"
}

Expected: 400 BAD_REQUEST { code: "AUTH-011", message: "Mat khau hien tai khong dung" }
```

**Test 3: Mật khẩu mới trùng mật khẩu cũ**

```http
{
  "currentPassword": "Pass123",
  "newPassword": "Pass123"
}

Expected: 400 BAD_REQUEST { code: "AUTH-012" }
```

**Test 4: Mật khẩu mới < 8 ký tự**

```http
{
  "currentPassword": "Pass123",
  "newPassword": "abc"
}

Expected: 400 BAD_REQUEST (validation error)
```

**Test 5: Set goal mới → check startWeightKg snapshot**

```http
PUT http://localhost:8080/api/user-goals/current
Authorization: Bearer <valid_jwt>
Content-Type: application/json

{
  "goalCode": "GIAM",
  "targetWeightKg": 65,
  "targetDurationMonths": 6
}

Expected: 200 OK với response data.startWeightKg = current_weight (vd 75.0)
```

Verify trong DB:

```sql
SELECT id, goal_code, start_weight_kg, target_weight_kg, start_date
FROM user_goals
WHERE user_id = '<userId>' AND is_active = true
ORDER BY start_date DESC LIMIT 1;
```

Kỳ vọng `start_weight_kg` có giá trị.

### A4.3. Test edge case startWeight null

**Scenario:** Stop health-data-service container, gọi `PUT /api/user-goals/current`.

```bash
docker stop health-data-service
```

Gọi update goal → response vẫn 200 OK, nhưng `startWeightKg = null` (graceful degrade).

```bash
docker start health-data-service
```

---

# PHẦN B — FRONTEND (~6-7h)

## §B1 — Service layer (~1h)

### B1.1. Tạo `password.service.ts`

**File mới:** `src/services/password.service.ts`

```typescript
import { apiClient } from './axios';
import type { DataResponse } from './apiResponse';

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

/**
 * Change current user's password. Token will remain valid (BE does not
 * invalidate JWT on password change in MVP).
 */
export const changePassword = async (payload: ChangePasswordPayload): Promise<void> => {
  await apiClient.put<DataResponse<void>>('/api/auth/change-password', payload);
};
```

### B1.2. Update `userGoals.service.ts` thêm `startWeightKg`

**File:** `src/services/userGoals.service.ts`

**Tìm interface `UserGoalResponse` và thêm field:**

```typescript
export interface UserGoalResponse {
  id: string;
  goalCode: GoalCode;
  startDate: string;
  endDate: string | null;
  isActive: boolean;
  targetWeightKg: number | null;
  startWeightKg: number | null;          // ← ADD
  targetDurationMonths: number | null;
  note: string | null;
}
```

**Thêm function `getGoalHistory()`:**

```typescript
export const getGoalHistory = async (): Promise<UserGoalResponse[]> => {
  const response = await apiClient.get<DataResponse<UserGoalResponse[]>>(
    '/api/user-goals/history'
  );
  return response.data.data;
};
```

**Verify BE endpoint `/api/user-goals/history` đã có chưa.** Nếu chưa, agent BE thêm method trong `UserGoalController` tương ứng (đã có `getHistory()` trong service rồi, chỉ thiếu controller endpoint nếu chưa expose).

### B1.3. Tạo `profile.service.ts` (aggregate)

**File mới:** `src/services/profile.service.ts`

```typescript
import { getCurrentGoal, getGoalHistory } from './userGoals.service';
import { getAllPreferences } from './userPreferences.service';
import { getDashboardMetrics } from './dashboard.service';
import { getConstitution } from './constitution.service';
import type { UserGoalResponse } from './userGoals.service';
import type { PreferenceResponse } from './userPreferences.service';
import type { ConstitutionResponse } from './constitution.service';
import type { DashboardMetricsResponse } from './dashboard.service';

export interface ProfileOverview {
  currentGoal: UserGoalResponse | null;
  goalHistory: UserGoalResponse[];
  preferences: PreferenceResponse[];
  metrics: DashboardMetricsResponse | null;
  constitution: ConstitutionResponse | null;
  errors: Partial<Record<
    'currentGoal' | 'goalHistory' | 'preferences' | 'metrics' | 'constitution',
    string
  >>;
}

const extractError = (reason: unknown, fallback: string): string => {
  if (reason instanceof Error) return reason.message;
  return fallback;
};

/**
 * Fetch all data needed by the Profile page in parallel.
 * Each failure is captured independently so the page can show partial data.
 */
export const getProfileOverview = async (): Promise<ProfileOverview> => {
  const [
    currentGoalResult,
    goalHistoryResult,
    preferencesResult,
    metricsResult,
    constitutionResult,
  ] = await Promise.allSettled([
    getCurrentGoal(),
    getGoalHistory(),
    getAllPreferences(),
    getDashboardMetrics(),
    getConstitution(),
  ]);

  return {
    currentGoal: currentGoalResult.status === 'fulfilled' ? currentGoalResult.value : null,
    goalHistory: goalHistoryResult.status === 'fulfilled' ? goalHistoryResult.value : [],
    preferences: preferencesResult.status === 'fulfilled' ? preferencesResult.value : [],
    metrics: metricsResult.status === 'fulfilled' ? metricsResult.value : null,
    constitution: constitutionResult.status === 'fulfilled' ? constitutionResult.value : null,
    errors: {
      ...(currentGoalResult.status === 'rejected' && {
        currentGoal: extractError(currentGoalResult.reason, 'Không tải được mục tiêu hiện tại'),
      }),
      ...(goalHistoryResult.status === 'rejected' && {
        goalHistory: extractError(goalHistoryResult.reason, 'Không tải được lịch sử mục tiêu'),
      }),
      ...(preferencesResult.status === 'rejected' && {
        preferences: extractError(preferencesResult.reason, 'Không tải được cài đặt'),
      }),
      ...(metricsResult.status === 'rejected' && {
        metrics: extractError(metricsResult.reason, 'Không tải được chỉ số'),
      }),
      ...(constitutionResult.status === 'rejected' && {
        constitution: extractError(constitutionResult.reason, 'Không tải được thể trạng'),
      }),
    },
  };
};
```

### B1.4. Tạo `user.service.ts` update profile

**Verify file `src/services/user.service.ts` có function `updateUserProfile`** (đã có sẵn từ onboarding). Nếu chưa có, thêm:

```typescript
export interface UpdateProfilePayload {
  name: string;
  birthDate: string;       // ISO yyyy-mm-dd
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  phone?: string | null;
}

export const updateUserProfile = async (payload: UpdateProfilePayload): Promise<void> => {
  await apiClient.put('/api/user/profile', payload);
};
```

---

## §B2 — Validation schemas (~30 phút)

**File mới:** `src/types/profile.schemas.ts`

```typescript
import { z } from 'zod';

// Reuse age helper from onboarding.schemas.ts
export function computeAge(birthDate: string): number {
  const today = new Date();
  const birth = new Date(birthDate);
  let age = today.getFullYear() - birth.getFullYear();
  const m = today.getMonth() - birth.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
  return age;
}

export const personalInfoSchema = z.object({
  name: z.string()
    .trim()
    .min(2, 'Tên phải có ít nhất 2 ký tự')
    .max(100, 'Tên tối đa 100 ký tự'),
  birthDate: z.string()
    .min(1, 'Vui lòng chọn ngày sinh')
    .refine((val) => {
      const age = computeAge(val);
      return age >= 13 && age <= 100;
    }, 'Tuổi phải từ 13 đến 100'),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER'], {
    required_error: 'Vui lòng chọn giới tính',
  }),
  phone: z.string()
    .optional()
    .or(z.literal(''))
    .refine(
      (val) => !val || /^0\d{9,10}$/.test(val),
      'Số điện thoại không hợp lệ (VD: 0912345678)'
    ),
});

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, 'Vui lòng nhập mật khẩu hiện tại'),
  newPassword: z.string()
    .min(8, 'Mật khẩu mới phải có ít nhất 8 ký tự')
    .max(100, 'Mật khẩu tối đa 100 ký tự')
    .regex(/[A-Z]/, 'Mật khẩu phải có ít nhất 1 chữ HOA')
    .regex(/[0-9]/, 'Mật khẩu phải có ít nhất 1 chữ số'),
  confirmPassword: z.string(),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: 'Xác nhận mật khẩu không khớp',
  path: ['confirmPassword'],
}).refine((data) => data.currentPassword !== data.newPassword, {
  message: 'Mật khẩu mới phải khác mật khẩu hiện tại',
  path: ['newPassword'],
});

export type PersonalInfoData = z.infer<typeof personalInfoSchema>;
export type ChangePasswordData = z.infer<typeof changePasswordSchema>;
```

---

## §B3 — Routing setup (~15 phút)

### B3.1. Add `/profile` route in App.tsx

**File:** `src/App.tsx`

**Imports:**

```typescript
import ProfilePage from './pages/ProfilePage';
```

**Thêm route trong `<Routes>` (bọc bằng `ProtectedRoute`):**

```tsx
<Route
  path="/profile"
  element={
    <ProtectedRoute>
      <MainLayout>
        <ProfilePage />
      </MainLayout>
    </ProtectedRoute>
  }
/>
```

**Lưu ý:** Profile dùng `MainLayout` (có Sidebar + TopHeader). KHÔNG access từ sidebar — access qua user menu trong TopHeader (xem §B4 dưới).

### B3.2. Thêm link "Hồ sơ của tôi" vào TopHeader user menu

**File:** `src/components/layout/TopHeader.tsx` (hoặc tương đương)

Tìm block dropdown user menu, thêm item:

```tsx
<Link
  to="/profile"
  className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
>
  <UserCircleIcon className="h-4 w-4" />
  Hồ sơ của tôi
</Link>
```

Nếu project chưa có user menu dropdown trong TopHeader, agent FE tạo mới với 3 item:
- Hồ sơ của tôi (→ `/profile`)
- Cài đặt (→ `/profile#settings` — anchor jump to Section 4)
- Đăng xuất (→ logout)

---

## §B4 — Component structure (~3h)

### B4.1. File organization

```
src/
├── pages/
│   └── ProfilePage.tsx                    ← Container chính
├── components/
│   └── profile/
│       ├── shared/
│       │   ├── SectionCard.tsx
│       │   ├── Avatar.tsx
│       │   ├── ConstitutionPill.tsx
│       │   ├── FieldRow.tsx
│       │   ├── Segmented.tsx
│       │   ├── ProgressBar.tsx
│       │   └── EditIconButton.tsx
│       ├── sections/
│       │   ├── S1ProfileHeader.tsx
│       │   ├── S2PersonalInfo.tsx
│       │   ├── S3Goal.tsx
│       │   ├── S4HealthSettings.tsx
│       │   ├── S5Security.tsx
│       │   └── S6DangerZone.tsx
│       ├── modals/
│       │   ├── GoalChangeModal.tsx
│       │   └── DeleteAccountModal.tsx     ← Placeholder, button click → toast
│       └── PbfMethodCard.tsx
├── services/
│   ├── password.service.ts                ← NEW
│   ├── profile.service.ts                  ← NEW
│   └── userGoals.service.ts               ← UPDATE (add getGoalHistory + startWeightKg)
└── types/
    └── profile.schemas.ts                 ← NEW
```

### B4.2. Mapping design tokens → Tailwind

Trong design source `profile-shared.jsx` dùng `DB.*` tokens (kế thừa từ `dashboard-shared.jsx`). Mapping cụ thể:

| Design token (DB.*) | Tailwind class |
|---|---|
| `DB.green` (#059669) | `brand-green` |
| `DB.green50` (#ecfdf5) | `brand-green-light` |
| `DB.greenDark` (#047857) | `brand-green-dark` |
| `DB.green100` (#d1fae5) | `green-100` (default Tailwind) |
| `DB.green200` (#a7f3d0) | `green-200` |
| `DB.amber100/700` | `amber-100/700` |
| `DB.blue50/600/700` | `blue-50/600/700` |
| `DB.red100/600/700` | `red-100/600/700` |
| `DB.orange50/600` | `orange-50/600` |
| `DB.ink` (#0f1f1a) | `text-gray-900` |
| `DB.text` (#1f2937) | `text-gray-800` |
| `DB.textMid` (#4b5563) | `text-gray-600` |
| `DB.textMute` (#6b7280) | `brand-gray` hoặc `text-gray-500` |
| `DB.textFaint` (#9ca3af) | `text-gray-400` |
| `DB.border` (#e5e7eb) | `border-gray-200` |
| `DB.borderSoft` (#f1f5f4) | `border-gray-100` |

**Bonus:** Gradient `linear-gradient(135deg, ${DB.green} 0%, #10b981 100%)` → Tailwind `bg-gradient-to-br from-brand-green to-brand-green-medium` (token `brand-green-medium = #10b981` đã có).

### B4.3. ProfilePage container

**File:** `src/pages/ProfilePage.tsx`

```tsx
import React, { useCallback, useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useAuth } from '../contexts/AuthContext';
import { getProfileOverview, type ProfileOverview } from '../services/profile.service';
import S1ProfileHeader from '../components/profile/sections/S1ProfileHeader';
import S2PersonalInfo from '../components/profile/sections/S2PersonalInfo';
import S3Goal from '../components/profile/sections/S3Goal';
import S4HealthSettings from '../components/profile/sections/S4HealthSettings';
import S5Security from '../components/profile/sections/S5Security';
import S6DangerZone from '../components/profile/sections/S6DangerZone';
import ProfileSkeleton from '../components/profile/ProfileSkeleton';

const ProfilePage: React.FC = () => {
  const { user, refreshUser } = useAuth();
  const [overview, setOverview] = useState<ProfileOverview | null>(null);
  const [loading, setLoading] = useState(true);

  const loadProfile = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getProfileOverview();
      setOverview(data);
    } catch (err) {
      toast.error('Không tải được hồ sơ');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  if (loading || !overview) {
    return (
      <main className="px-6 py-8 lg:px-10 lg:py-10">
        <ProfileSkeleton />
      </main>
    );
  }

  return (
    <main className="px-6 py-8 lg:px-10 lg:py-10">
      {/* Breadcrumb + title */}
      <div className="mx-auto mb-6 max-w-[880px]">
        <div className="flex items-center gap-1.5 text-xs font-medium text-gray-500">
          <span>Trang chủ</span>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
            <path d="M9 18l6-6-6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          <span className="text-gray-800">Hồ sơ của tôi</span>
        </div>
        <h1 className="mt-1.5 text-2xl font-bold tracking-tight text-gray-900">
          Hồ sơ của tôi
        </h1>
        <p className="mt-1 text-sm text-gray-600">
          Quản lý thông tin cá nhân, mục tiêu và cài đặt sức khỏe của bạn
        </p>
      </div>

      {/* 6 sections */}
      <div className="mx-auto flex w-full max-w-[880px] flex-col gap-5 pb-6">
        <S1ProfileHeader
          user={user}
          constitution={overview.constitution?.constitution ?? 'CAN_DOI'}
        />
        <S2PersonalInfo user={user} onUpdated={refreshUser} />
        <S3Goal
          currentGoal={overview.currentGoal}
          history={overview.goalHistory}
          currentWeight={overview.metrics?.weight?.value ?? null}
          onGoalChanged={loadProfile}
        />
        <S4HealthSettings
          preferences={overview.preferences}
          onPreferenceChanged={loadProfile}
        />
        <S5Security />
        <S6DangerZone />
      </div>
    </main>
  );
};

export default ProfilePage;
```

### B4.4. Sample component — S2PersonalInfo

**File:** `src/components/profile/sections/S2PersonalInfo.tsx`

Đây là component PHỨC TẠP NHẤT (có view/edit/saving state). Code mẫu đầy đủ:

```tsx
import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { personalInfoSchema, computeAge, type PersonalInfoData } from '../../../types/profile.schemas';
import { updateUserProfile } from '../../../services/user.service';
import type { UserProfileData } from '../../../services/auth.service';
import SectionCard from '../shared/SectionCard';
import FieldRow from '../shared/FieldRow';
import Segmented from '../shared/Segmented';
import EditIconButton from '../shared/EditIconButton';

interface Props {
  user: UserProfileData | null;
  onUpdated: () => Promise<void>;
}

const GENDER_LABEL = { MALE: 'Nam', FEMALE: 'Nữ', OTHER: 'Khác' };

const S2PersonalInfo: React.FC<Props> = ({ user, onUpdated }) => {
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors, isValid },
  } = useForm<PersonalInfoData>({
    resolver: zodResolver(personalInfoSchema),
    mode: 'onChange',
    defaultValues: {
      name: user?.name ?? '',
      birthDate: user?.birthDate ?? '',
      gender: (user?.gender as 'MALE' | 'FEMALE' | 'OTHER') ?? undefined,
      phone: user?.phone ?? '',
    },
  });

  const enterEdit = () => {
    reset({
      name: user?.name ?? '',
      birthDate: user?.birthDate ?? '',
      gender: (user?.gender as 'MALE' | 'FEMALE' | 'OTHER') ?? undefined,
      phone: user?.phone ?? '',
    });
    setEditing(true);
  };

  const cancelEdit = () => setEditing(false);

  const onSubmit = async (data: PersonalInfoData) => {
    setSaving(true);
    try {
      await updateUserProfile({
        name: data.name,
        birthDate: data.birthDate,
        gender: data.gender,
        phone: data.phone || null,
      });
      await onUpdated();
      toast.success('Đã cập nhật thông tin');
      setEditing(false);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại');
    } finally {
      setSaving(false);
    }
  };

  const age = user?.birthDate ? computeAge(user.birthDate) : null;
  const birthDateWatch = watch('birthDate');
  const editAge = birthDateWatch ? computeAge(birthDateWatch) : null;

  if (!editing) {
    // View mode
    return (
      <SectionCard
        title="Thông tin cá nhân"
        subtitle="Các thông tin cơ bản hệ thống dùng để tính chỉ số"
        rightSlot={<EditIconButton onClick={enterEdit} />}
      >
        <div className="grid grid-cols-1 gap-x-6 gap-y-5 md:grid-cols-2">
          <FieldRow label="Họ và tên" value={user?.name} />
          <FieldRow
            label="Ngày sinh"
            value={user?.birthDate ? formatVnDate(user.birthDate) : ''}
            help={age ? `Bạn ${age} tuổi` : undefined}
          />
          <FieldRow
            label="Giới tính"
            value={user?.gender ? GENDER_LABEL[user.gender] : ''}
          />
          <FieldRow label="Số điện thoại" value={user?.phone} />
        </div>
      </SectionCard>
    );
  }

  // Edit mode
  return (
    <SectionCard
      title="Thông tin cá nhân"
      subtitle="Đang chỉnh sửa..."
    >
      <form onSubmit={handleSubmit(onSubmit)}>
        <div className="grid grid-cols-1 gap-x-6 gap-y-5 md:grid-cols-2">
          <div>
            <label className="block text-xs font-semibold text-gray-700">Họ và tên *</label>
            <input
              type="text"
              {...register('name')}
              className={inputClass(!!errors.name)}
              disabled={saving}
            />
            {errors.name && <p className="mt-1 text-xs text-red-600">{errors.name.message}</p>}
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700">Ngày sinh *</label>
            <input
              type="date"
              {...register('birthDate')}
              className={inputClass(!!errors.birthDate)}
              disabled={saving}
            />
            {editAge && <p className="mt-1 text-xs text-gray-500">Bạn {editAge} tuổi</p>}
            {errors.birthDate && <p className="mt-1 text-xs text-red-600">{errors.birthDate.message}</p>}
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700">Giới tính *</label>
            <Segmented
              options={[
                { value: 'MALE', label: 'Nam' },
                { value: 'FEMALE', label: 'Nữ' },
                { value: 'OTHER', label: 'Khác' },
              ]}
              value={watch('gender')}
              onChange={(v) => setValue('gender', v as 'MALE' | 'FEMALE' | 'OTHER', {
                shouldValidate: true, shouldDirty: true,
              })}
              disabled={saving}
            />
            {errors.gender && <p className="mt-1 text-xs text-red-600">{errors.gender.message}</p>}
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700">Số điện thoại</label>
            <input
              type="tel"
              {...register('phone')}
              placeholder="0xxx xxx xxx"
              className={inputClass(!!errors.phone)}
              disabled={saving}
            />
            {errors.phone && <p className="mt-1 text-xs text-red-600">{errors.phone.message}</p>}
          </div>
        </div>

        <div className="mt-6 flex items-center justify-end gap-2 border-t border-gray-100 pt-4">
          <button
            type="button"
            onClick={cancelEdit}
            disabled={saving}
            className="rounded-lg px-4 py-2 text-sm font-semibold text-gray-600 transition hover:bg-gray-50"
          >
            Hủy
          </button>
          <button
            type="submit"
            disabled={!isValid || saving}
            className="inline-flex items-center gap-2 rounded-lg bg-gradient-to-br from-brand-green to-brand-green-medium px-5 py-2 text-sm font-semibold text-white shadow-sm transition disabled:opacity-50"
          >
            {saving && (
              <span className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent" />
            )}
            {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
          </button>
        </div>
      </form>
    </SectionCard>
  );
};

function formatVnDate(iso: string): string {
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

function inputClass(error: boolean): string {
  return `mt-1 w-full rounded-xl border-2 px-3 py-2.5 text-sm outline-none transition ${
    error
      ? 'border-red-300 bg-red-50 focus:border-red-500'
      : 'border-gray-100 focus:border-brand-green focus:ring-4 focus:ring-brand-green-light'
  }`;
}

export default S2PersonalInfo;
```

Các section còn lại (S1, S3, S4, S5, S6) follow tương tự pattern này. Agent convert từ `profile-sectionsV1.jsx` design source theo §B4.2 mapping table.

### B4.5. S3Goal — progress bar logic

Trong `S3Goal.tsx`, tính progress dựa vào `startWeightKg`, `targetWeightKg`, `currentWeight`:

```tsx
const startWeight = currentGoal?.startWeightKg;
const targetWeight = currentGoal?.targetWeightKg;

let progressPct: number | null = null;
let progressText: string | null = null;

if (startWeight != null && targetWeight != null && currentWeight != null) {
  // For GIAM: progress = (startWeight - currentWeight) / (startWeight - targetWeight)
  // For TANG: progress = (currentWeight - startWeight) / (targetWeight - startWeight)
  // For DUY_TRI: KHÔNG hiển thị progress bar
  const goal = currentGoal!.goalCode;
  if (goal === 'GIAM' && startWeight > targetWeight) {
    const lost = startWeight - currentWeight;
    const need = startWeight - targetWeight;
    progressPct = Math.max(0, Math.min(100, Math.round((lost / need) * 100)));
    progressText = `Đã giảm ${lost.toFixed(1)}kg / ${need.toFixed(1)}kg`;
  } else if (goal === 'TANG' && startWeight < targetWeight) {
    const gained = currentWeight - startWeight;
    const need = targetWeight - startWeight;
    progressPct = Math.max(0, Math.min(100, Math.round((gained / need) * 100)));
    progressText = `Đã tăng ${gained.toFixed(1)}kg / ${need.toFixed(1)}kg`;
  }
}

// Render:
{progressPct !== null ? (
  <div className="mt-5">
    <div className="flex items-center justify-between">
      <span className="text-sm text-gray-600">{progressText}</span>
      <span className="text-sm font-bold text-brand-green-dark">{progressPct}%</span>
    </div>
    <ProgressBar value={progressPct} max={100} />
    <div className="mt-2 flex justify-between text-xs text-gray-500">
      <span>Bắt đầu: {startWeight.toFixed(1)} kg</span>
      <span className="font-semibold text-brand-green-dark">Hiện tại: {currentWeight.toFixed(1)} kg</span>
      <span>Mục tiêu: {targetWeight.toFixed(1)} kg</span>
    </div>
  </div>
) : (
  <div className="mt-5 rounded-xl bg-gray-50 p-3 text-center text-sm text-gray-500">
    {!startWeight
      ? 'Chưa có dữ liệu cân nặng khởi điểm'
      : !targetWeight
        ? 'Chưa đặt cân nặng mục tiêu'
        : 'Tiến độ chưa khả dụng'}
  </div>
)}
```

### B4.6. S4HealthSettings — toggle PBF method

```tsx
const pbfPref = preferences.find((p) => p.prefKey === 'pbf_method');
const currentMethod = pbfPref?.prefValue ?? 'FORMULA';

const handleToggle = async (newMethod: 'FORMULA' | 'MODEL_1') => {
  if (newMethod === currentMethod) return;
  try {
    await updatePreference('pbf_method', { prefValue: newMethod, valueType: 'STRING' });
    toast.success(`Đã đổi phương pháp tính PBF sang ${newMethod === 'FORMULA' ? 'Công thức Navy' : 'Model AI'}`);
    onPreferenceChanged();
  } catch (err) {
    toast.error('Không đổi được, vui lòng thử lại');
  }
};
```

### B4.7. S5Security — Change Password form

```tsx
const onSubmit = async (data: ChangePasswordData) => {
  setSaving(true);
  try {
    await changePassword({
      currentPassword: data.currentPassword,
      newPassword: data.newPassword,
    });
    toast.success('Đã đổi mật khẩu thành công');
    reset(); // Clear form
  } catch (err: any) {
    const code = err.response?.data?.code;
    if (code === 'AUTH-011') {
      setError('currentPassword', { message: 'Mật khẩu hiện tại không đúng' });
    } else if (code === 'AUTH-012') {
      setError('newPassword', { message: 'Mật khẩu mới phải khác mật khẩu cũ' });
    } else {
      toast.error(err.response?.data?.message || 'Có lỗi xảy ra');
    }
  } finally {
    setSaving(false);
  }
};
```

### B4.8. S6DangerZone — placeholder

```tsx
const handleDelete = () => {
  toast.info('Tính năng đang phát triển. Vui lòng liên hệ admin để xóa tài khoản.');
};
```

KHÔNG mở modal DeleteAccountModal trong MVP. Chỉ render UI section với button → click → toast.

---

## §B5 — Acceptance Criteria

### B5.1. Build pass

- [ ] `npx tsc -b --pretty false` không lỗi
- [ ] `npm run build` không error

### B5.2. Visual verify

So sánh với mockup `ProfileV1.html`:

**Section 1 Header:**
- [ ] Avatar 80x80 gradient xanh có glow shadow
- [ ] Tên + email + ngày tham gia hiển thị đúng từ `useAuth().user`
- [ ] ConstitutionPill có màu đúng theo thể trạng

**Section 2 Personal Info:**
- [ ] View mode: 4 field readonly, pencil icon góc phải
- [ ] Click pencil → enter edit mode: 4 input field
- [ ] Saving state: button spinner + disabled
- [ ] Validation error: input border đỏ + message dưới
- [ ] Submit thành công → toast + về view mode + data refresh

**Section 3 Goal:**
- [ ] Current goal card có icon emoji + label + ngày bắt đầu
- [ ] Progress bar hiển thị đúng cho GIAM/TANG, ẩn cho DUY_TRI
- [ ] Empty state khi `startWeightKg` null: "Chưa có dữ liệu cân nặng khởi điểm"
- [ ] Collapsible history click expand/collapse
- [ ] Empty state history khi `goalHistory.length === 0`
- [ ] Click "Đổi mục tiêu" → open GoalChangeModal

**Section 4 Health Settings:**
- [ ] 2 PbfMethodCard render, currentMethod được highlight
- [ ] Click card khác → toast success + refresh preferences

**Section 5 Security:**
- [ ] 3 field password
- [ ] Validate min 8 + có HOA + có số + xác nhận match
- [ ] Submit sai current password → error inline trên field currentPassword
- [ ] Submit new = current → error inline trên field newPassword
- [ ] Submit thành công → toast + clear form

**Section 6 Danger Zone:**
- [ ] Card border đỏ + bg đỏ nhạt
- [ ] Button "Xóa tài khoản" click → toast info "Tính năng đang phát triển..."

### B5.3. Responsive verify

- [ ] Desktop ≥ 1024px: form grid 2 cột, max-width 880px
- [ ] Tablet 768px: form grid 1 cột
- [ ] Mobile 380px: stack vertical, button full-width

### B5.4. Integration test

- [ ] Login → click avatar dropdown → "Hồ sơ của tôi" → vào `/profile`
- [ ] Section 2 edit + save → reload page → data persist
- [ ] Section 3 set goal mới → check DB: `start_weight_kg` có giá trị
- [ ] Section 4 toggle PBF → reload Dashboard → constitution card hiển thị method mới
- [ ] Section 5 đổi password → logout → login với password mới → thành công

---

## §B6 — Order of Implementation

**Tổng:** ~6-7h FE.

### Phase FE-1 (~1.5h) — Foundation
1. Update `userGoals.service.ts` add `startWeightKg` + `getGoalHistory()`
2. Create `password.service.ts`
3. Create `profile.service.ts`
4. Create `types/profile.schemas.ts`
5. Build `npx tsc` pass

### Phase FE-2 (~1h) — Routing + Container + Shared atoms
6. Add `/profile` route trong `App.tsx`
7. Add link "Hồ sơ của tôi" trong TopHeader
8. Build `ProfilePage.tsx` container (chưa render section, mock empty divs)
9. Build shared atoms: `SectionCard`, `Avatar`, `ConstitutionPill`, `FieldRow`, `Segmented`, `ProgressBar`, `EditIconButton`
10. Verify routing work

### Phase FE-3 (~2h) — 6 sections
11. Build `S1ProfileHeader` (đơn giản nhất)
12. Build `S2PersonalInfo` view + edit + saving (phức tạp nhất)
13. Build `S3Goal` với progress bar logic
14. Build `S4HealthSettings` với PbfMethodCard
15. Build `S5Security` change password
16. Build `S6DangerZone` placeholder

### Phase FE-4 (~1h) — Modals + Polish
17. Build `GoalChangeModal`
18. Build `ProfileSkeleton`
19. Toast notifications integration
20. Visual polish, responsive verify

### Phase FE-5 (~30 phút) — Integration test
21. Full flow test theo §B5.4
22. Screenshot từng section so sánh với mockup
23. Fix visual gaps nếu có

---

## §B7 — ⚠ Điểm cực kỳ dễ sai

### B7.1. KHÔNG hardcode sample data

Trong `profile-sectionsV1.jsx` có:
- `name = 'Nguyễn Văn Chiến'`
- `email = 'chien.nguyen@healthcare.vn'`
- `joined = 'Tham gia từ 03/2026'`
- `GOAL_HISTORY` sample 3 items
- `currentWeight = 71.2, startWeight = 75`

Đây là **SAMPLE DATA cho preview**, KHÔNG được copy. Bind với:
- `user.name`, `user.username` (cho email proxy nếu không có email field)
- Format `Tham gia từ MM/YYYY` từ `user.createdAt`
- `goalHistory` từ API
- `currentWeight` từ `overview.metrics.weight.value`

### B7.2. `TextInput` design có `readOnly` attribute

Dòng 246 của `profile-shared.jsx`:
```jsx
<input ... readOnly />
```

Đây là cho design preview. Agent FE PHẢI bỏ `readOnly` và dùng `{...register('field')}`.

### B7.3. RestTemplate timeout phải đủ ngắn

Trong A2.1: connect 2s, read 3s. KHÔNG để default Spring (vô hạn). Lý do: nếu health-data-service hang, set goal sẽ hang vô thời hạn → bad UX.

### B7.4. `startWeightKg` null phải handle ở FE

UserGoal cũ (trước migration) sẽ có `start_weight_kg = null`. UserGoal mới nếu health-data-service down lúc set goal cũng null. FE PHẢI render fallback "Chưa có dữ liệu cân nặng khởi điểm" — KHÔNG để `null` lọt vào tính toán (sẽ ra NaN%).

### B7.5. Password security

- KHÔNG log password ra console BE (`log.info("New password: {}")` ← SAI)
- KHÔNG return password trong response (đã đảm bảo vì DTO không có field)
- FE KHÔNG localStorage password
- Production phải HTTPS để mã hóa transit (local dev có thể HTTP OK)

### B7.6. JWT vẫn hợp lệ sau change password

MVP: BE KHÔNG invalidate JWT khi đổi password (tránh logout user khỏi tất cả device). Nếu sau này cần feature "logout all devices on password change", thêm `tokenVersion` field vào Auth + check trong JwtFilter.

### B7.7. Hibernate auto-update phải work với column mới

`spring.jpa.hibernate.ddl-auto=update` trong `application.yml`. Sau khi deploy entity `UserGoal` mới với `startWeightKg`, Hibernate tự `ALTER TABLE ADD COLUMN`. **Verify:** Sau khi BE restart, check DB:

```sql
SHOW COLUMNS FROM user_goals LIKE 'start_weight_kg';
```

Nếu KHÔNG thấy column → kiểm tra `ddl-auto` config, restart container.

### B7.8. `phone` empty string vs null

BE DTO `UpdateProfileRequest` field `phone` validate optional. FE gửi `null` nếu empty (không gửi `""`). Pattern:

```typescript
phone: data.phone || null,   // ← convert "" → null
```

### B7.9. Gateway routing change-password phải pass JWT filter

Verify gateway forward `PUT /api/auth/change-password` đi qua JWT filter (require auth). KHÔNG được bypass như `/api/auth/login`.

### B7.10. Goal history endpoint có thể chưa expose

`UserGoalService.getHistory()` đã có trong service nhưng có thể chưa có endpoint controller. Agent BE check `UserGoalController.java`, nếu chưa có `GET /api/user-goals/history` thì thêm:

```java
@GetMapping("/history")
public ResponseEntity<DataResponse<List<UserGoalResponse>>> getHistory(
        @RequestHeader("userId") String userId) {
    return ResponseEntity.ok(DataResponse.success(userGoalService.getHistory(userId)));
}
```

---

## §B8 — Deliverables

### BE deliverables

**Files mới:**
- [ ] `user-service/src/main/java/.../config/RestTemplateConfig.java`
- [ ] `user-service/src/main/java/.../service/HealthDataClient.java`
- [ ] `user-service/src/main/java/.../dto/request/ChangePasswordRequest.java`

**Files update:**
- [ ] `user-service/src/main/java/.../entity/UserGoal.java` (add `startWeightKg`)
- [ ] `user-service/src/main/java/.../dto/response/UserGoalResponse.java` (add `startWeightKg`)
- [ ] `user-service/src/main/java/.../service/UserGoalServiceImpl.java` (snapshot logic)
- [ ] `user-service/src/main/java/.../service/AuthService.java` (interface + impl)
- [ ] `user-service/src/main/java/.../service/AuthServiceImpl.java` (changePassword method)
- [ ] `user-service/src/main/java/.../controller/AuthController.java` (PUT endpoint)
- [ ] `user-service/src/main/java/.../controller/UserGoalController.java` (history endpoint nếu chưa có)
- [ ] `common/src/main/java/.../exception/ErrorCode.java` (AUTH-011, AUTH-012)
- [ ] `user-service/src/main/resources/application.yml` (add `app.services.health-data.url`)

### FE deliverables

**Files mới:**
- [ ] `src/pages/ProfilePage.tsx`
- [ ] `src/services/password.service.ts`
- [ ] `src/services/profile.service.ts`
- [ ] `src/types/profile.schemas.ts`
- [ ] `src/components/profile/shared/SectionCard.tsx`
- [ ] `src/components/profile/shared/Avatar.tsx`
- [ ] `src/components/profile/shared/ConstitutionPill.tsx`
- [ ] `src/components/profile/shared/FieldRow.tsx`
- [ ] `src/components/profile/shared/Segmented.tsx`
- [ ] `src/components/profile/shared/ProgressBar.tsx`
- [ ] `src/components/profile/shared/EditIconButton.tsx`
- [ ] `src/components/profile/sections/S1ProfileHeader.tsx`
- [ ] `src/components/profile/sections/S2PersonalInfo.tsx`
- [ ] `src/components/profile/sections/S3Goal.tsx`
- [ ] `src/components/profile/sections/S4HealthSettings.tsx`
- [ ] `src/components/profile/sections/S5Security.tsx`
- [ ] `src/components/profile/sections/S6DangerZone.tsx`
- [ ] `src/components/profile/PbfMethodCard.tsx`
- [ ] `src/components/profile/modals/GoalChangeModal.tsx`
- [ ] `src/components/profile/ProfileSkeleton.tsx`

**Files update:**
- [ ] `src/App.tsx` (add `/profile` route)
- [ ] `src/components/layout/TopHeader.tsx` (add user menu link)
- [ ] `src/services/userGoals.service.ts` (add `startWeightKg`, `getGoalHistory`)

### Commits gợi ý

**BE commits:**

```bash
# A1
git add user-service/src/main/java/org/example/userservice/entity/UserGoal.java \
        user-service/src/main/java/org/example/userservice/dto/response/UserGoalResponse.java \
        user-service/src/main/java/org/example/userservice/service/UserGoalServiceImpl.java
git commit -m "feat(user-goal): add start_weight_kg column for progress tracking

Add nullable column start_weight_kg to user_goals table via
Hibernate ddl-auto. Update entity, response DTO, and toResponse
mapping. Snapshot logic added separately in HealthDataClient."

# A2
git add user-service/src/main/java/org/example/userservice/config/RestTemplateConfig.java \
        user-service/src/main/java/org/example/userservice/service/HealthDataClient.java
git commit -m "feat(user-goal): snapshot current weight on goal change

Add lightweight RestTemplate client to fetch current weight from
health-data-service when user sets a new goal. Failures return null
so goal update succeeds even if downstream is unavailable.

Timeouts: 2s connect, 3s read."

# A3
git commit -m "feat(auth): add change-password endpoint

PUT /api/auth/change-password with currentPassword + newPassword.
Validates current password via BCrypt match, rejects same password,
returns AUTH-011/AUTH-012 error codes for specific failures."
```

**FE commits:**

```bash
git commit -m "feat(profile): service layer for profile page

Add password.service, profile.service, update userGoals.service
to include startWeightKg + getGoalHistory."

git commit -m "feat(profile): build ProfilePage container + 6 sections

Implement Profile page with 6 sections matching Claude Design
mockup. View/edit modes for personal info, progress bar for goal
(uses new startWeightKg from BE), PBF method toggle, change
password form. Delete account is UI placeholder for MVP."
```

---

## §B9 — Khuyến nghị thứ tự run

**Day 1 (BE — 2-3h):**
1. Run BE Phase A1 + A2 (migration + snapshot logic), test với Postman
2. Run BE Phase A3 (change password endpoint), test với Postman
3. Verify DB schema có `start_weight_kg`, set goal mới thấy field được populated

**Day 2 (FE — 6-7h):**
4. Run FE Phase FE-1 + FE-2 (services + routing), `npx tsc` pass
5. Run FE Phase FE-3 (6 sections), test từng section riêng
6. Run FE Phase FE-4 (modals + polish)
7. Run FE Phase FE-5 (integration test full flow)

**KHÔNG bỏ qua step:** BE phải build xong + verify Postman trước khi FE bắt đầu. Tránh tình trạng FE code dựa trên BE chưa work.

---

## VERSION HISTORY

| Ngày | Version | Thay đổi |
|---|---|---|
| 26/05/2026 | v1.0 | Hướng dẫn ban đầu Profile page. Scope MVP: build §1-5 đầy đủ, §6 placeholder. BE work: add `start_weight_kg` column + cross-service snapshot logic + change-password endpoint. FE work: routing + 6 sections + 1 modal. Tổng ~8-10h work. |
