# BACKEND ENDPOINT SPECIFICATION — DASHBOARD SUPPORT v3.0

> **Phiên bản này thay thế v1.0** (v1.0 sai assumption về schema và Feign).
> Sau khi clone repo và verify kiến trúc thực tế, spec được rewrite hoàn toàn.
>
> **Mục đích:** Spec các endpoint BE cần build để support Dashboard mới (xem `dashboard_spec.md`).


---

## 0. KIẾN TRÚC HIỆN TẠI — CẦN HIỂU TRƯỚC KHI CODE

### 0.1. Data flow tổng quan

```
user-service (DB: user_db)
├── User entity:        id, name, birthDate, gender, ...
├── Auth entity:        username, password, role, ...
└── (cần thêm)          UserPreference entity
        │
        │ RabbitMQ events (đã có pattern):
        │ - UserCreatedEvent
        │ - UserProfileUpdatedEvent
        │ - (cần thêm) UserPreferencesUpdatedEvent
        ▼
health-data-service (DB: health_data_db)
├── UserForHealthData:           mirror userId, birthDate, gender (đã có)
├── (cần thêm) UserPreferenceMirror   mirror pbf_method
├── BaseMetricValue:             EAV table: HEIGHT/WEIGHT/WAIST/HIP/NECK/ACTIVITY_FACTOR → Double
└── CalculatedMetricSnapshot:    EAV table: BMI/BMR/TDEE/PBF/WHR → Double (auto-tính)
```

### 0.2. Compute pipeline đã có

Khi user submit health data (POST `/api/health-data/submit`):
1. `HealthDataSubmitService.processSubmittedHealthData()` lưu base metrics
2. Auto trigger `CalculatedMetricService.recalculateAndSaveDerivedMetrics()`
3. Pipeline tính BMI, BMR, TDEE, PBF, WHR bằng `HealthCalculator` và lưu `CalculatedMetricSnapshot`

→ Khi user submit cân + chiều cao + vòng eo + cổ + hông → PBF tự tính, không cần FE/dashboard service tự compute.

### 0.3. Decisions đã chốt v3.0

| Decision | Lựa chọn | Lý do |
|---|---|---|
| Sync user profile cross-service | **RabbitMQ event** (theo pattern hiện tại) | Đã có, không cần Feign |
| Lưu `pbf_method` ở đâu | Bảng `user_preferences` ở **user-service** | Mở rộng cho nhiều preference |
| Sync `pbf_method` sang health-data-service | RabbitMQ event mới `UserPreferencesUpdatedEvent` | Consistency với pattern hiện tại |
| Logic phân loại thể trạng | BE only, endpoint mới ở `health-data-service` | Single source of truth |
| Constants BMI/PBF threshold | Hard-code trong util class | MVP, không cần admin config phức tạp |
| Class util phân loại | `BodyClassifier.java` tách khỏi `HealthCalculator` | SoC: HealthCalculator compute, BodyClassifier classify |
| **Lưu mục tiêu user** | **Bảng `user_goals` ở user-service** | Time-bound (3-6 tháng), track lịch sử, future "đề xuất đổi mục tiêu" |
| **Suggestion goal theo thể trạng** | **Map hard-code trong service** | Đơn giản, tái sử dụng matrix warning hiện có |
| **`profile_completed` flag** | **Column trong `User` entity** | Đơn giản nhất, không cần bảng riêng |

### 0.4. Convention (giữ nguyên project hiện tại)

- Entity package: `org.example.<service>service.entity.<sub-package>`
- Service pattern: Interface + Impl (match style hiện có của project)
- DTO suffix: `Request`, `Response`
- Code comment: tiếng Việt
- Commit prefix: `feat:`, `fix:`, `refactor:`
- Response wrapper: `DataResponse<T>` từ `org.example.web.dto.response`
- Exception: `BusinessException` từ `org.example.web.exception`

---

## 1. SCHEMA `user_preferences` (user-service)

### 1.1. Entity

**File:** `user-service/src/main/java/org/example/userservice/entity/UserPreference.java`

```java
package org.example.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@IdClass(UserPreferenceId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Id
    @Column(name = "pref_key", length = 100, nullable = false)
    private String prefKey;

    @Column(name = "pref_value", nullable = false, length = 500)
    private String prefValue;

    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType;  // STRING, INTEGER, BOOLEAN, JSON

    @Column(name = "description", length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

**File:** `UserPreferenceId.java`

```java
package org.example.userservice.entity;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserPreferenceId implements Serializable {
    private String userId;
    private String prefKey;
}
```

### 1.2. Repository

```java
package org.example.userservice.repository;

