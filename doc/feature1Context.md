# Feature 1 Context - View Tomorrow Menu

## Backend contract

- Endpoint: `POST /api/recommendation/full-day`.
- Request now accepts optional field `planDay`.
- Valid values:
  - `TODAY`
  - `TOMORROW`
- Missing `planDay` is treated like `TODAY`.
- Response shape is unchanged.

## TODAY behavior

`TODAY` keeps the previous behavior:

- The response `planDate` is today, except the existing 21:00-05:00 rule still rolls over to tomorrow.
- Meals are filtered by current time.
- Meals already logged for today are excluded.

## TOMORROW behavior

`TOMORROW` is for viewing a full menu for the next calendar day:

- `planDate` is tomorrow.
- The response returns all meals configured by `planType`.
- Time-of-day filtering is skipped.
- Logged-meal filtering is skipped for tomorrow.
- Recommendation scoring and response DTOs are not changed.

## Frontend usage

Use the same request body as the current full-day recommendation flow and add:

```json
{
  "planDay": "TOMORROW"
}
```

For the current-day tab or existing screen, omit `planDay` or send:

```json
{
  "planDay": "TODAY"
}
```

## Example

```http
POST /api/recommendation/full-day
Content-Type: application/json
X-User-Id: <user-id>
```

```json
{
  "tdee": 2100,
  "goalCode": "GIAM",
  "planType": "3_BUA",
  "constitution": "CAN_DOI",
  "constitutionConfirmed": true,
  "perMealConfig": {
    "SANG": {
      "mealKind": "COMBO"
    },
    "TRUA": {
      "mealKind": "NHIEU_MON",
      "nMain": 1,
      "nRau": 1,
      "nCarb": 1
    },
    "TOI": {
      "mealKind": "NHIEU_MON",
      "nMain": 1,
      "nRau": 1,
      "nCarb": 1
    }
  },
  "planDay": "TOMORROW"
}
```

Expected frontend-visible differences:

- `data.planDate` is tomorrow.
- `data.meals` contains the full set of meals for the selected `planType`.
- No other response fields need new handling.

## Validation notes

- `planDay` is case-sensitive at request validation time. Send uppercase values only.
- Invalid values return the existing validation error response.
