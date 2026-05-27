# HƯỚNG DẪN BACKEND — PROFILE PAGE + REFACTOR DATARESPONSE

> **Audience:** Code agent thực hiện backend tại repo `HealthManagement` branch `feature/profilePage-be`.
>
> **Mục đích:** Hoàn thiện 100% backend cần thiết cho trang Profile, đồng thời chuẩn hóa toàn bộ contract `health-data-service` về dạng `DataResponse<T>`.
>
> **Tổng workload:** ~4-5h (refactor ~2h, profile setup ~1.5h, change-password endpoint ~1h, test ~30 phút).
>
> **KHÔNG TỰ Ý mở rộng scope:** chỉ làm những gì doc này yêu cầu. Mọi thay đổi ngoài scope phải hỏi user trước.

---

## 0. CONTEXT & SCOPE

### 0.1. Tổng quan công việc

| Phase | Nội dung | Time | Severity |
|---|---|---|---|
| **A** | Refactor `health-data-service` về `DataResponse<T>` | ~2h | Standardization |
| **B** | Setup Profile data — `start_weight_kg` + cross-service snapshot | ~1h | Build mới |
| **C** | Setup Profile data — `email` + `createdAt` + `phone` null fix | ~30 phút | Fix existing |
| **D** | Change Password endpoint | ~1h | Build mới |
| **E** | Test toàn bộ qua Postman | ~30 phút | Verify |

### 0.2. State hiện tại (đã verify clone repo commit `54940d8`)

**health-data-service controllers — inconsistency contract:**

| Endpoint | Pattern hiện tại |
|---|---|
| `POST /api/health-data/submit` | ✅ `DataResponse<Void>` |
| `GET /api/health-data/latest-metrics` | ❌ raw `LatestHealthDataResponse` |
| `GET /api/health-data/dashboard-metrics` | ❌ raw `DashboardMetricsResponse` |
| `GET /api/health-data/constitution` | ✅ `DataResponse<ConstitutionResponse>` |
| `GET /api/health-data/query/history/{type}` | ❌ raw `List<HistoricalDataPointDTO>` |
| `UnitController` toàn bộ (6 endpoint) | ❌ raw `UnitDTO`/`List<UnitDTO>`/`Void` |
| `HealthIndicatorConfigsController` toàn bộ (3 endpoint) | ❌ raw |

→ **Tổng 11 endpoint không wrap, 2 endpoint đã wrap.** Phase A sẽ chuẩn hóa toàn bộ về `DataResponse<T>`.

**Profile entities/DTO issues đã phát hiện:**

| Issue | Hậu quả | Fix ở |
|---|---|---|
| `User.createdAt` không có `@CreationTimestamp` | Insert luôn null | §C1 |
| `UserProfileResponse`/`UserResponseDTO` thiếu `email`, `createdAt` | Section 1 Profile thiếu data | §C2 |
| `UserServiceImpl.updateUserProfile()` bỏ qua `phone: null` | User không clear được phone | §C3 |
| `UserGoal` thiếu `start_weight_kg` | Không tính được progress bar Section 3 | §B1 |
| Chưa có cross-service call mechanism | Không snapshot weight khi set goal | §B2 |
| Chưa có `PUT /api/auth/change-password` | Section 5 không hoạt động | §D |

### 0.3. Convention bắt buộc

1. **Code comment + commit message:** tiếng Anh
2. **ErrorCode message:** không dấu (tiếng Việt không dấu), tránh encoding issue
3. **Header userId:** `@RequestHeader("userId")` (KHÔNG phải `X-User-Id`)
4. **Password security:** KHÔNG log password ra console, KHÔNG return password trong response
5. **Cross-service timeout:** 2s connect / 3s read (tránh hang user request)
6. **KHÔNG tự ý refactor:** chỉ refactor các file được liệt kê trong doc

### 0.4. KHÔNG động vào (out of scope)

- Các controller của `user-service` (chỉ thêm field, KHÔNG refactor contract)
- Các controller của `nutrition-service`
- Các entity ngoài `User`, `UserGoal`, `Auth`
- File `pom.xml`, dependency versions
- Database schema migration tay (Hibernate `ddl-auto=update` tự xử lý)

---

# PHASE A — REFACTOR HEALTH-DATA-SERVICE VỀ DATARESPONSE (~2h)

## A1. Background

`health-data-service` hiện có 13 endpoint, trong đó:
- ✅ 2 endpoint đã wrap `DataResponse<T>` (giữ nguyên)
- ❌ 11 endpoint trả raw type (cần refactor)

**FE đã có `unwrapDataResponse()` helper** trong `src/services/apiResponse.ts` xử lý CẢ 2 case (`DataResponse<T>` và raw `T`). Nghĩa là:
- File `src/services/dashboard.service.ts` (mới) đã dùng `unwrapDataResponse` → **AUTOMATICALLY work** sau refactor
- File `src/services/healthData.service.ts` (cũ) parse `response.data` trực tiếp → **CẦN UPDATE FE** sau khi BE merge

→ FE agent sẽ xử lý file FE cũ trong doc FE riêng. BE agent KHÔNG đụng FE.

## A2. Refactor `HealthDataController` (3 endpoint)

**File:** `health-data-service/src/main/java/org/example/healthdataservice/controller/HealthDataController.java`

### A2.1. `GET /latest-metrics`

**BEFORE:**

