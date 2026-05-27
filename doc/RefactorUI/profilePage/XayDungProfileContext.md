# Context xay dung Profile Page

Ngay tao: 2026-05-27

Cap nhat gan nhat: 2026-05-27 - Phase A/B committed, Phase C BE completed in working tree, pending user review/commit.

Tai lieu goc: `doc/RefactorUI/profilePage/HuongDanXayDungProfilePage.md`

Muc dich file nay:
- Lam context ngan gon cho FE khi xay dung trang `/profile`.
- Ghi lai danh gia backend hien tai so voi yeu cau Profile MVP.
- Lam handoff/compact context khi session dai bi rut gon.

## Ket luan nhanh

Backend hien tai CHUA san sang day du cho Profile MVP theo huong dan.

Phase A cua `HuongDanXayDungProfilePage_BE.md` da commit:
- Commit: `9c9bea3 refactor(health-data): standardize profile metrics responses`
- `health-data-service` endpoints da duoc wrap ve `DataResponse<T>`.
- Compile module `health-data-service` pass voi `.\mvnw -pl health-data-service -am test -DskipTests`.

Phase B da commit:
- Commit: `3446d37 feat(user-goal): snapshot current weight on goal change`
- Them `startWeightKg` vao `UserGoal` va `UserGoalResponse`.
- Them `RestTemplateConfig` va `HealthDataClient`.
- `PUT /api/user-goals/current` se snapshot current weight tu `GET /api/health-data/dashboard-metrics`.
- Compile module `user-service` pass voi `.\mvnw -pl user-service -am test -DskipTests`.

Phase C da duoc thuc hien trong working tree, chua commit:
- `GET /api/user/currentUser` tra them `email`, `createdAt`.
- `UserResponseDTO` tra them `email`, `createdAt`.
- `User.createdAt` co `@CreationTimestamp`.
- `PUT /api/user/profile` clear phone duoc khi request co `phone: null` hoac `phoneNumber: null`.
- Neu client omit phone, backend khong clear phone ngoai y muon.
- Compile module `user-service` pass voi `.\mvnw -pl user-service -am test -DskipTests`.
- Can user review/OK truoc khi commit va sang Phase D.

Da co cac API nen tang:
- Lay current user profile: `GET /api/user/currentUser`
- Cap nhat personal info: `PUT /api/user/profile`
- Lay/cap nhat user preferences: `GET/PUT /api/user-preferences`
- Lay current/history goal: `GET /api/user-goals/current`, `GET /api/user-goals/history`
- Lay dashboard metrics/current weight: `GET /api/health-data/dashboard-metrics` (sau Phase A: `DataResponse<DashboardMetricsResponse>`)
- Lay constitution: `GET /api/health-data/constitution`

Con thieu cac phan bat buoc cua Profile MVP:
- Chua co endpoint `PUT /api/auth/change-password`.
- Chua co `ChangePasswordRequest`.
- Chua co `AUTH-011`, `AUTH-012` trong `ErrorCode`.

## Phase execution log

### Phase A - Refactor health-data-service to DataResponse

Trang thai: DA COMMIT.

Commit: `9c9bea3 refactor(health-data): standardize profile metrics responses`

Files updated:
- `health-data-service/src/main/java/org/example/healthdataservice/controller/HealthDataController.java`
- `health-data-service/src/main/java/org/example/healthdataservice/controller/UnitController.java`
- `health-data-service/src/main/java/org/example/healthdataservice/controller/HealthIndicatorConfigsController.java`

Thay doi:
- `GET /api/health-data/latest-metrics` tra `DataResponse<LatestHealthDataResponse>`.
- `GET /api/health-data/dashboard-metrics` tra `DataResponse<DashboardMetricsResponse>`.
- `GET /api/health-data/query/history/{indicatorTypeString}` tra `DataResponse<List<HistoricalDataPointDTO>>`.
- Invalid history indicator khong con tra fake DTO, tra `DataResponse.error(...)`.
- Toan bo `UnitController` endpoints tra `DataResponse<T>`; `DELETE /units/{id}` doi tu 204 sang 200 voi body success.
- `HealthIndicatorConfigsController` endpoints tra `DataResponse<T>`.
- `GET /indicator-configs/{indicatorType}` neu khong tim thay tra HTTP 404 voi `DataResponse.error(...)`.