import org.example.userservice.entity.UserPreference;
import org.example.userservice.entity.UserPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPreferenceRepository
        extends JpaRepository<UserPreference, UserPreferenceId> {

    List<UserPreference> findByUserId(String userId);

    Optional<UserPreference> findByUserIdAndPrefKey(String userId, String prefKey);

    void deleteByUserIdAndPrefKey(String userId, String prefKey);
}
```

### 1.3. Initial keys (MVP)

| Key | Type | Default | Mô tả |
|---|---|---|---|
| `pbf_method` | STRING | `FORMULA` | `FORMULA` (Navy) hoặc `MODEL_1` (ML, future) |

Future keys: `language`, `theme`, `notification_dismissed_types`, etc.

### 1.4. Seed default khi register

Update `AuthServiceImpl.registerUser()` — thêm sau khi save User:

```java
// === MỚI: seed default preferences ===
userPreferenceRepo.save(UserPreference.builder()
    .userId(user.getId())
    .prefKey("pbf_method")
    .prefValue("FORMULA")
    .valueType("STRING")
    .description("Method tinh PBF: FORMULA (Navy) hoac MODEL_1 (ML)")
    .build());

// Existing: publish UserCreatedEvent
// ... (giữ nguyên)
```

---

## 2. ENDPOINTS USER PREFERENCES (user-service)

### 2.1. Endpoints

| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/user-preferences` | List all preferences |
| GET | `/api/user-preferences/{prefKey}` | Get 1 preference |
| PUT | `/api/user-preferences/{prefKey}` | Update/upsert |
| DELETE | `/api/user-preferences/{prefKey}` | Delete (về default) |

### 2.2. DTOs

```java
// dto/response/PreferenceResponse.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PreferenceResponse {
    private String prefKey;
    private String prefValue;
    private String valueType;
    private String description;
}

// dto/request/PreferenceUpdateRequest.java
@Data
public class PreferenceUpdateRequest {
    @NotBlank
    private String prefValue;
    private String valueType;  // optional
}
```

### 2.3. Service

**File:** `service/UserPreferenceService.java`

```java
public interface UserPreferenceService {
    List<PreferenceResponse> getAll(String userId);
    Optional<PreferenceResponse> getOne(String userId, String prefKey);
    PreferenceResponse upsert(String userId, String prefKey, PreferenceUpdateRequest req);
    void delete(String userId, String prefKey);
}
```

**File:** `service/UserPreferenceServiceImpl.java`

```java
package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.events.UserPreferencesUpdatedEvent;
import org.example.userservice.dto.request.PreferenceUpdateRequest;
import org.example.userservice.dto.response.PreferenceResponse;
import org.example.userservice.entity.UserPreference;
import org.example.userservice.repository.UserPreferenceRepository;
import org.example.web.exception.BusinessException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceRepository repo;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.user-events}")
    private String userEventsExchangeName;

    @Value("${app.rabbitmq.routing-key.user-preferences-updated}")
    private String prefsUpdatedRoutingKey;

    // Whitelist các key được phép (tránh user inject key bậy)
    private static final Set<String> ALLOWED_KEYS = Set.of("pbf_method");

    @Override
    public List<PreferenceResponse> getAll(String userId) {
        return repo.findByUserId(userId).stream()
            .map(this::toResponse).toList();
    }

    @Override
    public Optional<PreferenceResponse> getOne(String userId, String prefKey) {
        return repo.findByUserIdAndPrefKey(userId, prefKey).map(this::toResponse);
    }

    @Override
    @Transactional
    public PreferenceResponse upsert(String userId, String prefKey,
                                     PreferenceUpdateRequest req) {
        if (!ALLOWED_KEYS.contains(prefKey)) {
            throw new BusinessException("INVALID_PREF_KEY",
                "Key '" + prefKey + "' khong duoc ho tro");
        }
        validateValueForKey(prefKey, req.getPrefValue());

        UserPreference pref = repo.findByUserIdAndPrefKey(userId, prefKey)
            .orElse(UserPreference.builder()
                .userId(userId).prefKey(prefKey).build());

        pref.setPrefValue(req.getPrefValue());
        pref.setValueType(req.getValueType() != null ? req.getValueType() : "STRING");
        UserPreference saved = repo.save(pref);

        publishEvent(userId, prefKey, req.getPrefValue());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(String userId, String prefKey) {
        repo.deleteByUserIdAndPrefKey(userId, prefKey);
        publishEvent(userId, prefKey, null);  // null báo cho consumer biết user revert default
    }

    private void validateValueForKey(String key, String value) {
        switch (key) {
            case "pbf_method":
                if (!Set.of("FORMULA", "MODEL_1").contains(value)) {
                    throw new BusinessException("INVALID_PREF_VALUE",
                        "pbf_method phai la FORMULA hoac MODEL_1");
                }
                break;
        }
    }

    private void publishEvent(String userId, String prefKey, String prefValue) {
        try {
            UserPreferencesUpdatedEvent event = new UserPreferencesUpdatedEvent(
                userId, prefKey, prefValue);
            rabbitTemplate.convertAndSend(userEventsExchangeName,
                prefsUpdatedRoutingKey, event);
            log.info("Published UserPreferencesUpdatedEvent userId={} key={}",
                userId, prefKey);
        } catch (Exception e) {
            log.error("Failed publish event: {}", e.getMessage(), e);
            // KHÔNG throw — event fail không block user request
        }
    }

    private PreferenceResponse toResponse(UserPreference pref) {
        return PreferenceResponse.builder()
            .prefKey(pref.getPrefKey())
            .prefValue(pref.getPrefValue())
            .valueType(pref.getValueType())
            .description(pref.getDescription())
            .build();
    }
}
```