```java
@GetMapping("/latest-metrics")
public ResponseEntity<LatestHealthDataResponse> getLatestUserMetrics(
        @RequestHeader("userId") String userId
) {
    if (userId == null) {
        return ResponseEntity.badRequest().build();
    }
    log.info("Fetching latest metrics for userId: {}", userId);

    Set<IndicatorType> baseTypesToFetch = Arrays.stream(IndicatorType.values())
            .filter(IndicatorType::isBaseMetric)
            .collect(Collectors.toSet());

    Map<IndicatorType, BaseMetricValue> latestBaseValues = baseMetricService.getLatestBaseMetrics(userId, baseTypesToFetch);

    Map<String, Double> baseMetricsMap = new HashMap<>();

    latestBaseValues.forEach((type, metricValue) -> {
        baseMetricsMap.put(type.name(), metricValue.getValue());
    });
    LatestHealthDataResponse response = new LatestHealthDataResponse(baseMetricsMap);

    return ResponseEntity.ok(response);
}
```

**AFTER:**

```java
@GetMapping("/latest-metrics")
public ResponseEntity<DataResponse<LatestHealthDataResponse>> getLatestUserMetrics(
        @RequestHeader("userId") String userId
) {
    if (userId == null) {
        return ResponseEntity.badRequest().build();
    }
    log.info("Fetching latest metrics for userId: {}", userId);

    Set<IndicatorType> baseTypesToFetch = Arrays.stream(IndicatorType.values())
            .filter(IndicatorType::isBaseMetric)
            .collect(Collectors.toSet());

    Map<IndicatorType, BaseMetricValue> latestBaseValues = baseMetricService.getLatestBaseMetrics(userId, baseTypesToFetch);

    Map<String, Double> baseMetricsMap = new HashMap<>();

    latestBaseValues.forEach((type, metricValue) -> {
        baseMetricsMap.put(type.name(), metricValue.getValue());
    });
    LatestHealthDataResponse response = new LatestHealthDataResponse(baseMetricsMap);

    return ResponseEntity.ok(DataResponse.success(response));
}
```

**Thay đổi:** Generic type wrap `DataResponse<>`, return dùng `DataResponse.success(response)`.

### A2.2. `GET /dashboard-metrics`

**BEFORE:**

```java
@GetMapping("/dashboard-metrics")
public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics(
        @RequestHeader("userId") String userId
) {
    if (userId == null) {
        return ResponseEntity.badRequest().build();
    }
    log.info("Fetching dashboard metrics for userId: {}", userId);

    DashboardMetricsResponse response = new DashboardMetricsResponse();
    // ... toàn bộ logic build response
    return ResponseEntity.ok(response);
}
```

**AFTER:**

```java
@GetMapping("/dashboard-metrics")
public ResponseEntity<DataResponse<DashboardMetricsResponse>> getDashboardMetrics(
        @RequestHeader("userId") String userId
) {
    if (userId == null) {
        return ResponseEntity.badRequest().build();
    }
    log.info("Fetching dashboard metrics for userId: {}", userId);

    DashboardMetricsResponse response = new DashboardMetricsResponse();
    // ... toàn bộ logic build response GIỮ NGUYÊN
    return ResponseEntity.ok(DataResponse.success(response));
}
```

**Lưu ý:** KHÔNG đụng logic build `response` ở giữa — chỉ thay đổi return type + return statement cuối.

### A2.3. `GET /query/history/{indicatorTypeString}`

**BEFORE:**

```java
@GetMapping("/query/history/{indicatorTypeString}")
public ResponseEntity<List<HistoricalDataPointDTO>> getIndicatorHistory(
        @RequestHeader("userId") String userId,
        @PathVariable String indicatorTypeString,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "DAILY") String granularity
) {
    if (userId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    IndicatorType indicatorType;

    try {
        indicatorType = IndicatorType.valueOf(indicatorTypeString.toUpperCase());
    } catch (IllegalArgumentException e) {
        log.warn("Invalid indicatorType string: {}", indicatorTypeString);
        return ResponseEntity.badRequest().body(Collections.singletonList(new HistoricalDataPointDTO(null, null, "Invalid Indicator Type")));
    }

    LocalDateTime fromDateTime = from.atStartOfDay();
    LocalDateTime toDateTime = to.atTime(23, 59, 59, 999999999);

    List<HistoricalDataPointDTO> history = historicalDataService.getHistoricalData(
            userId, indicatorType, fromDateTime, toDateTime, granularity.toUpperCase()
    );
    return ResponseEntity.ok(history);
}
```

**AFTER:**

```java
@GetMapping("/query/history/{indicatorTypeString}")
public ResponseEntity<DataResponse<List<HistoricalDataPointDTO>>> getIndicatorHistory(
        @RequestHeader("userId") String userId,
        @PathVariable String indicatorTypeString,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "DAILY") String granularity
) {
    if (userId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    IndicatorType indicatorType;

    try {
        indicatorType = IndicatorType.valueOf(indicatorTypeString.toUpperCase());
    } catch (IllegalArgumentException e) {
        log.warn("Invalid indicatorType string: {}", indicatorTypeString);
        return ResponseEntity.badRequest().body(
                DataResponse.error(
                        ErrorCode.HEALTH_INVALID_METRIC.getCode(),
                        "Loai chi so khong hop le: " + indicatorTypeString
                )
        );
    }

    LocalDateTime fromDateTime = from.atStartOfDay();
    LocalDateTime toDateTime = to.atTime(23, 59, 59, 999999999);

    List<HistoricalDataPointDTO> history = historicalDataService.getHistoricalData(
            userId, indicatorType, fromDateTime, toDateTime, granularity.toUpperCase()
    );
    return ResponseEntity.ok(DataResponse.success(history));
}
```

