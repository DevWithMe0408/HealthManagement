# Refactor UI Backend Implementation Context

Last updated: 2026-05-25

Branch: `feature/onboarding-dashboard-be`

Purpose: keep the implemented backend contract for Onboarding/Dashboard in one short file so future Codex sessions and the FE project can continue without re-reading every spec.

## Source Specs

- `doc/RefactorUI/backend_endpoint_spec_v3.md`
- `doc/RefactorUI/dashboard_spec_v3.md`
- `D:\OneDrive\fifthYear_5\20252\DoAn\UI\Onboarding\onboarding_spec.md`

## Confirmed Decisions

- `POST /api/health-data/submit` should support `baseMetrics[]` as in the Onboarding spec.
- Add self-service endpoint `PUT /api/user/profile`; FE should not need to pass user id in URL.
- Keep the current `ErrorCode` enum pattern and add new enum values instead of ad-hoc string error codes.
- Finish and test BE APIs first, then hand off contract/context to the FE project.

## Step 1 Commit

Commit: `bbe71d9 feat: align onboarding backend contracts`

Implemented:

- `POST /api/health-data/submit` now returns `DataResponse<Void>`.
- `SubmitHealthDataRequest` supports:

```json
{
  "baseMetrics": [
    { "type": "HEIGHT", "value": 168 },
    { "type": "WEIGHT", "value": 58.5 },
    { "type": "ACTIVITY_FACTOR", "value": 1.375 }
  ]
}
```

- Old flat fields are still accepted temporarily:

```json
{
  "height": 168,
  "weight": 58.5,
  "activityFactor": 1.375
}
```

- Added `PUT /api/user/profile`.

Request:

```json
{
  "name": "Nguyen Van A",
  "birthDate": "2002-03-15",
  "gender": "MALE",
  "phone": "0912345678"
}
```

Response wraps `UserResponseDTO` in `DataResponse`.

- `UserRequestDTO` accepts both `phone` and legacy `phoneNumber`.
- Added `ErrorCode` values:
  - `PREFERENCE_INVALID_KEY`
  - `PREFERENCE_INVALID_VALUE`
  - `PREFERENCE_NOT_FOUND`
  - `GOAL_NO_ACTIVE`
  - `GOAL_INVALID`
  - `HEALTH_MISSING_BASIC_DATA`
  - `HEALTH_MISSING_GENDER`
  - `HEALTH_INVALID_METRIC`

Verification:

```powershell
.\mvnw.cmd -pl common,user-service,health-data-service -am test
```

Result: `BUILD SUCCESS`.

## Step 3 Commit

Commit message: `feat: add user goal endpoints`

Implemented:

- Add `GoalCode` enum in `user-service`: `GIAM`, `DUY_TRI`, `TANG`.
- Add `user_goals` entity/table model.
- Add active-goal history behavior:
  - Updating current goal deactivates any previous active goal.
  - New active goal starts today.
  - `targetDurationMonths` defaults to `6` if omitted.
- Add gateway route `/api/user-goals/**` to `user-service`.

Endpoints:

### `GET /api/user-goals/current`

- Reads current user from `userId` gateway header.
- Returns `404` with `GOAL-001` if no active goal exists.

Success response:

```json
{
  "code": null,
  "message": "Success",
  "data": {
    "id": "019...",
    "goalCode": "GIAM",
    "startDate": "2026-05-25",
    "endDate": null,
    "isActive": true,
    "targetWeightKg": 58.5,
    "targetDurationMonths": 6,
    "note": "Onboarding goal"
  }
}
```

### `PUT /api/user-goals/current`

Request:

```json
{
  "goalCode": "GIAM",
  "targetWeightKg": 58.5,
  "targetDurationMonths": 6,
  "note": "Onboarding goal"
}
```

Notes:

- `goalCode` is required.
- `targetDurationMonths` must be `1..24`; defaults to `6` if `null`.
- `targetWeightKg` and `note` are optional.

### `GET /api/user-goals/history`

Returns all goals for current user ordered by `startDate desc`.

Verification:

```powershell
.\mvnw.cmd -pl common,user-service,api-gateway -am test
```