### 2.4. Controller

```java
package org.example.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.request.PreferenceUpdateRequest;
import org.example.userservice.dto.response.PreferenceResponse;
import org.example.userservice.service.UserPreferenceService;
import org.example.web.dto.response.DataResponse;
import org.example.web.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService service;

    @GetMapping
    public ResponseEntity<DataResponse<List<PreferenceResponse>>> getAll(
            @RequestHeader("userId") String userId) {
        return ResponseEntity.ok(DataResponse.success(service.getAll(userId)));
    }

    @GetMapping("/{prefKey}")
    public ResponseEntity<DataResponse<PreferenceResponse>> getOne(
            @RequestHeader("userId") String userId,
            @PathVariable String prefKey) {
        PreferenceResponse data = service.getOne(userId, prefKey)
            .orElseThrow(() -> new BusinessException("NOT_FOUND",
                "Khong tim thay preference"));
        return ResponseEntity.ok(DataResponse.success(data));
    }

    @PutMapping("/{prefKey}")
    public ResponseEntity<DataResponse<PreferenceResponse>> upsert(
            @RequestHeader("userId") String userId,
            @PathVariable String prefKey,
            @Valid @RequestBody PreferenceUpdateRequest req) {
        return ResponseEntity.ok(DataResponse.success(
            service.upsert(userId, prefKey, req)));
    }

    @DeleteMapping("/{prefKey}")
    public ResponseEntity<DataResponse<Void>> delete(
            @RequestHeader("userId") String userId,
            @PathVariable String prefKey) {
        service.delete(userId, prefKey);
        return ResponseEntity.ok(DataResponse.success());
    }
}
```

---

## 3. EVENT MỚI + RABBITMQ CONFIG

### 3.1. Common module — event class

**File:** `common/src/main/java/org/example/events/UserPreferencesUpdatedEvent.java`

```java
package org.example.events;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class UserPreferencesUpdatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String prefKey;
    private String prefValue;  // null nếu user delete preference

    public UserPreferencesUpdatedEvent(String userId, String prefKey, String prefValue) {
        this.userId = userId;
        this.prefKey = prefKey;
        this.prefValue = prefValue;
    }
}
```

### 3.2. user-service RabbitMQ config

Update `application.yml` (hoặc properties tương đương):

```yaml
app:
  rabbitmq:
    exchange:
      user-events: user-events-exchange  # EXISTING
    routing-key:
      user-created: user.created                       # EXISTING
      user-profile-updated: user.profile.updated       # EXISTING
      user-preferences-updated: user.preferences.updated  # MỚI
```

Update RabbitMQ Java config của user-service: thêm `@Bean` cho routing key mới (tham khảo pattern 2 routing key cũ).

### 3.3. health-data-service RabbitMQ config

Update `application.yml`:

```yaml
app:
  rabbitmq:
    queue:
      health-data-user-created: ...                  # EXISTING
      health-data-user-profile-updated: ...          # EXISTING
      health-data-user-preferences-updated: health-data.user-preferences-updated  # MỚI
```

Update `HealthDataRabbitMQConfig.java`:
- Declare `Queue` mới với name từ properties
- Declare `Binding` với `user-events-exchange`, routing key `user.preferences.updated`

(Tham khảo declaration của 2 queue cũ trong file đó.)

### 3.4. Listener mới — health-data-service

Update `UserEventListener.java`:

```java
@Autowired
private UserPreferenceMirrorService userPreferenceMirrorService;

@RabbitListener(queues = "${app.rabbitmq.queue.health-data-user-preferences-updated}")
public void handleUserPreferencesUpdatedEvent(@Payload UserPreferencesUpdatedEvent event) {
    String userId = event.getUserId();
    log.info("Received UserPreferencesUpdatedEvent userId={} key={} value={}",
        userId, event.getPrefKey(), event.getPrefValue());
    try {
        userPreferenceMirrorService.saveOrUpdate(userId,
            event.getPrefKey(), event.getPrefValue());
        log.info("Successfully synced preference for userId: {}", userId);
    } catch (Exception e) {
        log.error("Error syncing preference for userId {}: {}", userId, e.getMessage(), e);
        throw new RuntimeException("Failed to sync preference", e);
    }
}
```

---

## 4. MIRROR PREFERENCES (health-data-service)

### 4.1. Entity

**File:** `entity/UserPreferenceMirror.java`

```java
package org.example.healthdataservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preference_mirror")
@IdClass(UserPreferenceMirrorId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceMirror {

    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Id
    @Column(name = "pref_key", length = 100, nullable = false)
    private String prefKey;

    @Column(name = "pref_value", nullable = false, length = 500)
    private String prefValue;

    @UpdateTimestamp
    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;
}
```

**File:** `UserPreferenceMirrorId.java` — composite PK (pattern giống `UserPreferenceId`).

### 4.2. Repository