**Thay đổi quan trọng:**
1. Generic type wrap `DataResponse<List<HistoricalDataPointDTO>>`
2. Lỗi `IllegalArgumentException` không trả "fake DTO" nữa, trả `DataResponse.error()` đúng pattern
3. Verify `ErrorCode.HEALTH_INVALID_METRIC` đã tồn tại trong `common/src/main/java/.../ErrorCode.java`. Nếu chưa, dùng `ErrorCode.INTERNAL_SERVER_ERROR` tạm hoặc thêm mới (xem §A5 dưới).

## A3. Refactor `UnitController` (6 endpoint)

**File:** `health-data-service/src/main/java/org/example/healthdataservice/controller/UnitController.java`

**REPLACE TOÀN BỘ file:**

```java
package org.example.healthdataservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.dto.response.UnitDTO;
import org.example.healthdataservice.service.UnitService;
import org.example.web.dto.response.DataResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-data/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService service;

    @PostMapping
    public ResponseEntity<DataResponse<UnitDTO>> create(@Valid @RequestBody UnitDTO dto) {
        return ResponseEntity.ok(DataResponse.success(service.create(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<UnitDTO>> update(@PathVariable Long id, @Valid @RequestBody UnitDTO dto) {
        return ResponseEntity.ok(DataResponse.success(service.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(DataResponse.success());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<UnitDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(DataResponse.success(service.getById(id)));
    }

    @GetMapping
    public ResponseEntity<DataResponse<List<UnitDTO>>> getAll() {
        return ResponseEntity.ok(DataResponse.success(service.getAll()));
    }

    @PostMapping("/batch")
    public ResponseEntity<DataResponse<List<UnitDTO>>> createMany(@Valid @RequestBody List<@Valid UnitDTO> dtos) {
        return ResponseEntity.ok(DataResponse.success(service.createMany(dtos)));
    }
}
```

**Lưu ý:** `delete()` đổi từ `ResponseEntity.noContent()` (204) sang `ResponseEntity.ok()` (200) để đồng nhất với pattern các endpoint khác. Nếu FE đang dựa vào status 204, agent FE sẽ cần update.

## A4. Refactor `HealthIndicatorConfigsController` (3 endpoint)

**File:** `health-data-service/src/main/java/org/example/healthdataservice/controller/HealthIndicatorConfigsController.java`

**REPLACE TOÀN BỘ file:**

```java
package org.example.healthdataservice.controller;

import org.example.healthdataservice.dto.HealthIndicatorConfigsDTO;
import org.example.healthdataservice.entity.HealthIndicatorConfigs;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.mapper.HealthIndicatorConfigsMapper;
import org.example.healthdataservice.service.HealthIndicatorConfigsService;
import org.example.web.dto.response.DataResponse;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-data")
public class HealthIndicatorConfigsController {

    private final HealthIndicatorConfigsService healthIndicatorConfigsService;

    public HealthIndicatorConfigsController(HealthIndicatorConfigsService healthIndicatorConfigsService) {
        this.healthIndicatorConfigsService = healthIndicatorConfigsService;
    }

    @GetMapping("/indicator-configs")
    public ResponseEntity<DataResponse<List<HealthIndicatorConfigsDTO>>> getAll() {
        List<HealthIndicatorConfigsDTO> list = healthIndicatorConfigsService.getAll().stream()
                .map(HealthIndicatorConfigsMapper::toDTO)
                .toList();
        return ResponseEntity.ok(DataResponse.success(list));
    }

    @GetMapping("/indicator-configs/{indicatorType}")
    public ResponseEntity<DataResponse<HealthIndicatorConfigs>> getByIndicatorType(@PathVariable IndicatorType indicatorType) {
        return healthIndicatorConfigsService.getByType(indicatorType)
                .map(config -> ResponseEntity.ok(DataResponse.success(config)))
                .orElseThrow(() -> new BusinessException(ErrorCode.HEALTH_INVALID_METRIC));
    }

    @PostMapping("/indicator-configs")
    public ResponseEntity<DataResponse<HealthIndicatorConfigsDTO>> create(@RequestBody HealthIndicatorConfigsDTO healthIndicatorConfigsDTO) {
        HealthIndicatorConfigs entity = HealthIndicatorConfigsMapper.toEntity(healthIndicatorConfigsDTO);
        HealthIndicatorConfigs saved = healthIndicatorConfigsService.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(DataResponse.success(HealthIndicatorConfigsMapper.toDTO(saved)));
    }
}
```

**Thay đổi quan trọng:**
- `getByIndicatorType` đổi từ `ResponseEntity.notFound().build()` sang `throw BusinessException` — đồng nhất pattern error handling toàn project (global exception handler sẽ map thành 404 + DataResponse.error)
- Verify `BusinessException` + `ErrorCode.HEALTH_INVALID_METRIC` đã tồn tại. Nếu không, dùng `ErrorCode.NOT_FOUND` chung hoặc thêm code mới.

## A5. Verify ErrorCode tồn tại