Verification:
- Da chay `.\mvnw -pl health-data-service -am test -DskipTests`.
- Ket qua: BUILD SUCCESS.
- Chua chay Postman vi can service/runtime va token hop le.

### Phase B - startWeightKg and snapshot current weight

Trang thai: DA COMMIT.

Commit: `3446d37 feat(user-goal): snapshot current weight on goal change`

Files updated/added:
- `user-service/src/main/java/org/example/userservice/entity/UserGoal.java`
- `user-service/src/main/java/org/example/userservice/dto/response/UserGoalResponse.java`
- `user-service/src/main/java/org/example/userservice/service/UserGoalServiceImpl.java`
- `user-service/src/main/java/org/example/userservice/config/RestTemplateConfig.java`
- `user-service/src/main/java/org/example/userservice/service/HealthDataClient.java`
- `user-service/src/main/resources/application.yml`

Thay doi:
- Add nullable column mapping `start_weight_kg` via `UserGoal.startWeightKg`.
- Add `startWeightKg` to `UserGoalResponse`.
- `UserGoalServiceImpl.updateCurrent()` calls `HealthDataClient.fetchCurrentWeightKg(userId)` before creating new active goal.
- If snapshot fails or no weight exists, goal update still succeeds and `startWeightKg = null`.
- `HealthDataClient` calls `GET /api/health-data/dashboard-metrics` and parses Phase A contract: `body.data.weight.value`.
- RestTemplate timeout: 2s connect / 3s read.
- Config added: `app.services.health-data.url`, default `${HEALTH_DATA_SERVICE_URL:http://localhost:8085}`.

Verification:
- Da chay `.\mvnw -pl user-service -am test -DskipTests`.
- Da chay them `.\mvnw -pl user-service,health-data-service -am test -DskipTests`.
- Ket qua: BUILD SUCCESS.
- Chua chay Postman vi can service/runtime va token hop le.

### Phase C - profile email/createdAt and safe phone clear

Trang thai: DA THUC HIEN TRONG WORKING TREE, CHUA COMMIT, DANG CHO USER REVIEW.

Files updated:
- `user-service/src/main/java/org/example/userservice/entity/User.java`
- `user-service/src/main/java/org/example/userservice/dto/request/UserRequestDTO.java`
- `user-service/src/main/java/org/example/userservice/dto/response/UserProfileResponse.java`
- `user-service/src/main/java/org/example/userservice/dto/response/UserResponseDTO.java`
- `user-service/src/main/java/org/example/userservice/controller/UserController.java`
- `user-service/src/main/java/org/example/userservice/mapper/UserMapper.java`
- `user-service/src/main/java/org/example/userservice/service/UserServiceImpl.java`

Thay doi:
- Add `@CreationTimestamp` to `User.createdAt`.
- Add transient `User.phoneProvided` to carry request intent after DTO mapping.
- `UserRequestDTO` tracks whether `phone`/`phoneNumber` exists in request via custom setter.
- `UserServiceImpl.updateUserProfile()` updates phone only when request included phone field. This supports explicit `null` clear without clearing phone on omitted field.
- `UserProfileResponse` and `UserResponseDTO` now include `email`, `createdAt`.
- `UserController.currentUser` maps `user.auth.email` and `user.createdAt`.
- `UserMapper.toDTO()` maps `email`, `createdAt`.

Intentional deviation from BE guide:
- Did not add `@NotBlank/@NotNull` to `UserRequestDTO` in Phase C because no runtime/E2E onboarding verification is available in this session. This keeps existing partial-update behavior safer while still supporting Profile clear-phone.

Verification:
- Da chay `.\mvnw -pl user-service -am test -DskipTests`.
- Da chay them `.\mvnw -pl user-service,health-data-service -am test -DskipTests`.
- Ket qua: BUILD SUCCESS.
- Chua chay Postman vi can service/runtime va token hop le.

## Danh gia backend theo tung muc

### A1 - start_weight_kg cho user_goals

Trang thai: DA LAM TRONG WORKING TREE, CHUA COMMIT.