```java
public interface UserPreferenceMirrorRepository
        extends JpaRepository<UserPreferenceMirror, UserPreferenceMirrorId> {

    Optional<UserPreferenceMirror> findByUserIdAndPrefKey(String userId, String prefKey);

    void deleteByUserIdAndPrefKey(String userId, String prefKey);
}
```

### 4.3. Service interface + impl

**File:** `service/UserPreferenceMirrorService.java`

```java
public interface UserPreferenceMirrorService {
    void saveOrUpdate(String userId, String prefKey, String prefValue);
    Optional<String> getValue(String userId, String prefKey);
    String getValueOrDefault(String userId, String prefKey, String defaultValue);
}
```

**File:** `service/UserPreferenceMirrorServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
public class UserPreferenceMirrorServiceImpl implements UserPreferenceMirrorService {

    private final UserPreferenceMirrorRepository repo;

    @Override
    @Transactional
    public void saveOrUpdate(String userId, String prefKey, String prefValue) {
        if (prefValue == null) {
            // User delete pref → xóa mirror
            repo.deleteByUserIdAndPrefKey(userId, prefKey);
            return;
        }
        UserPreferenceMirror mirror = repo.findByUserIdAndPrefKey(userId, prefKey)
            .orElse(new UserPreferenceMirror());
        mirror.setUserId(userId);
        mirror.setPrefKey(prefKey);
        mirror.setPrefValue(prefValue);
        repo.save(mirror);
    }

    @Override
    public Optional<String> getValue(String userId, String prefKey) {
        return repo.findByUserIdAndPrefKey(userId, prefKey)
            .map(UserPreferenceMirror::getPrefValue);
    }

    @Override
    public String getValueOrDefault(String userId, String prefKey, String defaultValue) {
        return getValue(userId, prefKey).orElse(defaultValue);
    }
}
```

---

## 5. UTIL `BodyClassifier` (health-data-service)

**File:** `util/BodyClassifier.java`

```java
package org.example.healthdataservice.util;

import org.example.healthdataservice.entity.enums.Gender;
import org.springframework.stereotype.Component;

/**
 * Phan loai the trang theo BMI va PBF (rule-based).
 * Nguon academic:
 * - BMI: WHO Western Pacific Region 2000 (Asian cutoff)
 * - PBF: Pasco et al. 2024, PMC11913102, NHANES n=16,918, gender-specific
 *
 * Worst-case principle (max(bmiClass, pbfClass)) bat duoc Normal Weight Obesity.
 */
@Component
public class BodyClassifier {

    // BMI thresholds (WHO Asian)
    private static final double BMI_UNDERWEIGHT = 18.5;
    private static final double BMI_NORMAL_HIGH = 23.0;
    private static final double BMI_OVERWEIGHT_HIGH = 25.0;

    // PBF thresholds (Pasco et al. 2024)
    private static final double PBF_MALE_LOW = 8.0;
    private static final double PBF_MALE_NORMAL_HIGH = 25.0;
    private static final double PBF_MALE_OVERWEIGHT_HIGH = 30.0;

    private static final double PBF_FEMALE_LOW = 21.0;
    private static final double PBF_FEMALE_NORMAL_HIGH = 36.0;
    private static final double PBF_FEMALE_OVERWEIGHT_HIGH = 42.0;

    public static final String[] CONSTITUTION_NAMES = {
        "GAY", "CAN_DOI", "THUA_CAN", "BEO_PHI"
    };

    public Integer classifyByBmi(Double bmi) {
        if (bmi == null) return null;
        if (bmi < BMI_UNDERWEIGHT) return 0;
        if (bmi < BMI_NORMAL_HIGH) return 1;
        if (bmi < BMI_OVERWEIGHT_HIGH) return 2;
        return 3;
    }

    public Integer classifyByPbf(Double pbf, Gender gender) {
        if (pbf == null || gender == null) return null;

        if (gender == Gender.FEMALE) {
            if (pbf < PBF_FEMALE_LOW) return 0;
            if (pbf < PBF_FEMALE_NORMAL_HIGH) return 1;
            if (pbf < PBF_FEMALE_OVERWEIGHT_HIGH) return 2;
            return 3;
        } else {  // MALE
            if (pbf < PBF_MALE_LOW) return 0;
            if (pbf < PBF_MALE_NORMAL_HIGH) return 1;
            if (pbf < PBF_MALE_OVERWEIGHT_HIGH) return 2;
            return 3;
        }
    }

    /**
     * Lay nhom te hon giua BMI va PBF (worst case).
     * Bat duoc Normal Weight Obesity.
     */
    public Integer classifyFinal(Integer bmiClass, Integer pbfClass) {
        if (bmiClass == null) return null;
        if (pbfClass == null) return bmiClass;
        return Math.max(bmiClass, pbfClass);
    }

    public String classNameOf(Integer classIndex) {
        if (classIndex == null || classIndex < 0 || classIndex > 3) return null;
        return CONSTITUTION_NAMES[classIndex];
    }
}
```

---

## 6. ENDPOINT CONSTITUTION

### 6.1. DTO

**File:** `dto/response/ConstitutionResponse.java`