Agent BE chạy:

```bash
grep -n "HEALTH_INVALID_METRIC\|HEALTH_" common/src/main/java/org/example/web/exception/ErrorCode.java
```

Nếu KHÔNG có `HEALTH_INVALID_METRIC`, thêm vào `ErrorCode.java`:

```java
// ===== Health-data module =====
HEALTH_INVALID_METRIC ("HEALTH-001", HttpStatus.BAD_REQUEST, "Loai chi so khong hop le"),
HEALTH_NOT_FOUND      ("HEALTH-002", HttpStatus.NOT_FOUND,   "Khong tim thay du lieu suc khoe"),
```

Vị trí thêm: ngay sau block AUTH error codes hiện có.

## A6. Verify Phase A — Postman test

Sau khi refactor + restart container `health-data-service`, test 4 endpoint chính:

**Test 1: GET dashboard-metrics**
```http
GET http://localhost:8080/api/health-data/dashboard-metrics
Authorization: Bearer <valid_jwt>

Expected response body:
{
  "code": null,
  "message": "Success",
  "data": {
    "weight": { "value": 70.5, "unit": "kg", "lastUpdatedAt": "..." },
    "height": { ... },
    ...
  }
}
```

**Test 2: GET latest-metrics**
```http
GET http://localhost:8080/api/health-data/latest-metrics
Authorization: Bearer <valid_jwt>

Expected: { code: null, message: "Success", data: { ...baseMetricsMap... } }
```

**Test 3: GET query/history/WEIGHT**
```http
GET http://localhost:8080/api/health-data/query/history/WEIGHT?from=2026-04-01&to=2026-05-26
Authorization: Bearer <valid_jwt>

Expected: { code: null, message: "Success", data: [ { timestamp, value, unit }, ... ] }
```

**Test 4: GET indicator-configs**
```http
GET http://localhost:8080/api/health-data/indicator-configs

Expected: { code: null, message: "Success", data: [ ... ] }
```

→ Nếu ALL 4 endpoint trả format `{ code, message, data }` → Phase A DONE. Báo cáo user trước khi sang Phase B.

---

# PHASE B — START WEIGHT KG + CROSS-SERVICE SNAPSHOT (~1h)

## B1. Add `start_weight_kg` column to UserGoal

### B1.1. Update UserGoal entity

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

**Lý do nullable:** UserGoal cũ trước migration sẽ có `start_weight_kg = NULL`. FE sẽ handle null case (hiển thị "Chưa có dữ liệu cân nặng khởi điểm"). Cũng cho phép health-data-service down lúc set goal mà không fail toàn bộ request.

### B1.2. Update UserGoalResponse DTO

**File:** `user-service/src/main/java/org/example/userservice/dto/response/UserGoalResponse.java`

**Thêm field tương ứng:**

```java
private BigDecimal targetWeightKg;
private BigDecimal startWeightKg;  // ← ADD
private Integer targetDurationMonths;
```

### B1.3. Update mapping toResponse()

**File:** `user-service/src/main/java/org/example/userservice/service/UserGoalServiceImpl.java`

**Trong method `toResponse(UserGoal goal)`, thêm dòng:**

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

## B2. Cross-service call — RestTemplate + HealthDataClient

### B2.1. Tạo RestTemplate config

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
 * Short timeouts (2s connect / 3s read) so that failures in downstream
 * services do not block user-facing requests. Callers MUST handle
 * exceptions and return null/fallback.
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

### B2.2. Tạo HealthDataClient service

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
import java.util.Map;