File da kiem tra:
- `user-service/src/main/java/org/example/userservice/entity/UserGoal.java`
- `user-service/src/main/java/org/example/userservice/dto/response/UserGoalResponse.java`
- `user-service/src/main/java/org/example/userservice/service/UserGoalServiceImpl.java`

Hien tai `UserGoal` chi co:
- `targetWeightKg`
- `targetDurationMonths`
- `note`
- `createdAt`

Hien tai `UserGoalResponse` chi co:
- `id`
- `goalCode`
- `startDate`
- `endDate`
- `isActive`
- `targetWeightKg`
- `targetDurationMonths`
- `note`

Can bo sung:
- Entity field `startWeightKg` map column `start_weight_kg`.
- Response field `startWeightKg`.
- Mapping trong `toResponse()`.
- Set `startWeightKg` khi tao goal moi trong `updateCurrent()`.

FE impact:
- Section Goal progress bar co the dung `startWeightKg` sau khi Phase B duoc commit/deploy.
- FE van phai render fallback neu `startWeightKg == null` cho old rows hoac khi health-data unavailable.

### A2 - Snapshot current weight khi doi goal

Trang thai: DA LAM TRONG WORKING TREE, CHUA COMMIT.

File da kiem tra:
- `user-service/src/main/java/org/example/userservice/service/UserGoalServiceImpl.java`
- `user-service/src/main/resources/application.yml`
- `health-data-service/src/main/java/org/example/healthdataservice/controller/HealthDataController.java`
- `health-data-service/src/main/java/org/example/healthdataservice/dto/response/DashboardMetricsResponse.java`
- `health-data-service/src/main/java/org/example/healthdataservice/dto/response/MetricData.java`

Da ton tai trong working tree:
- `user-service/src/main/java/org/example/userservice/config/RestTemplateConfig.java`
- `user-service/src/main/java/org/example/userservice/service/HealthDataClient.java`
- Config `app.services.health-data.url`

Luu y quan trong:
- Truoc Phase A, `GET /api/health-data/dashboard-metrics` tra raw object `DashboardMetricsResponse`.
- Sau Phase A trong working tree, endpoint nay da tra `DataResponse<DashboardMetricsResponse>`.
- Shape trong `data` sau Phase A:

```json
{
  "weight": {
    "value": 75.0,
    "unit": "kg",
    "lastUpdatedAt": "2026-05-27T08:00:00"
  },
  "height": { "...": "..." },
  "bmi": { "...": "..." },
  "bmr": { "...": "..." },
  "tdee": { "...": "..." },
  "pbf": { "...": "..." },
  "whr": { "...": "..." }
}
```

Neu BE implement `HealthDataClient` theo huong dan goc va di vao `body.get("data")`, se khong lay duoc weight voi backend hien tai.

Khuyen nghi BE cho Phase B:
- Implement `HealthDataClient` theo contract sau Phase A: doc `body.get("data")`, sau do doc `weight.value`.
- Neu Phase A chua duoc commit/merge ma da lam Phase B, client se phai support raw fallback; hien tai flow duoc chon la Phase A truoc Phase B.

FE impact:
- FE lay current weight tu service dashboard/health-data sau unwrap, field can doc la `weight.value`.
- Cac service FE cu dang doc `response.data` truc tiep can doi sang `unwrapDataResponse()` sau Phase A.

### A3 - Change Password endpoint

Trang thai: CHUA LAM.

File da kiem tra:
- `user-service/src/main/java/org/example/userservice/service/AuthService.java`
- `user-service/src/main/java/org/example/userservice/service/AuthServiceImpl.java`
- `user-service/src/main/java/org/example/userservice/controller/AuthController.java`
- `common/src/main/java/org/example/web/exception/ErrorCode.java`
- `user-service/src/main/java/org/example/userservice/config/SecurityConfig.java`
- `api-gateway/src/main/java/org/example/apigateway/security/JwtFilter.java`