```java
package org.example.healthdataservice.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConstitutionResponse {
    private String constitution;       // "GAY" | "CAN_DOI" | "THUA_CAN" | "BEO_PHI"
    private String method;             // "RULE_BMI_PBF" | "MODEL_2"
    private Double bmi;                 // null nếu chưa có data
    private Double pbf;                 // null nếu chưa có data
    private String pbfSource;           // "FORMULA" | "MODEL_1"
    private Integer bmiClass;
    private Integer pbfClass;
    private Integer finalClass;
    private String warning;             // null hoặc message
    private LocalDateTime computedAt;
}
```

### 6.2. Service interface + impl

**File:** `service/BodyClassificationService.java`

```java
public interface BodyClassificationService {
    ConstitutionResponse classifyCurrent(String userId);
}
```

**File:** `service/BodyClassificationServiceImpl.java`

```java
package org.example.healthdataservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.healthdataservice.dto.response.ConstitutionResponse;
import org.example.healthdataservice.entity.CalculatedMetricSnapshot;
import org.example.healthdataservice.entity.UserForHealthData;
import org.example.healthdataservice.entity.enums.Gender;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.util.BodyClassifier;
import org.example.web.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BodyClassificationServiceImpl implements BodyClassificationService {

    private final CalculatedMetricService calculatedMetricService;
    private final UserProfileMirrorService userProfileMirrorService;
    private final UserPreferenceMirrorService userPreferenceMirrorService;
    private final BodyClassifier bodyClassifier;

    @Override
    public ConstitutionResponse classifyCurrent(String userId) {
        // 1. Profile (gender) - REQUIRED
        Optional<UserForHealthData> profileOpt = userProfileMirrorService.getUserProfile(userId);
        if (profileOpt.isEmpty()) {
            throw new BusinessException("MISSING_PROFILE",
                "Profile chua duoc dong bo. Vui long thu lai sau.");
        }
        Gender gender = profileOpt.get().getGender();
        if (gender == null) {
            throw new BusinessException("MISSING_GENDER",
                "Can thong tin gioi tinh trong profile");
        }

        // 2. BMI từ calculated_metric_snapshots (đã có sẵn)
        Optional<CalculatedMetricSnapshot> bmiOpt =
            calculatedMetricService.getLatestSnapshot(userId, IndicatorType.BMI);
        if (bmiOpt.isEmpty()) {
            throw new BusinessException("MISSING_BASIC_DATA",
                "Can chieu cao va can nang de xac dinh the trang");
        }
        Double bmi = bmiOpt.get().getValue();

        // 3. PBF từ calculated_metric_snapshots (đã có sẵn từ pipeline)
        Optional<CalculatedMetricSnapshot> pbfOpt =
            calculatedMetricService.getLatestSnapshot(userId, IndicatorType.PBF);
        Double pbf = pbfOpt.map(CalculatedMetricSnapshot::getValue).orElse(null);

        // 4. pbf_method từ preferences mirror
        String pbfMethod = userPreferenceMirrorService.getValueOrDefault(
            userId, "pbf_method", "FORMULA"
        );

        // 5. Warning
        String warning = (pbf == null) ? buildMissingPbfWarning(gender) : null;

        // 6. Classify
        Integer bmiClass = bodyClassifier.classifyByBmi(bmi);
        Integer pbfClass = bodyClassifier.classifyByPbf(pbf, gender);
        Integer finalClass = bodyClassifier.classifyFinal(bmiClass, pbfClass);

        // 7. Build response
        return ConstitutionResponse.builder()
            .constitution(bodyClassifier.classNameOf(finalClass))
            .method("RULE_BMI_PBF")  // V2.0: "MODEL_2" khi ML wired
            .bmi(bmi)
            .pbf(pbf)
            .pbfSource(pbfMethod)
            .bmiClass(bmiClass)
            .pbfClass(pbfClass)
            .finalClass(finalClass)
            .warning(warning)
            .computedAt(LocalDateTime.now())
            .build();
    }

    private String buildMissingPbfWarning(Gender gender) {
        if (gender == Gender.FEMALE) {
            return "Thieu vong eo, co, hoac hong - chi phan loai theo BMI";
        }
        return "Thieu vong eo hoac co - chi phan loai theo BMI";
    }
}
```

### 6.3. Controller

Thêm endpoint vào `HealthDataController.java` (hoặc tạo controller riêng):

```java
// Inject thêm
private final BodyClassificationService bodyClassificationService;

@GetMapping("/constitution")
public ResponseEntity<DataResponse<ConstitutionResponse>> getConstitution(
        @RequestHeader("userId") String userId) {
    if (userId == null) {
        return ResponseEntity.badRequest().build();
    }
    ConstitutionResponse data = bodyClassificationService.classifyCurrent(userId);
    return ResponseEntity.ok(DataResponse.success(data));
}
```

---

### 6.4. Update endpoint Constitution để bao gồm `suggestedGoal`

Update `ConstitutionResponse` DTO thêm field:

```java
private String suggestedGoal;       // "GIAM" | "DUY_TRI" | "TANG"
```

Update `BodyClassificationServiceImpl.classifyCurrent()` cuối method:

```java
String suggestedGoal = suggestGoalForConstitution(finalClass);

return ConstitutionResponse.builder()
    // ... existing fields
    .suggestedGoal(suggestedGoal)
    .build();
```

```java
/**
 * Map thể trạng sang mục tiêu phù hợp.
 * Mapping:
 * - GAY (0)      → TANG
 * - CAN_DOI (1)  → DUY_TRI
 * - THUA_CAN (2) → GIAM
 * - BEO_PHI (3)  → GIAM
 */
private String suggestGoalForConstitution(Integer finalClass) {
    if (finalClass == null) return null;
    return switch (finalClass) {
        case 0 -> "TANG";
        case 1 -> "DUY_TRI";
        case 2, 3 -> "GIAM";
        default -> null;
    };
}
```

---

## 7. SCHEMA & ENDPOINTS `user_goals` (user-service)

### 7.1. Entity

**File:** `user-service/src/main/java/org/example/userservice/entity/UserGoal.java`

```java
package org.example.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.userservice.enums.GoalCode;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_goals", indexes = {
    @Index(name = "idx_user_active", columnList = "user_id, is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGoal {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_code", length = 20, nullable = false)
    private GoalCode goalCode;  // GIAM | DUY_TRI | TANG

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "target_weight_kg", precision = 5, scale = 2)
    private BigDecimal targetWeightKg;

    @Column(name = "target_duration_months")
    private Integer targetDurationMonths;  // default 6 (3-6 tháng)

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

Cần thêm enum `GoalCode` ở `enums/GoalCode.java` (verify có chưa, nếu chưa thì tạo):

```java
package org.example.userservice.enums;

public enum GoalCode {
    GIAM, DUY_TRI, TANG
}
```

### 7.2. Repository

```java
package org.example.userservice.repository;

import org.example.userservice.entity.UserGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserGoalRepository extends JpaRepository<UserGoal, String> {

    Optional<UserGoal> findByUserIdAndIsActiveTrue(String userId);

    List<UserGoal> findByUserIdOrderByStartDateDesc(String userId);

    @Modifying
    @Query("UPDATE UserGoal g SET g.isActive = false, g.endDate = :endDate "
         + "WHERE g.userId = :userId AND g.isActive = true")
    int deactivateCurrentGoal(@Param("userId") String userId,
                              @Param("endDate") LocalDate endDate);
}
```

### 7.3. Endpoints

| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/user-goals/current` | Lấy goal đang active |
| PUT | `/api/user-goals/current` | Đổi goal (upsert: deactivate cũ + insert mới) |
| GET | `/api/user-goals/history` | Lịch sử goals |

### 7.4. DTOs

```java
// dto/response/UserGoalResponse.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserGoalResponse {
    private String id;
    private String goalCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private BigDecimal targetWeightKg;
    private Integer targetDurationMonths;
    private String note;
}

// dto/request/UpdateGoalRequest.java
@Data
public class UpdateGoalRequest {
    @NotNull
    private GoalCode goalCode;

    private BigDecimal targetWeightKg;

    @Min(1) @Max(24)
    private Integer targetDurationMonths;  // default 6 nếu null

    private String note;
}
```

### 7.5. Service

**File:** `service/UserGoalService.java`

```java
public interface UserGoalService {
    Optional<UserGoalResponse> getCurrent(String userId);
    List<UserGoalResponse> getHistory(String userId);
    UserGoalResponse updateCurrent(String userId, UpdateGoalRequest req);
}
```

**File:** `service/UserGoalServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserGoalServiceImpl implements UserGoalService {

    private final UserGoalRepository repo;

    @Override
    public Optional<UserGoalResponse> getCurrent(String userId) {
        return repo.findByUserIdAndIsActiveTrue(userId).map(this::toResponse);
    }

    @Override
    public List<UserGoalResponse> getHistory(String userId) {
        return repo.findByUserIdOrderByStartDateDesc(userId).stream()
            .map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public UserGoalResponse updateCurrent(String userId, UpdateGoalRequest req) {
        LocalDate today = LocalDate.now();

        // 1. Deactivate goal cũ (nếu có)
        repo.deactivateCurrentGoal(userId, today);

        // 2. Insert goal mới
        UserGoal newGoal = UserGoal.builder()
            .id(UUID.randomUUID().toString())
            .userId(userId)
            .goalCode(req.getGoalCode())
            .startDate(today)
            .endDate(null)
            .isActive(true)
            .targetWeightKg(req.getTargetWeightKg())
            .targetDurationMonths(req.getTargetDurationMonths() != null
                ? req.getTargetDurationMonths() : 6)
            .note(req.getNote())
            .build();

        UserGoal saved = repo.save(newGoal);
        log.info("Updated goal for userId {}: {}", userId, req.getGoalCode());
        return toResponse(saved);
    }

    private UserGoalResponse toResponse(UserGoal g) {
        return UserGoalResponse.builder()
            .id(g.getId())
            .goalCode(g.getGoalCode().name())
            .startDate(g.getStartDate())
            .endDate(g.getEndDate())
            .isActive(g.getIsActive())
            .targetWeightKg(g.getTargetWeightKg())
            .targetDurationMonths(g.getTargetDurationMonths())
            .note(g.getNote())
            .build();
    }
}
```

