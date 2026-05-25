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