Result: `BUILD SUCCESS`.

## Step 4 Commit

Commit message: `feat: add user preference endpoints`

Implemented:

- Add common event `UserPreferencesUpdatedEvent`.
- Add `user_preferences` entity/table model in `user-service`.
- Add default preference seed on registration:
  - `prefKey`: `pbf_method`
  - `prefValue`: `FORMULA`
  - `valueType`: `STRING`
- Add gateway route `/api/user-preferences/**` to `user-service`.
- Add RabbitMQ routing key in `user-service`:
  - `app.rabbitmq.routing-key.user-preferences-updated=user.preferences.updated`

Important current state:

- `user-service` now publishes `UserPreferencesUpdatedEvent` on preference update/delete.
- `health-data-service` does not consume this event yet. The mirror table/listener will be implemented in the next backend step.
- The seed during registration writes the default row in `user_db`; it does not publish the preference event yet. `health-data-service` can default to `FORMULA` until the user changes the preference.

Allowed preference keys:

- `pbf_method`

Allowed `pbf_method` values:

- `FORMULA`
- `MODEL_1`

Endpoints:

### `GET /api/user-preferences`

Returns all preferences for current user.

Success response:

```json
{
  "code": null,
  "message": "Success",
  "data": [
    {
      "prefKey": "pbf_method",
      "prefValue": "FORMULA",
      "valueType": "STRING",
      "description": "Method tinh PBF: FORMULA (Navy) hoac MODEL_1 (ML)"
    }
  ]
}
```

### `GET /api/user-preferences/{prefKey}`

Returns one preference or `404` with `PREF-003` if not found.

### `PUT /api/user-preferences/{prefKey}`

Request:

```json
{
  "prefValue": "MODEL_1",
  "valueType": "STRING"
}
```

Notes:

- `prefValue` is required.
- `valueType` is optional and defaults to `STRING`.
- Invalid key returns `400` with `PREF-001`.
- Invalid value returns `400` with `PREF-002`.
- Publishes `UserPreferencesUpdatedEvent`.

### `DELETE /api/user-preferences/{prefKey}`

- Deletes the preference row for current user.
- Publishes `UserPreferencesUpdatedEvent` with `prefValue=null`.

Verification:

```powershell
.\mvnw.cmd -pl common,user-service,api-gateway -am test
```

Result: `BUILD SUCCESS`.

## Step 5 Commit

Commit message: `feat: mirror user preferences in health data`

Implemented:

- Add `user_preference_mirror` entity/table model in `health-data-service`.
- Add `UserPreferenceMirrorService`:
  - `saveOrUpdate(userId, prefKey, prefValue)`
  - `getValue(userId, prefKey)`
  - `getValueOrDefault(userId, prefKey, defaultValue)`
- Add RabbitMQ queue and binding in `health-data-service`:
  - queue: `health-data.user-preferences-updated.queue`
  - routing key: `user.preferences.updated`
- Add listener for `UserPreferencesUpdatedEvent`.

Event behavior:

- When `prefValue` is non-null, health-data-service upserts the mirror row.
- When `prefValue` is `null`, health-data-service deletes the mirror row.
- Future `GET /api/health-data/constitution` should read:

```java
userPreferenceMirrorService.getValueOrDefault(userId, "pbf_method", "FORMULA")
```

Known limitation:

- Register seed in `user-service` writes `pbf_method=FORMULA`, but does not publish a preference event.
- This is acceptable for now because health-data-service should default to `FORMULA` when no mirror row exists.

Verification:

```powershell
.\mvnw.cmd -pl common,health-data-service -am test
```

Result: `BUILD SUCCESS`.

## Step 2 Commit

Commit message: `feat: add onboarding profile completion contract`

Implemented:

- Add `profile_completed` column to `User`.
- New users default `profileCompleted=false`.
- Add `PUT /api/user/profile-completed`.
- Expand `GET /api/user/currentUser` to include profile fields needed by Onboarding/Dashboard:
  - `userId`
  - `username`
  - `roles`
  - `name`
  - `phone`
  - `birthDate`
  - `gender`
  - `profileCompleted`