### 7.6. Controller

```java
@RestController
@RequestMapping("/api/user-goals")
@RequiredArgsConstructor
public class UserGoalController {

    private final UserGoalService service;

    @GetMapping("/current")
    public ResponseEntity<DataResponse<UserGoalResponse>> getCurrent(
            @RequestHeader("userId") String userId) {
        UserGoalResponse data = service.getCurrent(userId)
            .orElseThrow(() -> new BusinessException("NO_ACTIVE_GOAL",
                "Chua co muc tieu nao duoc dat"));
        return ResponseEntity.ok(DataResponse.success(data));
    }

    @PutMapping("/current")
    public ResponseEntity<DataResponse<UserGoalResponse>> updateCurrent(
            @RequestHeader("userId") String userId,
            @Valid @RequestBody UpdateGoalRequest req) {
        return ResponseEntity.ok(DataResponse.success(
            service.updateCurrent(userId, req)));
    }

    @GetMapping("/history")
    public ResponseEntity<DataResponse<List<UserGoalResponse>>> getHistory(
            @RequestHeader("userId") String userId) {
        return ResponseEntity.ok(DataResponse.success(service.getHistory(userId)));
    }
}
```

---

## 8. PROFILE COMPLETED FLAG (user-service)

### 8.1. Schema

Add column vào `User` entity:

```java
@Entity
@Table(name = "user")
@Data
public class User {
    // ... existing fields

    @Column(name = "profile_completed", nullable = false)
    private Boolean profileCompleted = false;  // default false
}
```

Hibernate `ddl-auto=update` sẽ tự add column.

### 8.2. Endpoint

**`PUT /api/user/profile-completed`** — set flag = true cho user hiện tại.

```java
// Trong UserController hoặc UserController hiện có:
@PutMapping("/profile-completed")
public ResponseEntity<DataResponse<Void>> markCompleted(
        @RequestHeader("userId") String userId) {
    userService.markProfileCompleted(userId);
    return ResponseEntity.ok(DataResponse.success());
}

// Trong UserService:
@Transactional
public void markProfileCompleted(String userId) {
    User user = userRepo.findById(userId)
        .orElseThrow(() -> new BusinessException("NOT_FOUND", "User khong ton tai"));
    user.setProfileCompleted(true);
    userRepo.save(user);
}
```

### 8.3. Expose flag trong `currentUser` response

Verify endpoint `GET /api/user/currentUser` đã trả về `profileCompleted` chưa. Nếu chưa:

```java
@Data @Builder
public class CurrentUserResponse {
    private String id;
    private String username;
    private String name;
    private LocalDate birthDate;
    private String gender;
    private String phone;
    private Boolean profileCompleted;  // MỚI
    // ... other fields
}
```

FE check `user.profileCompleted === false` → redirect onboarding.

---

## 9. ISSUE PHÁT HIỆN TRONG CODE HIỆN TẠI (optional fix)

### 9.1. `HealthCalculator.calculatePBF()` require age không cần thiết

Hiện tại code:

```java
public Double calculatePBF(String gender, Double waistCm, Double hipCm, Double neckCm,
                           Double heightCm, Double ageYears) {
    if (... ageYears == null || ageYears <=0) {
        return null;  // <-- Dead constraint
    }
    ...
}
```

**Vấn đề:** Công thức Navy KHÔNG có age parameter. Comment trong code ghi "PBF Hải quân Mỹ có yếu tố tuổi" là sai. Constraint `ageYears != null` làm PBF luôn null nếu user thiếu birthDate, dù đã có đủ vòng eo/cổ/hông.

**Fix:** Bỏ check `ageYears`, không truyền `age` parameter vào method (hoặc giữ nhưng không validate). Tham khảo công thức gốc: https://en.wikipedia.org/wiki/Body_fat_percentage#US_Navy_method

**Mức ưu tiên:** Low — fix khi rảnh. Hiện tại nếu user đã có age (từ birthDate sync về `UserForHealthData`), PBF vẫn được tính.

---

## 10. GATEWAY ROUTING

Verify `api-gateway/application.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Existing route — đã catch /api/health-data/**
        # → /api/health-data/constitution tự match, KHÔNG cần thêm

        # MỚI: route cho user-preferences
        - id: user-preferences
          uri: lb://user-service
          predicates:
            - Path=/api/user-preferences/**

        # MỚI: route cho user-goals
        - id: user-goals
          uri: lb://user-service
          predicates:
            - Path=/api/user-goals/**

        # Existing route user-service đã catch /api/user/**
        # → /api/user/profile-completed tự match
```

---

## 11. ACCEPTANCE CRITERIA

### 9.1. Schema migration

- [ ] Bảng `user_preferences` (user-service) tự tạo qua Hibernate `ddl-auto=update`
- [ ] Bảng `user_preference_mirror` (health-data-service) tự tạo
- [ ] Verify composite PK: `SHOW INDEX FROM user_preferences;` thấy 2 cột PK

### 9.2. Default seed khi register