/**
 * Lightweight HTTP client to call health-data-service.
 *
 * MVP scope: fetch current weight to snapshot start_weight_kg when user
 * sets a new goal. All failures logged and return null so the goal update
 * still succeeds.
 *
 * IMPORTANT — Contract of /api/health-data/dashboard-metrics (AFTER Phase A refactor):
 * Returns DataResponse-wrapped:
 *   { code: null, message: "Success", data: { weight: { value, unit, lastUpdatedAt }, ... } }
 * Parse via body.get("data") -> map -> weight -> value.
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

            // Unwrap DataResponse: { code, message, data: { weight: { value, ... } } }
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

**Lưu ý quan trọng:** Code parse `body.get("data")` DỰA TRÊN phase A đã refactor xong. Nếu agent muốn build phase B trước phase A, phải parse `body.get("weight")` trực tiếp (raw contract).

→ **Khuyến nghị: làm Phase A trước, sau đó Phase B.** Tránh dual code path.

### B2.3. Add config to application.yml

**File:** `user-service/src/main/resources/application.yml` (hoặc tương đương)

Thêm vào section `app:`:

```yaml
app:
  services:
    health-data:
      url: ${HEALTH_DATA_SERVICE_URL:http://health-data-service:8082}
```

URL mặc định là service name trong Docker Compose. Override qua env var nếu deploy khác.

### B2.4. Wire snapshot into UserGoalServiceImpl.updateCurrent()

**File:** `user-service/src/main/java/org/example/userservice/service/UserGoalServiceImpl.java`

**Update dependencies:**

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

## B3. Verify Phase B — Postman

**Test 5: Set new goal → check startWeightKg**

```http
PUT http://localhost:8080/api/user-goals/current
Authorization: Bearer <valid_jwt>
Content-Type: application/json

{
  "goalCode": "GIAM",
  "targetWeightKg": 65,
  "targetDurationMonths": 6
}

Expected: 200 OK response.data.startWeightKg = <current_weight> (vd 70.5)
```

Verify DB:

```sql
SELECT id, goal_code, start_weight_kg, target_weight_kg, start_date
FROM user_goals
WHERE user_id = '<userId>' AND is_active = true
ORDER BY start_date DESC LIMIT 1;
```

Kỳ vọng `start_weight_kg` có giá trị (số dương, không null).

**Test 6: Stop health-data-service → set goal → graceful degrade**

```bash
docker stop health-data-service
```

Gọi `PUT /api/user-goals/current` lại → expected 200 OK, response `data.startWeightKg = null`. KHÔNG fail toàn bộ.

```bash
docker start health-data-service
```

---

# PHASE C — PROFILE DATA FIX (~30 phút)

## C1. Add @CreationTimestamp to User entity

**File:** `user-service/src/main/java/org/example/userservice/entity/User.java`

**Thêm import:**

```java
import org.hibernate.annotations.CreationTimestamp;
```

**Update field `createdAt`:**

```java
// BEFORE:
private LocalDateTime createdAt;

// AFTER:
@CreationTimestamp
@Column(name = "created_at", updatable = false)
private LocalDateTime createdAt;
```

**Lưu ý:** User cũ trong DB sẽ có `created_at = NULL`. Sau khi restart container, chạy SQL backfill:

```sql
-- Trong MySQL container:
USE user_service_db;
UPDATE user SET created_at = NOW() WHERE created_at IS NULL;

-- Verify:
SELECT id, name, created_at FROM user LIMIT 5;
```

User cũ sẽ có "ngày tham gia" = ngày migrate. Acceptable cho MVP.

## C2. Add email + createdAt to response DTOs

### C2.1. Update UserProfileResponse

**File:** `user-service/src/main/java/org/example/userservice/dto/response/UserProfileResponse.java`

```java
package org.example.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.userservice.enums.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;   // ← ADD
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String userId;
    private String username;
    private List<String> roles;
    private String name;
    private String email;             // ← ADD
    private String phone;
    private LocalDate birthDate;
    private Gender gender;
    private Boolean profileCompleted;
    private LocalDateTime createdAt;  // ← ADD

    public UserProfileResponse(String userId, String username, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
    }
}
```

### C2.2. Update UserResponseDTO

**File:** `user-service/src/main/java/org/example/userservice/dto/response/UserResponseDTO.java`

```java
package org.example.userservice.dto.response;

import lombok.Data;
import org.example.userservice.enums.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;   // ← ADD

@Data
public class UserResponseDTO {
    private String id;
    private String name;
    private String email;             // ← ADD
    private String phone;
    private LocalDate birthDate;
    private Gender gender;
    private Boolean profileCompleted;
    private LocalDateTime createdAt;  // ← ADD
}
```

### C2.3. Update mapping trong UserController.getCurrentUserProfile()

**File:** `user-service/src/main/java/org/example/userservice/controller/UserController.java`

Tìm block tạo `UserProfileResponse` (gần dòng 80-95). Sau dòng `resp.setProfileCompleted(...)`, thêm:

```java
// === ADD ===
resp.setEmail(user.getAuth() != null ? user.getAuth().getEmail() : null);
resp.setCreatedAt(user.getCreatedAt());
// === END ADD ===
```

### C2.4. Update UserMapper (cho UserResponseDTO)

**File:** Tìm `UserMapper.java` hoặc tương đương trong `user-service/src/main/java/.../mapper/`

```bash
find user-service/src/main/java -name "UserMapper*" 2>&1
```

Trong method `toDTO(User user)` (hoặc tương tự), thêm:

```java
dto.setEmail(user.getAuth() != null ? user.getAuth().getEmail() : null);
dto.setCreatedAt(user.getCreatedAt());
```

## C3. Fix phone null clear được

**File:** `user-service/src/main/java/org/example/userservice/service/UserServiceImpl.java`

**Tìm method `updateUserProfile()`, sửa logic phone:**

```java
// BEFORE:
if (newUserDataRequest.getPhone() != null &&
        !newUserDataRequest.getPhone().equals(existingUser.getPhone())) {
    existingUser.setPhone(newUserDataRequest.getPhone());
}

// AFTER (phone is optional — null means clear):
existingUser.setPhone(newUserDataRequest.getPhone());
```

**Defense — thêm validation cho required field:**

**File:** Tìm `UserRequestDTO` hoặc tương đương trong `user-service/src/main/java/.../dto/request/`

Thêm annotation:

```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class UserRequestDTO {
    @NotBlank(message = "Ten khong duoc bo trong")
    private String name;

    private String phone;   // ← optional, null OK = clear

    @NotNull(message = "Ngay sinh khong duoc bo trong")
    private LocalDate birthDate;

    @NotNull(message = "Gioi tinh khong duoc bo trong")
    private Gender gender;
}
```

Controller `updateCurrentUserProfile` đã có `@Valid` → BE tự reject nếu thiếu required field.

**⚠ Verify trước khi thêm `@NotBlank`/`@NotNull`:**

Onboarding flow Step 3 PUT `/api/user/profile` có gửi đủ 4 field (name, birthDate, gender, phone) không? Nếu có flow nào gửi partial (vd update riêng phone), thêm annotation sẽ break.

Test bằng cách: trước khi thêm `@NotBlank`, chạy test E2E onboarding 5 step. Nếu pass → thêm `@NotBlank` an toàn.

## C4. Verify Phase C — Postman

**Test 7: GET /currentUser có email + createdAt**

```http
GET http://localhost:8080/api/user/currentUser
Authorization: Bearer <valid_jwt>

Expected: response.data có
{
  "userId": "...",
  "name": "...",
  "email": "user@example.com",      // ← phải có
  "phone": "...",
  "birthDate": "...",
  "gender": "...",
  "profileCompleted": true,
  "createdAt": "2026-05-26T..."     // ← phải có
}
```

**Test 8: PUT /profile với phone null → clear**

```http
PUT http://localhost:8080/api/user/profile
Authorization: Bearer <valid_jwt>
Content-Type: application/json

{
  "name": "Test User",
  "birthDate": "2000-01-01",
  "gender": "MALE",
  "phone": null
}

Expected: 200 OK, sau đó GET /currentUser thấy phone = null
```

---

# PHASE D — CHANGE PASSWORD ENDPOINT (~1h)

## D1. Add ErrorCode

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

## D2. Tạo ChangePasswordRequest DTO

**File mới:** `user-service/src/main/java/org/example/userservice/dto/request/ChangePasswordRequest.java`

```java
package org.example.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Mat khau hien tai khong duoc bo trong")
    private String currentPassword;

    @NotBlank(message = "Mat khau moi khong duoc bo trong")
    @Size(min = 8, max = 100, message = "Mat khau moi phai tu 8 den 100 ky tu")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
        message = "Mat khau moi phai co it nhat 1 chu HOA va 1 chu so"
    )
    private String newPassword;
}
```

**Note về regex:**
- `(?=.*[A-Z])` lookahead — phải có ít nhất 1 chữ HOA
- `(?=.*\\d)` lookahead — phải có ít nhất 1 chữ số
- `.+` match phần còn lại
- → Đồng bộ với FE Zod schema (xem doc FE)

## D3. Add method to AuthService interface

**File:** `user-service/src/main/java/org/example/userservice/service/AuthService.java`

**Thêm method declaration:**

```java
public interface AuthService {
    // ... existing methods ...

    void changePassword(String userId, ChangePasswordRequest request);
}
```

## D4. Implement changePassword in AuthServiceImpl

**File:** `user-service/src/main/java/org/example/userservice/service/AuthServiceImpl.java`

**Thêm imports nếu chưa có:**

```java
import org.example.userservice.dto.request.ChangePasswordRequest;
```

**Thêm method ở cuối class:**

```java
@Override
@Transactional
public void changePassword(String userId, ChangePasswordRequest request) {
    // 1. Find user's auth record via User -> Auth relation
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    Auth auth = user.getAuth();
    if (auth == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    // 2. Verify current password
    if (!passwordEncoder.matches(request.getCurrentPassword(), auth.getPassword())) {
        throw new BusinessException(ErrorCode.CHANGE_PASSWORD_WRONG_CURRENT);
    }

    // 3. Reject same password
    if (passwordEncoder.matches(request.getNewPassword(), auth.getPassword())) {
        throw new BusinessException(ErrorCode.CHANGE_PASSWORD_SAME);
    }

    // 4. Encode and save
    auth.setPassword(passwordEncoder.encode(request.getNewPassword()));
    authRepository.save(auth);

    log.info("Password changed successfully for userId={}", userId);
    // NOTE: do NOT log raw passwords.
}
```

**⚠ Verify User entity có quan hệ tới Auth:** Đã verify, `User.auth` đã tồn tại với `@OneToOne(fetch = EAGER)`. Pattern `user.getAuth()` work.

## D5. Add endpoint to AuthController

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

## D6. Verify gateway routing

**File:** `api-gateway/src/main/resources/application.yml` (hoặc tương đương)

Verify route `/api/auth/**` đã forward tới user-service và JWT filter PASS đúng:
- `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh-token` → bypass JWT filter (public)
- `/api/auth/change-password` → require JWT filter

Nếu gateway hiện tại bypass cả `/api/auth/**`, agent BE cần update logic JwtFilter để cho phép change-password đi qua auth check. Cụ thể: thêm exception cho `change-password` path.

Tham khảo file `api-gateway/src/main/java/.../filter/JwtFilter.java` hoặc tương đương.

## D7. Verify Phase D — Postman

**Test 9: Change password thành công**

```http
PUT http://localhost:8080/api/auth/change-password
Authorization: Bearer <valid_jwt>
Content-Type: application/json

{
  "currentPassword": "OldPass123",
  "newPassword": "NewPass456"
}

Expected: 200 OK { code: null, message: "Success", data: null }
```

**Test 10: Sai mật khẩu hiện tại**

```http
{
  "currentPassword": "WrongPass",
  "newPassword": "NewPass456"
}

Expected: 400 BAD_REQUEST { code: "AUTH-011", message: "Mat khau hien tai khong dung" }
```

**Test 11: Mật khẩu mới trùng mật khẩu cũ**

```http
{
  "currentPassword": "NewPass456",
  "newPassword": "NewPass456"
}

Expected: 400 BAD_REQUEST { code: "AUTH-012" }
```

**Test 12: Mật khẩu mới không đủ policy (thiếu HOA)**

```http
{
  "currentPassword": "NewPass456",
  "newPassword": "abcdef123"
}

Expected: 400 BAD_REQUEST (validation error message từ @Pattern)
```

**Test 13: Mật khẩu mới < 8 ký tự**

```http
{
  "currentPassword": "NewPass456",
  "newPassword": "Ab1"
}

Expected: 400 BAD_REQUEST (validation error từ @Size)
```

**Test 14: Login với password mới**

```http
POST http://localhost:8080/api/auth/login
{ "username": "...", "password": "NewPass456" }

Expected: 200 OK với token mới
```

---

# PHASE E — VERIFY THE ORDER (~30 phút)

## E1. Order of Implementation

Tuần tự, KHÔNG nhảy:

1. **§A1-A6: Refactor health-data-service** → Postman Test 1-4 pass
2. **STOP — báo cáo user xem confirm Phase A xong**
3. **§B1-B3: Add startWeightKg + snapshot** → Postman Test 5-6 pass
4. **§C1-C4: Add email/createdAt + phone null fix** → Postman Test 7-8 pass
5. **§D1-D7: Change password endpoint** → Postman Test 9-14 pass
6. **STOP — báo cáo user toàn bộ BE work xong**

User sẽ test integration, nếu pass → giao agent FE thực hiện doc FE riêng.

## E2. Final acceptance criteria

Sau khi tất cả phase xong, agent BE PHẢI verify:

### E2.1. Build pass

```bash
cd /path/to/HealthManagement
.\mvnw clean install -DskipTests
docker-compose up -d --build user-service health-data-service
```

Kỳ vọng:
- Build success (KHÔNG có TypeScript/Java compile error)
- Container start lại không crash
- Hibernate ALTER TABLE chạy thành công (verify log có dòng `alter table user_goals add column start_weight_kg`)

### E2.2. Database schema verify

```sql
-- Verify user_goals có column mới
SHOW COLUMNS FROM user_goals LIKE 'start_weight_kg';

-- Verify user có column created_at và có giá trị (sau backfill)
SHOW COLUMNS FROM user LIKE 'created_at';
SELECT COUNT(*) FROM user WHERE created_at IS NULL;  -- phải = 0 sau backfill
```

### E2.3. All Postman tests pass

Test 1-14 (như §A6, §B3, §C4, §D7).

### E2.4. Regression — onboarding flow

Chạy full onboarding 5 step (FE đang dùng), verify KHÔNG break:
- Step 3 PUT `/profile` → success (vì `@NotBlank` cho `name` chỉ require nó, không break onboarding gửi đủ field)
- Step 4 POST `/submit` → success (đã wrap `DataResponse` từ trước, không đổi)
- Step 5 PUT `/user-goals/current` → success, response có `startWeightKg`

---

# F. ⚠ TRAPS CỰC KỲ DỄ SAI

## F1. Phase A: Đừng đụng logic build response

Khi refactor một controller method, **CHỈ thay đổi return type + return statement**, KHÔNG đụng logic ở giữa (build map, query DB, transform data). Đây là Top 1 nguồn bug.

## F2. Phase A: `delete()` đổi status 204 → 200

`UnitController.delete()` trước trả 204 No Content. Sau refactor trả 200 OK với `DataResponse.success()`. Nếu FE đang assume 204, sẽ break.

**Verify trước:** Test FE delete unit xem có dùng endpoint này không. Nếu có, ghi note vào commit message để FE biết phải update.

## F3. Phase B: HealthDataClient parse phụ thuộc Phase A

Code parse `body.get("data")` ở `HealthDataClient` DỰA TRÊN Phase A đã refactor `dashboard-metrics` xong. Nếu agent build Phase B trước, code sẽ break.

→ **Luôn làm Phase A trước Phase B.**

## F4. Phase B: RestTemplate timeout phải đủ ngắn

Connect 2s, read 3s. KHÔNG để default Spring (vô hạn). Nếu để default, health-data-service hang → user goal update sẽ hang vô thời hạn.

## F5. Phase B: KHÔNG throw exception khi snapshot fail

```java
// ĐÚNG:
BigDecimal startWeight = healthDataClient.fetchCurrentWeightKg(userId);
// Tiếp tục build goal kể cả null

// SAI:
BigDecimal startWeight = healthDataClient.fetchCurrentWeightKg(userId);
if (startWeight == null) throw new BusinessException(...);  // ← NO
```

Mục tiêu: user vẫn set được goal kể cả khi health-data-service down. FE sẽ handle null case.

## F6. Phase C: `@CreationTimestamp` chỉ work với INSERT mới

Sau khi thêm annotation và restart container, **user mới register sẽ có createdAt đúng**. User cũ vẫn có `created_at = NULL`. Bắt buộc chạy SQL backfill (§C1).

## F7. Phase C: KHÔNG thêm `@NotBlank` khi onboarding flow chưa verify

Nếu vội thêm `@NotBlank` cho `name` trong `UserRequestDTO`, có thể break onboarding nếu Step 3 nào đó gửi name rỗng. Verify bằng integration test trước.

## F8. Phase D: Password KHÔNG được log

```java
// SAI:
log.info("Changing password from {} to {}", req.getCurrentPassword(), req.getNewPassword());

// ĐÚNG:
log.info("Password changed successfully for userId={}", userId);
```

Defense talking point lúc bảo vệ luận: "không log password để bảo mật".

## F9. Phase D: JWT vẫn hợp lệ sau change password

MVP: BE KHÔNG invalidate JWT khi đổi password (tránh logout user khỏi tất cả device). Nếu sau này cần "logout all devices on password change", thêm `tokenVersion` field vào Auth + check trong JwtFilter.

## F10. Hibernate ddl-auto

`spring.jpa.hibernate.ddl-auto=update` trong `application.yml`. Sau khi deploy entity mới, Hibernate tự `ALTER TABLE ADD COLUMN`. **Verify:** Sau khi BE restart, kiểm tra DB:

```sql
SHOW COLUMNS FROM user_goals LIKE 'start_weight_kg';
SHOW COLUMNS FROM user LIKE 'created_at';
```

Nếu KHÔNG thấy column → kiểm tra `ddl-auto` config, restart container, check log Hibernate.

---

# G. DELIVERABLES

## Files mới

- [ ] `user-service/src/main/java/.../config/RestTemplateConfig.java`
- [ ] `user-service/src/main/java/.../service/HealthDataClient.java`
- [ ] `user-service/src/main/java/.../dto/request/ChangePasswordRequest.java`

## Files update

### Health-data-service (Phase A)
- [ ] `health-data-service/src/main/java/.../controller/HealthDataController.java` (3 endpoint)
- [ ] `health-data-service/src/main/java/.../controller/UnitController.java` (6 endpoint)
- [ ] `health-data-service/src/main/java/.../controller/HealthIndicatorConfigsController.java` (3 endpoint)

### Common
- [ ] `common/src/main/java/.../exception/ErrorCode.java` (add AUTH-011, AUTH-012, optional HEALTH-001/002)

### User-service (Phase B + C + D)
- [ ] `user-service/src/main/java/.../entity/UserGoal.java` (add `startWeightKg`)
- [ ] `user-service/src/main/java/.../entity/User.java` (add `@CreationTimestamp`)
- [ ] `user-service/src/main/java/.../dto/response/UserGoalResponse.java` (add `startWeightKg`)
- [ ] `user-service/src/main/java/.../dto/response/UserProfileResponse.java` (add `email`, `createdAt`)
- [ ] `user-service/src/main/java/.../dto/response/UserResponseDTO.java` (add `email`, `createdAt`)
- [ ] `user-service/src/main/java/.../dto/request/UserRequestDTO.java` (add `@NotBlank`/`@NotNull`)
- [ ] `user-service/src/main/java/.../mapper/UserMapper.java` (map email + createdAt — verify file path)
- [ ] `user-service/src/main/java/.../service/UserGoalServiceImpl.java` (snapshot logic)
- [ ] `user-service/src/main/java/.../service/UserServiceImpl.java` (fix phone null clear)
- [ ] `user-service/src/main/java/.../service/AuthService.java` (interface)
- [ ] `user-service/src/main/java/.../service/AuthServiceImpl.java` (changePassword impl)
- [ ] `user-service/src/main/java/.../controller/AuthController.java` (PUT endpoint)
- [ ] `user-service/src/main/java/.../controller/UserController.java` (map email + createdAt)
- [ ] `user-service/src/main/resources/application.yml` (add `app.services.health-data.url`)

## SQL migration thủ công

```sql
UPDATE user SET created_at = NOW() WHERE created_at IS NULL;
```

## Commits gợi ý

```bash
# Phase A
git commit -m "refactor(health-data): standardize all endpoints to DataResponse<T>

Wrap 11 endpoints in HealthDataController, UnitController, and
HealthIndicatorConfigsController with DataResponse<T> for consistent
API contract across the service.

Breaking change for FE: getDashboardMetrics, getLatestHealthData,
getHistoricalHealthData in healthData.service.ts must use
unwrapDataResponse() like dashboard.service.ts already does."

# Phase B
git commit -m "feat(user-goal): snapshot current weight on goal change

- Add nullable start_weight_kg column to user_goals via Hibernate
- Add lightweight RestTemplate client to fetch current weight from
  health-data-service (2s connect / 3s read timeout)
- Snapshot weight on PUT /api/user-goals/current; null fallback if
  downstream unavailable"

# Phase C
git commit -m "feat(user): add email and createdAt to profile response

- Add @CreationTimestamp to User entity (auto-fill on insert)
- Map User.auth.email to UserProfileResponse.email
- Map User.createdAt to UserProfileResponse.createdAt
- Fix updateUserProfile to allow phone null (clears the field)
- Add @NotBlank/@NotNull on UserRequestDTO required fields

SQL backfill needed: UPDATE user SET created_at = NOW() WHERE created_at IS NULL"

# Phase D
git commit -m "feat(auth): add change-password endpoint

PUT /api/auth/change-password with currentPassword + newPassword.
Enforces password policy (min 8, uppercase + digit) via Bean Validation.
Returns AUTH-011/AUTH-012 for wrong-current / same-as-current errors.
JWT remains valid after password change (no token invalidation)."
```

---

## VERSION HISTORY

| Ngày | Version | Thay đổi |
|---|---|---|
| 26/05/2026 | v1.0 | Hướng dẫn BE đầy đủ cho Profile page + refactor toàn bộ health-data-service về DataResponse<T>. 5 phase: (A) Refactor 11 endpoint, (B) startWeightKg + cross-service snapshot, (C) email/createdAt + phone null fix, (D) Change Password endpoint, (E) Final verify. 14 Postman tests, ~4-5h work. |