`GET /api/user/currentUser` response shape:

```json
{
  "code": null,
  "message": "Success",
  "data": {
    "userId": "019...",
    "username": "demo",
    "roles": ["ROLE_USER"],
    "name": "Nguyen Van A",
    "phone": "0912345678",
    "birthDate": "2002-03-15",
    "gender": "MALE",
    "profileCompleted": false
  }
}
```

`PUT /api/user/profile-completed`:

- Request body: none.
- Reads current user from `userId` gateway header.
- Sets `profileCompleted=true`.
- Response:

```json
{
  "code": null,
  "message": "Success",
  "data": null
}
```

Verification:

```powershell
.\mvnw.cmd -pl common,user-service -am test
```

Result: `BUILD SUCCESS`.

## Step 6 Commit

Commit message: `feat: add health constitution endpoint`

Implemented:

- Add `BodyClassifier` util in `health-data-service`.
- Add `ConstitutionResponse` DTO.
- Add `BodyClassificationService` / `BodyClassificationServiceImpl`.
- Add endpoint `GET /api/health-data/constitution`.
- Add unit test `BodyClassifierTest` for BMI/PBF thresholds and final class fallback.

Endpoint:

### `GET /api/health-data/constitution`

- Reads current user from `userId` gateway header.
- Requires synced user profile gender in `user_for_health_data`.
- Requires latest BMI snapshot from `calculated_metric_snapshots`.
- Reads latest PBF snapshot if available.
- Reads `pbf_method` from `user_preference_mirror`; defaults to `FORMULA` if mirror row is absent.
- Classification method is currently `RULE_BMI_PBF`; `MODEL_1` / `MODEL_2` ML wiring is not implemented yet.
- `suggestedGoal` mapping:
  - `GAY` -> `TANG`
  - `CAN_DOI` -> `DUY_TRI`
  - `THUA_CAN` / `BEO_PHI` -> `GIAM`

Success response:

```json
{
  "code": null,
  "message": "Success",
  "data": {
    "constitution": "CAN_DOI",
    "method": "RULE_BMI_PBF",
    "bmi": 22.1,
    "pbf": 15.4,
    "pbfSource": "FORMULA",
    "bmiClass": 1,
    "pbfClass": 1,
    "finalClass": 1,
    "suggestedGoal": "DUY_TRI",
    "warning": null,
    "computedAt": "2026-05-25T23:19:00"
  }
}
```

Incomplete PBF behavior:

- If BMI exists but PBF is missing, endpoint still returns `200`.
- `pbf`, `pbfClass` are `null`.
- `finalClass` falls back to `bmiClass`.
- `warning` is set:
  - Female: `Thieu vong eo, co, hoac hong - chi phan loai theo BMI`
  - Male/other: `Thieu vong eo hoac co - chi phan loai theo BMI`

Error behavior:

- Missing synced gender/profile returns `422` with `HEALTH-002`.
- Missing latest BMI snapshot returns `422` with `HEALTH-001`.

Verification:

```powershell
.\mvnw.cmd -pl common,health-data-service -am test
```

Result: `BUILD SUCCESS`.

Resolved in Step 7:

- `HealthCalculator.calculatePBF()` no longer requires age.

## Step 7 Commit

Commit message: `fix: allow pbf calculation without age`

Implemented:

- Updated `HealthCalculator.calculatePBF(...)` so `ageYears` is no longer required.
- Kept the existing method signature for compatibility with current callers.
- Removed misleading comments in the PBF recalculation path that said Navy PBF depends on age.
- Added `HealthCalculatorTest`.

Behavior after this step:

- Male PBF requires: `gender`, `waist`, `neck`, `height`.
- Female PBF requires: `gender`, `waist`, `hip`, `neck`, `height`.
- `birthDate` / age is still needed for BMR/TDEE, but no longer blocks PBF.
- This improves `GET /api/health-data/constitution` completeness because PBF snapshots can now be generated even when profile has gender but missing birth date.

Verification:

```powershell
.\mvnw.cmd -pl common,health-data-service -am test
```

Result: `BUILD SUCCESS`.