Hien tai `AuthController` chi co:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh-token`

Chua co:
- `PUT /api/auth/change-password`
- `ChangePasswordRequest`
- `AuthService.changePassword(...)`
- `AuthServiceImpl.changePassword(...)`
- `ErrorCode.CHANGE_PASSWORD_WRONG_CURRENT` / `AUTH-011`
- `ErrorCode.CHANGE_PASSWORD_SAME` / `AUTH-012`

San sang san co:
- `SecurityConfig` da co `BCryptPasswordEncoder`.
- `AuthServiceImpl` dang dung `passwordEncoder.encode()` khi register.
- `AuthRepository.findByUsername()` da co.
- `User` entity da co relation `@OneToOne Auth auth`.
- `UserRepository.findByAuth_Id(...)` va `findByAuth_Username(...)` da co.

Gateway:
- Route `/api/auth/**` da qua `JwtFilter`.
- `JwtFilter.isPublicEndpoint()` chi bypass login/register/refresh/public/actuator/docs.
- Neu them `/api/auth/change-password`, endpoint nay mac dinh se require JWT va gateway inject header `userId`, `username`, `userRoles`.
- Khong can sua gateway cho change-password neu giu path nay.

FE impact:
- Chua the implement Section Security voi API that.
- Co the build UI/form validation truoc, nhung submit that se fail 404/405 cho den khi BE them endpoint.
- Khi BE xong, FE service nen goi:

```ts
apiClient.put<DataResponse<void>>('/api/auth/change-password', {
  currentPassword,
  newPassword,
});
```

Expected BE errors sau khi implement:
- Sai current password: HTTP 400, `code = "AUTH-011"`.
- New password trung old password: HTTP 400, `code = "AUTH-012"`.
- Validation error: HTTP 400, `code = "COMMON-001"`, message gom field error.

### A4 - Goal history endpoint

Trang thai: DA CO.

File da kiem tra:
- `user-service/src/main/java/org/example/userservice/controller/UserGoalController.java`
- `user-service/src/main/java/org/example/userservice/service/UserGoalServiceImpl.java`
- `user-service/src/main/java/org/example/userservice/repository/UserGoalRepository.java`

Endpoints:

```http
GET /api/user-goals/current
GET /api/user-goals/history
PUT /api/user-goals/current
```

Tat ca dung header:

```http
userId: <injected-by-gateway>
```

Response wrapper:

```json
{
  "code": null,
  "message": "Success",
  "data": {}
}
```

Neu chua co active goal:
- `GET /api/user-goals/current` tra BusinessException `GOAL-001`.

FE impact:
- `getGoalHistory()` co the implement ngay.
- Current goal call can handle `GOAL-001` thanh empty state.
- Response chua co `startWeightKg` cho den khi BE lam A1/A2.

## Contract backend hien co cho FE

### Current user profile

```http
GET /api/user/currentUser
Authorization: Bearer <jwt>
```

Gateway inject:
- `userId`
- `username`
- `userRoles`

Response data:

```ts
interface CurrentUserProfile {
  userId: string;
  username: string;
  roles: string[];
  name: string | null;
  phone: string | null;
  birthDate: string | null; // yyyy-MM-dd
  gender: 'MALE' | 'FEMALE' | null;
  profileCompleted: boolean;
}
```

Sau Phase C trong working tree, response co them:
- `email`
- `createdAt`

### Account details

```http
GET /api/user/account-details
Authorization: Bearer <jwt>
```

Response data co email:

```ts
interface UserAccountDetails {
  userId: string;
  userName: string;
  email: string;
  roles: string[];
  name: string | null;
  phoneNumber: string | null;
  birthDate: string | null;
  gender: 'MALE' | 'FEMALE' | null;
}
```

FE co the dung endpoint nay de lay email cho Profile Header.

Gap:
- Endpoint nay van chua expose `createdAt`; Profile FE co the uu tien `GET /api/user/currentUser` sau Phase C.

### Update personal profile

```http
PUT /api/user/profile
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "name": "Nguyen Van A",
  "phone": "0900000000",
  "birthDate": "1995-01-15",
  "gender": "MALE"
}
```

`UserRequestDTO` accept:
- `name`
- `phone` hoac `phoneNumber` vi co `@JsonAlias("phone")` va `@JsonProperty("phoneNumber")`
- `birthDate`
- `gender`

Response data:

```ts
interface UserResponseDTO {
  id: string;
  name: string | null;
  phone: string | null;
  birthDate: string | null;
  gender: 'MALE' | 'FEMALE' | null;
  profileCompleted: boolean;
}
```

Sau Phase C trong working tree:
- Neu request co field `phone` hoac `phoneNumber`, backend se update phone theo value do.
- Neu value la `null`, phone duoc clear.
- Neu request omit phone, backend giu phone cu.

### User preferences / PBF method

```http
GET /api/user-preferences
PUT /api/user-preferences/{prefKey}
```

Key hien duoc support:
- `pbf_method`

Allowed values:
- `FORMULA`
- `MODEL_1`

Payload:

```json
{
  "prefValue": "FORMULA",
  "valueType": "STRING"
}
```

Response data:

```ts
interface PreferenceResponse {
  prefKey: string;
  prefValue: string;
  valueType: string | null;
  description: string | null;
}
```

Trang thai: san sang cho Section Health Settings.

### Health dashboard metrics

```http
GET /api/health-data/dashboard-metrics
Authorization: Bearer <jwt>
```

Sau Phase A, response co boc `DataResponse`.

```ts
interface MetricData {
  value: number;
  unit: string | null;
  lastUpdatedAt: string | null;
}

interface DashboardMetricsResponse {
  weight: MetricData | null;
  height: MetricData | null;
  bmi: MetricData | null;
  bmr: MetricData | null;
  tdee: MetricData | null;
  pbf: MetricData | null;
  whr: MetricData | null;
}
```

FE current weight cho Section Goal:

```ts
const currentWeight = dashboardMetrics.weight?.value ?? null;
```

### Constitution

```http
GET /api/health-data/constitution
Authorization: Bearer <jwt>
```

Response co boc `DataResponse`.

```ts
interface ConstitutionResponse {
  constitution: string;
  method: string;
  bmi: number | null;
  pbf: number | null;
  pbfSource: string | null;
  bmiClass: number | null;
  pbfClass: number | null;
  finalClass: number | null;
  suggestedGoal: string | null;
  warning: string | null;
  computedAt: string | null;
}
```

### User goals

```http
GET /api/user-goals/current
GET /api/user-goals/history
PUT /api/user-goals/current
```

Current response data hien tai:

```ts
interface UserGoalResponseCurrentBackend {
  id: string;
  goalCode: string;
  startDate: string;
  endDate: string | null;
  isActive: boolean;
  targetWeightKg: number | null;
  startWeightKg: number | null;
  targetDurationMonths: number | null;
  note: string | null;
}
```

Truoc Phase B response chua co `startWeightKg`; sau Phase B trong working tree field nay da co.

`PUT /api/user-goals/current` payload:

```ts
interface UpdateGoalRequest {
  goalCode: 'GIAM' | 'DUY_TRI' | 'TANG';
  targetWeightKg?: number | null;
  targetDurationMonths?: number | null;
  note?: string | null;
}
```

Can confirm enum exact trong FE neu co mapping rieng. Backend enum file la `user-service/src/main/java/org/example/userservice/enums/GoalCode.java`.

### Delete account

Backend co endpoint:

```http
DELETE /api/user/{id}
```

Nhung theo scope Profile MVP, Danger Zone DEFER.

FE KHONG goi endpoint delete account trong MVP. Button chi toast:

```text
Tinh nang dang phat trien. Vui long lien he admin de xoa tai khoan.
```

## Response wrapper va error handling

Common success wrapper:

```ts
interface DataResponse<T> {
  code: string | null;
  message: string;
  data: T;
}
```

Success:

```json
{
  "code": null,
  "message": "Success",
  "data": {}
}
```

Business/validation error:

```json
{
  "code": "COMMON-001",
  "message": "Du lieu khong hop le: field: reason",
  "data": null
}
```

Ngoai le dang chu y:
- Sau Phase A, `GET /api/health-data/dashboard-metrics`, `latest-metrics`, `query/history`, `units`, va `indicator-configs` da duoc wrap `DataResponse<T>` trong working tree.
- `POST /api/health-data/submit` va `GET /api/health-data/constitution` da wrap tu truoc.

## Backend blockers truoc khi FE integration full

Uu tien 1 - bat buoc cho Security section:
- Them `ChangePasswordRequest`.
- Them `AuthService.changePassword`.
- Implement trong `AuthServiceImpl` bang `passwordEncoder.matches()` va `encode()`.
- Them `PUT /api/auth/change-password`.
- Them `AUTH-011`, `AUTH-012`.
- Test wrong current password, same password, min length.

Uu tien 2 - bat buoc cho Goal progress:
- DONE trong working tree, pending review/commit.
- Postman can verify `PUT /api/user-goals/current` response co `data.startWeightKg`.
- Postman can verify health-data down fallback van 200 va `startWeightKg = null`.

Uu tien 3 - can cho Profile Header dung design:
- DONE trong working tree, pending review/commit.
- Postman can verify `GET /api/user/currentUser` has `data.email` and `data.createdAt`.
- Existing old users may need DB backfill if `created_at` is null.

Uu tien 4 - polish personal info:
- DONE trong working tree, pending review/commit.
- Backend accepts both `phone` and `phoneNumber`; explicit null clears phone.

## Khuyen nghi FE trong luc BE chua hoan tat

1. Build UI theo huong dan, nhung dat capability flags/no-op fallback cho cac API chua co.
2. Section Header:
   - Lay `name`, `birthDate`, `gender`, `phone`, `profileCompleted` tu `useAuth().user` hoac `GET /api/user/currentUser`.
   - Lay `email` tu `GET /api/user/account-details` neu AuthContext khong co email.
   - Joined date: an field, hien fallback trung tinh, hoac doi BE expose `createdAt`. Khong hardcode sample date.
3. Section Personal Info:
   - Dung `PUT /api/user/profile`.
   - Sau save goi `refreshUser()`.
   - Tam thoi khong ky vong clear phone ve null neu BE chua sua.
4. Section Goal:
   - Dung `GET /api/user-goals/current`, `GET /api/user-goals/history`, `PUT /api/user-goals/current`.
   - Handle `GOAL-001` thanh empty state.
   - Progress bar chi render khi co du `startWeightKg`, `targetWeightKg`, `currentWeight`.
5. Section Health Settings:
   - Co the integrate ngay voi `GET/PUT /api/user-preferences/pbf_method`.
6. Section Security:
   - Build form validation truoc.
   - Disable submit hoac hien toast "Tinh nang dang phat trien" cho den khi BE co `PUT /api/auth/change-password`.
7. Section Danger Zone:
   - Khong goi DELETE account endpoint trong MVP.

## Checklist backend hien tai

- [x] Gateway inject `userId` header.
- [x] Gateway se require JWT cho `/api/auth/change-password` neu endpoint duoc them.
- [x] `BCryptPasswordEncoder` da configured.
- [x] User/Auth relation da co.
- [x] Goal history endpoint da co.
- [x] PBF preference endpoint da co va validate `FORMULA`/`MODEL_1`.
- [x] Constitution endpoint da co.
- [x] Dashboard metrics endpoint da co current weight.
- [x] `startWeightKg` migration/field/response da co trong working tree.
- [x] Snapshot current weight khi doi goal da co trong working tree.
- [x] Cross-service RestTemplate client da co trong working tree.
- [ ] Change password endpoint chua co.
- [ ] Error codes `AUTH-011`, `AUTH-012` chua co.
- [x] `createdAt`/joined date da expose trong `currentUser` trong working tree.
- [x] Clear phone ve null da duoc backend support trong working tree.

## Compact summary

Profile BE status ngay 2026-05-27: Phase A da commit `9c9bea3`; Phase B da commit `3446d37`; Phase C da xong trong working tree, chua commit. Health-data endpoints lien quan da wrap `DataResponse<T>`. User goals co `startWeightKg`, snapshot client doc `body.data.weight.value`. Current user profile co `email`, `createdAt`; update profile clear phone duoc khi request explicit null. Chua co change-password. Gateway route `/api/auth/**` qua JwtFilter va chi bypass login/register/refresh, vi vay future `PUT /api/auth/change-password` se require JWT dung nhu mong doi. FE co the build Personal Info, Health Settings, Goal history UI ngay; Security submit can BE Phase D.