- [ ] Register user mới → row `pbf_method=FORMULA` insert vào user_db
- [ ] Sau 1-2s (RabbitMQ): row tương tự xuất hiện trong user_preference_mirror

### 9.3. Endpoints user-preferences

- [ ] `GET /api/user-preferences` → list 1 item `pbf_method=FORMULA`
- [ ] `PUT /api/user-preferences/pbf_method` body `{"prefValue":"MODEL_1"}` → 200
- [ ] Sau 1-2s: mirror DB cũng update value MODEL_1
- [ ] `PUT` với invalid value `"INVALID"` → 400 `INVALID_PREF_VALUE`
- [ ] `PUT` với invalid key `random_key` → 400 `INVALID_PREF_KEY`
- [ ] `DELETE` → row biến mất ở cả 2 DB

### 9.4. Endpoint constitution

**TC1 — Empty data:** User chưa submit gì → 422 `MISSING_BASIC_DATA`

**TC2 — BMI only (thiếu vòng đo):** Submit chỉ height + weight:
- 200 với `bmi` có value, `pbf=null`, `pbfClass=null`, `finalClass=bmiClass`, `warning` có text

**TC3 — Full data:** Submit đầy đủ → 200 với `bmi, pbf` đều có, `finalClass=max(bmiClass, pbfClass)`

**TC4 — Normal Weight Obesity:**
- BMI ≈ 22 (class 1) + PBF > 25% (nam, class 2) → `finalClass=2` (THUA_CAN, không phải CAN_DOI)
- **Defense talking point**

**TC5 — Missing gender:** Profile chưa có gender → 422 `MISSING_GENDER`

### 9.5. Event sync

- [ ] Update profile gender qua user-service → constitution endpoint phản ánh sau ≤ 5s
- [ ] Update pbf_method → response field `pbfSource` cập nhật

---

## 12. ORDER OF IMPLEMENTATION

**Phase 1 — Common:**
1. Tạo `UserPreferencesUpdatedEvent` trong `common/events`

**Phase 2 — User Service:**
2. Entity `UserPreference` + `UserPreferenceId` + Repository
3. `UserPreferenceService` + Impl + validation whitelist
4. `UserPreferenceController`
5. Update `AuthServiceImpl.registerUser()` seed default
6. RabbitMQ config: routing key `user.preferences.updated`
7. Test endpoints qua Postman (verify DB save + RabbitMQ publish)

**Phase 3 — Health-Data Service:**
8. Entity `UserPreferenceMirror` + Id + Repository
9. `UserPreferenceMirrorService` + Impl
10. Update `UserEventListener` thêm handler mới
11. Update `HealthDataRabbitMQConfig`: queue + binding mới
12. Util `BodyClassifier` với constants + methods
13. DTO `ConstitutionResponse`
14. `BodyClassificationService` + Impl
15. Update `HealthDataController` thêm endpoint `/constitution`
16. Unit test `BodyClassifier` (test edge cases BMI/PBF threshold)

**Phase 4 — Gateway:**
17. Verify route `/api/user-preferences/**` đã có

**Phase 5 — Integration test:**
18. End-to-end: register → seed FORMULA → submit health data → constitution endpoint
19. Test event sync: update profile/preference → constitution reflect

---

## 13. ENDPOINTS DASHBOARD ĐÃ TỒN TẠI (chỉ verify, không build)

| Endpoint | Mục đích | Note |
|---|---|---|
| `GET /api/health-data/dashboard-metrics` | 7 chỉ số (weight/height/BMI/BMR/TDEE/PBF/WHR) | Cho section "Chi tiết chỉ số" collapsed |
| `GET /api/health-data/latest-metrics` | Latest base metrics | Optional |
| `GET /api/health-data/query/history/{indicatorType}?from=&to=&granularity=DAILY` | History theo time range | Cho Widget 2A weight chart, dùng `indicatorType=WEIGHT` |

**Action cho FE:** Widget 2A dùng `query/history/WEIGHT?from=<30 days ago>&to=<today>&granularity=DAILY`. Không cần endpoint mới.

---

## VERSION HISTORY

| Ngày | Version | Thay đổi |
|---|---|---|
| 25/05/2026 | v1.0 | Spec ban đầu (sai assumption: Feign, schema flat) |
| 25/05/2026 | v3.1 | Thêm cho Onboarding wizard: (1) Section §7 `user_goals` table + 3 endpoint (current/PUT current/history); (2) Section §8 `profile_completed` flag + endpoint mark; (3) §6.4 update ConstitutionResponse thêm `suggestedGoal`; (4) Gateway routing thêm `/api/user-goals/**`. |
| 25/05/2026 | v3.0 | **Rewrite hoàn toàn** sau verify code: (1) Bỏ Feign — dùng RabbitMQ event pattern hiện có; (2) Đơn giản hóa service vì PBF đã có trong `calculated_metric_snapshots`; (3) Bảng mirror `user_preference_mirror` đồng bộ với pattern `UserForHealthData`; (4) Util `BodyClassifier` tách khỏi `HealthCalculator`. Phát hiện và note issue PBF require age trong code hiện tại. |
