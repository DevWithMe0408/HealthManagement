# API CONTRACT — ADMIN PANEL

> Đặc tả endpoints, request/response cho admin panel của HealthCare app.
> Tài liệu này dành cho cả frontend (build service calls) và backend (implement controller).
> Phiên bản: v1.0
> Ngày: 30/04/2026

---

## 0. NGUYÊN TẮC CHUNG

### 0.0. ⚠️ SCOPE THỰC TẾ CHO GIAI ĐOẠN HIỆN TẠI (v1.2)

Tài liệu này mô tả 33 endpoints đầy đủ, nhưng **scope thực tế của giai đoạn hiện tại CHỈ implement những endpoints sau**:

**LÀM TRONG GIAI ĐOẠN NÀY (~16 endpoints):**
- Section 2: Config endpoints (toàn bộ ~11 endpoints — goals, meals, penalties, scoring, system)
- Section 5: User management endpoints (2 endpoints — list users + detail user)

**DEFERRED (làm sau khi implement xong thuật toán đề xuất thực đơn):**
- Section 1: Dashboard endpoints (3 endpoints) — cần aggregate data từ dishes
- Section 3: Dish endpoints (10 endpoints) — schema dishes cũ chưa khớp spec v3
- Section 4: Ingredient endpoints (8 endpoints) — tương tự

**Lý do defer:** Schema bảng `dishes`, `ingredients` cũ trong `nutrition_db` chưa khớp spec v3 (thiếu `slot_code`, dùng `total_calories` thay vì `kcal_per_100g`). Sẽ refactor schema khi implement thuật toán đề xuất, sau đó mới implement endpoints liên quan.

### 0.1. Base URL

Mọi request đi qua API Gateway: `http://localhost:8080`

### 0.2. Authentication

Tất cả endpoint admin yêu cầu Bearer JWT với role `ROLE_ADMIN`. Header:

```
Authorization: Bearer <accessToken>
```

Gateway verify JWT, inject 3 header (`userId`, `username`, `userRoles`) xuống downstream service. Downstream service dùng `@PreAuthorize("hasRole('ADMIN')")` để enforce.

### 0.3. Response format

Mọi response (success và error) tuân theo `DataResponse<T>`:

```typescript
interface DataResponse<T> {
  code: string | null;   // null khi success, error code (ví dụ "AUTH-003") khi error
  message: string;       // "Success" hoặc thông báo lỗi
  data: T | null;        // payload thực tế, null khi error hoặc khi không có data
}
```

**Ví dụ success:**
```json
{
  "code": null,
  "message": "Success",
  "data": { ... }
}
```

**Ví dụ error:**
```json
{
  "code": "VALIDATION-001",
  "message": "Tổng macro ratio phải bằng 1.00",
  "data": null
}
```

### 0.4. HTTP Status Codes

| Status | Khi nào |
|---|---|
| 200 OK | Request thành công, có data trả về |
| 201 Created | Tạo mới resource thành công |
| 204 No Content | Request thành công nhưng không có data (ví dụ DELETE) |
| 400 Bad Request | Validation fail (request body sai format, sai constraint) |
| 401 Unauthorized | Token không hợp lệ hoặc hết hạn |
| 403 Forbidden | Token hợp lệ nhưng không đủ quyền (không phải ADMIN) |
| 404 Not Found | Resource không tồn tại |
| 409 Conflict | Vi phạm unique constraint (tên trùng, etc.) |
| 500 Internal Server Error | Lỗi server không lường trước |

### 0.5. Pagination convention

Endpoint trả về danh sách dùng pagination chuẩn Spring Data:

**Query params:**
```
?page=0&size=10&sort=name,asc
```

**Response data:**
```json
{
  "content": [...],
  "totalElements": 87,
  "totalPages": 9,
  "number": 0,
  "size": 10,
  "first": true,
  "last": false,
  "empty": false
}
```

**Note cho frontend:** `number` là 0-indexed (page 1 = number 0). Khi hiển thị "Trang 1/9" cho user, dùng `number + 1`.

### 0.6. Error codes mới cần thêm

Dưới đây là các error code mới cần thêm vào `ErrorCode.java` cho admin module:

```java
// ===== CONFIG module =====
CONFIG_NOT_FOUND          ("CONFIG-001", NOT_FOUND,    "Config không tồn tại"),
CONFIG_VALIDATION_FAILED  ("CONFIG-002", BAD_REQUEST,  "Cấu hình không hợp lệ"),
CONFIG_SUM_INVALID        ("CONFIG-003", BAD_REQUEST,  "Tổng giá trị phải bằng giá trị mong đợi"),

// ===== DISH module =====
DISH_NOT_FOUND            ("DISH-001",   NOT_FOUND,    "Món ăn không tồn tại"),
DISH_NAME_TAKEN           ("DISH-002",   CONFLICT,     "Tên món ăn đã tồn tại"),
DISH_IN_USE               ("DISH-003",   CONFLICT,     "Món ăn đang được sử dụng, không thể xóa"),
INVALID_IMAGE_FORMAT      ("DISH-004",   BAD_REQUEST,  "Ảnh không đúng định dạng (jpg/png/webp)"),
IMAGE_TOO_LARGE           ("DISH-005",   BAD_REQUEST,  "Ảnh vượt quá 2MB"),

// ===== INGREDIENT module =====
INGREDIENT_NOT_FOUND      ("INGR-001",   NOT_FOUND,    "Nguyên liệu không tồn tại"),
INGREDIENT_NAME_TAKEN     ("INGR-002",   CONFLICT,     "Tên nguyên liệu đã tồn tại"),

// ===== ADMIN module =====
ADMIN_PERMISSION_DENIED   ("ADMIN-001",  FORBIDDEN,    "Không có quyền thực hiện thao tác"),
```

### 0.7. Service phụ trách

| Endpoint prefix | Service | Port |
|---|---|---|
| `/api/auth/**` | user-service | 8081 |
| `/api/user/**` | user-service | 8081 |
| `/api/admin/users/**` | user-service | 8081 |
| `/api/health-data/**` | health-data-service | 8085 |
| `/api/admin/configs/**` | nutrition-service | 8083 |
| `/api/admin/dishes/**` | nutrition-service | 8083 |
| `/api/admin/ingredients/**` | nutrition-service | 8083 |
| `/api/admin/dashboard/**` | nutrition-service (aggregator) | 8083 |
| `/api/files/**` | nutrition-service | 8083 |

Endpoint dashboard tổng hợp data từ nhiều service. Để đơn giản, đặt trong nutrition-service và gọi user-service / health-data-service qua REST/Feign khi cần.

---

## 1. DASHBOARD ENDPOINTS

> ⚠️ **DEFERRED — KHÔNG implement trong giai đoạn hiện tại.**
> Dashboard cần aggregate data từ dishes/ingredients/users. Vì dishes/ingredients chưa refactor, defer.

### 1.1. GET `/api/admin/dashboard/stats`

Lấy 4 stat card cho trang Dashboard.

**Auth:** ADMIN

**Request:** không có body, không có query params

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": {
    "totalUsers": 152,
    "newUsersToday": 12,
    "totalDishes": 87,
    "dishesBelowThreshold": true,
    "totalMealsRecommended": 1423,
    "newMealsToday": 45,
    "totalGoalConfigs": 3
  }
}
```

**Field detail:**

| Field | Type | Mô tả |
|---|---|---|
| totalUsers | int | Tổng số user (cả admin + user thường) |
| newUsersToday | int | User register hôm nay |
| totalDishes | int | Tổng số dish trong DB |
| dishesBelowThreshold | bool | true nếu totalDishes < 100 (config threshold) |
| totalMealsRecommended | int | Tổng số bữa đã đề xuất từ trước đến giờ |
| newMealsToday | int | Bữa đề xuất hôm nay |
| totalGoalConfigs | int | Số mục tiêu trong goal_config (luôn = 3) |

### 1.2. GET `/api/admin/dashboard/user-growth`

Lấy data biểu đồ tăng trưởng user.

**Auth:** ADMIN

**Query params:**
- `days` (int, optional, default = 7): số ngày nhìn lại

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": [
    { "date": "2026-04-24", "count": 5 },
    { "date": "2026-04-25", "count": 8 },
    { "date": "2026-04-26", "count": 12 },
    { "date": "2026-04-27", "count": 15 },
    { "date": "2026-04-28", "count": 18 },
    { "date": "2026-04-29", "count": 14 },
    { "date": "2026-04-30", "count": 12 }
  ]
}
```

### 1.3. GET `/api/admin/dashboard/warnings`

Lấy danh sách cảnh báo cho admin.

**Auth:** ADMIN

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": [
    {
      "type": "DISHES_LOW",
      "severity": "warning",
      "message": "CSDL món ăn: chỉ có 87 món, dưới ngưỡng 100",
      "linkTo": "/admin/dishes"
    },
    {
      "type": "USERS_NO_PROFILE",
      "severity": "info",
      "message": "5 người dùng chưa cập nhật profile (BMR/TDEE = null)",
      "linkTo": "/admin/users?filter=no-profile"
    }
  ]
}
```

**Type values:** `DISHES_LOW`, `USERS_NO_PROFILE`, `CONFIG_ABNORMAL`, ... (extend khi cần)
**Severity values:** `info`, `warning`, `error`

---

## 2. CONFIG ENDPOINTS

### 2.1. Goal Config

#### 2.1.1. GET `/api/admin/configs/goals`

Lấy 3 config cho 3 mục tiêu.

**Auth:** ADMIN

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": [
    {
      "goalCode": "GIAM",
      "calMultiplier": 0.80,
      "proteinRatio": 0.35,
      "fatRatio": 0.30,
      "carbRatio": 0.35,
      "slotMainRatio": 0.55,
      "slotVegRatio": 0.15,
      "slotCarbRatio": 0.30,
      "weightP": 0.45,
      "weightF": 0.20,
      "weightC": 0.25,
      "weightKcal": 0.10,
      "description": "Giảm cân",
      "updatedAt": "2026-04-15T14:30:00",
      "updatedBy": "admin"
    },
    {
      "goalCode": "DUY_TRI",
      ...
    },
    {
      "goalCode": "TANG",
      ...
    }
  ]
}
```

#### 2.1.2. GET `/api/admin/configs/goals/{goalCode}`

Lấy config cho 1 mục tiêu.

**Auth:** ADMIN

**Path params:**
- `goalCode` (string): `GIAM`, `DUY_TRI`, `TANG`

**Response 200:** giống 1 element của 2.1.1.

**Response 404:**
```json
{
  "code": "CONFIG-001",
  "message": "Config không tồn tại",
  "data": null
}
```

#### 2.1.3. PUT `/api/admin/configs/goals/{goalCode}`

Cập nhật config cho 1 mục tiêu.

**Auth:** ADMIN

**Path params:**
- `goalCode` (string)

**Request body:**
```json
{
  "calMultiplier": 0.80,
  "proteinRatio": 0.35,
  "fatRatio": 0.30,
  "carbRatio": 0.35,
  "slotMainRatio": 0.55,
  "slotVegRatio": 0.15,
  "slotCarbRatio": 0.30,
  "weightP": 0.45,
  "weightF": 0.20,
  "weightC": 0.25,
  "weightKcal": 0.10
}
```

**Validation rules (backend enforce):**
- `calMultiplier`: required, decimal trong khoảng [0.50, 1.50]
- `proteinRatio + fatRatio + carbRatio`: phải = 1.00 (precision 2 decimal)
- `slotMainRatio + slotVegRatio + slotCarbRatio`: phải = 1.00
- `weightP + weightF + weightC + weightKcal`: phải = 1.00
- Tất cả tỷ lệ: trong khoảng [0.00, 1.00]

**Response 200:** trả về config sau khi update (giống 2.1.2).

**Response 400 (validation fail):**
```json
{
  "code": "CONFIG-003",
  "message": "Tổng macro ratio phải bằng 1.00 (hiện tại: 0.95)",
  "data": null
}
```

### 2.2. Meal Ratio Config

#### 2.2.1. GET `/api/admin/configs/meals`

Lấy tỷ lệ bữa cho cả 2 plan (3 và 5 bữa).

**Auth:** ADMIN

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": {
    "plan3Meals": [
      { "mealCode": "SANG", "ratio": 0.25, "sortOrder": 1 },
      { "mealCode": "TRUA", "ratio": 0.40, "sortOrder": 2 },
      { "mealCode": "TOI",  "ratio": 0.35, "sortOrder": 3 }
    ],
    "plan5Meals": [
      { "mealCode": "SANG",      "ratio": 0.20, "sortOrder": 1 },
      { "mealCode": "PHU_SANG",  "ratio": 0.10, "sortOrder": 2 },
      { "mealCode": "TRUA",      "ratio": 0.30, "sortOrder": 3 },
      { "mealCode": "PHU_CHIEU", "ratio": 0.10, "sortOrder": 4 },
      { "mealCode": "TOI",       "ratio": 0.30, "sortOrder": 5 }
    ],
    "updatedAt": "2026-04-10T09:15:00",
    "updatedBy": "admin"
  }
}
```

#### 2.2.2. PUT `/api/admin/configs/meals/{planType}`

Cập nhật tỷ lệ cho 1 plan (3 hoặc 5 bữa).

**Auth:** ADMIN

**Path params:**
- `planType` (string): `3_BUA` hoặc `5_BUA`

**Request body:**
```json
{
  "meals": [
    { "mealCode": "SANG", "ratio": 0.25 },
    { "mealCode": "TRUA", "ratio": 0.40 },
    { "mealCode": "TOI",  "ratio": 0.35 }
  ]
}
```

**Validation:**
- Số element phải đúng theo plan (3 cho `3_BUA`, 5 cho `5_BUA`)
- `mealCode` phải khớp với code đã định nghĩa
- Tổng `ratio` phải = 1.00

**Response 200:** trả về plan sau khi update.

### 2.3. Penalty Config

#### 2.3.1. GET `/api/admin/configs/penalties`

Lấy toàn bộ tham số penalty (gom 4 nhóm vào 1 endpoint).

**Auth:** ADMIN

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": {
    "layer1": {
      "sameDay": 12,
      "oneDayBefore": 6,
      "twoDayBefore": 3
    },
    "layer2": {
      "sameDay": 6,
      "oneDayBefore": 3,
      "twoDayBefore": 1
    },
    "slotFactors": {
      "main": 1.0,
      "veg": 0.5,
      "carb": 0.0,
      "combo": 1.0
    },
    "others": {
      "penaltyCap": 40,
      "favoriteDiscount": 0.5,
      "lookbackDays": 3
    },
    "updatedAt": "2026-04-12T11:20:00",
    "updatedBy": "admin"
  }
}
```

#### 2.3.2. PUT `/api/admin/configs/penalties`

Cập nhật toàn bộ tham số penalty.

**Auth:** ADMIN

**Request body:** giống response của 2.3.1 (bỏ updatedAt, updatedBy).

**Validation:**
- `layer1.*`, `layer2.*`: integer >= 0
- `slotFactors.*`: decimal [0.0, 1.0]
- `penaltyCap`: integer >= 10
- `favoriteDiscount`: decimal [0.0, 1.0]
- `lookbackDays`: integer [1, 7]

**Response 200:** giống 2.3.1.

### 2.4. Scoring Config

#### 2.4.1. GET `/api/admin/configs/scoring`

Lấy tham số scoring không phụ thuộc goal.

**Auth:** ADMIN

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": {
    "threshold": 0.20,
    "surplusFactors": {
      "protein": 0.3,
      "fat": 0.8,
      "carb": 0.5,
      "kcal": 0.7
    },
    "reoptimize": {
      "scoreThreshold": 50,
      "scoreDrop": 15
    },
    "updatedAt": "2026-04-15T09:45:00",
    "updatedBy": "admin"
  }
}
```

#### 2.4.2. PUT `/api/admin/configs/scoring`

**Request body:** giống response 2.4.1 (bỏ updatedAt, updatedBy).

**Validation:**
- `threshold`: decimal [0.05, 0.50]
- `surplusFactors.*`: decimal [0.0, 1.0]
- `reoptimize.scoreThreshold`: integer [0, 100]
- `reoptimize.scoreDrop`: integer [0, 100]

### 2.5. System Config

#### 2.5.1. GET `/api/admin/configs/system`

**Auth:** ADMIN

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": {
    "filter": {
      "kcalTolerance": 0.15,
      "servingMin": 0.50,
      "servingMax": 2.00,
      "servingSteps": [0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0],
      "comboServingSteps": [0.75, 1.0, 1.25, 1.5]
    },
    "constraints": [
      { "slotCode": "CHINH",    "minG": 50,  "maxG": 250 },
      { "slotCode": "RAU",      "minG": 80,  "maxG": 300 },
      { "slotCode": "TINH_BOT", "minG": 80,  "maxG": 250 },
      { "slotCode": "COMBO",    "minG": 100, "maxG": 400 }
    ],
    "display": {
      "topK": 10,
      "roundStepG": 25
    },
    "updatedAt": "2026-04-17T16:30:00",
    "updatedBy": "admin"
  }
}
```

#### 2.5.2. PUT `/api/admin/configs/system`

**Request body:** giống response 2.5.1 (bỏ updatedAt, updatedBy).

**Validation:**
- `filter.kcalTolerance`: decimal [0.05, 0.30]
- `filter.servingMin`: decimal [0.25, 1.0]
- `filter.servingMax`: decimal [1.0, 3.0]
- `filter.servingSteps`: array decimal, mỗi giá trị trong [servingMin, servingMax], chứa 1.0, sorted asc, không trùng
- `filter.comboServingSteps`: tương tự
- `constraints.*.minG`: integer >= 1
- `constraints.*.maxG`: integer > minG
- `display.topK`: integer [5, 20]
- `display.roundStepG`: integer [5, 50]

---

## 3. DISH ENDPOINTS

> ⚠️ **DEFERRED — KHÔNG implement trong giai đoạn hiện tại.**
> Schema bảng `dishes` cũ chưa khớp spec v3. Sẽ refactor khi implement thuật toán đề xuất, sau đó mới implement endpoints này.

### 3.1. GET `/api/admin/dishes`

Lấy danh sách dish có pagination, filter, search.

**Auth:** ADMIN

**Query params:**
- `page` (int, default 0)
- `size` (int, default 10, max 50)
- `sort` (string, default `name,asc`): có thể sort theo `name`, `kcalPer100g`, `createdAt`
- `search` (string, optional): search theo tên (case-insensitive, contains)
- `foodGroupCode` (string, optional): filter theo food group code
- `slotCode` (string, optional): filter theo slot — `CHINH`, `RAU`, `TINH_BOT`, `COMBO`

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "dish_001",
        "name": "Phở bò",
        "imageUrl": "/api/files/dishes/abc123.jpg",
        "foodGroupCode": "COMBO",
        "foodGroupLabel": "Món combo",
        "slotCode": "COMBO",
        "slotLabel": "Combo",
        "kcalPer100g": 105.0,
        "proteinPer100g": 6.5,
        "fatPer100g": 2.3,
        "carbPer100g": 15.8,
        "unit": "tô",
        "baseServingG": 350,
        "description": "Phở bò truyền thống Hà Nội"
      }
    ],
    "totalElements": 87,
    "totalPages": 9,
    "number": 0,
    "size": 10,
    "first": true,
    "last": false,
    "empty": false
  }
}
```

### 3.2. GET `/api/admin/dishes/{id}`

Lấy chi tiết 1 dish (dùng khi mở modal sửa).

**Auth:** ADMIN

**Response 200:** giống 1 element của `content` trong 3.1.

**Response 404:**
```json
{
  "code": "DISH-001",
  "message": "Món ăn không tồn tại",
  "data": null
}
```

### 3.3. POST `/api/admin/dishes`

Tạo dish mới.

**Auth:** ADMIN

**Request body:**
```json
{
  "name": "Cơm gà xối mỡ",
  "imageUrl": "/api/files/dishes/xyz.jpg",
  "foodGroupCode": "GIA_CAM",
  "slotCode": "CHINH",
  "kcalPer100g": 180.0,
  "proteinPer100g": 18.0,
  "fatPer100g": 9.0,
  "carbPer100g": 5.0,
  "unit": "phần",
  "baseServingG": 250,
  "description": "Cơm gà xối mỡ kiểu Việt"
}
```

**Validation:**
- `name`: required, max 100 chars, unique
- `imageUrl`: optional
- `foodGroupCode`, `slotCode`: required, phải tồn tại trong DB
- `kcalPer100g`: required, decimal >= 0
- `proteinPer100g`, `fatPer100g`, `carbPer100g`: required, decimal >= 0
- `unit`: optional, max 20 chars
- `baseServingG`: required, integer [1, 1000]
- `description`: optional, max 500 chars

**Response 201:** trả về dish vừa tạo (có id).

**Response 409:**
```json
{
  "code": "DISH-002",
  "message": "Tên món ăn 'Cơm gà xối mỡ' đã tồn tại",
  "data": null
}
```

### 3.4. PUT `/api/admin/dishes/{id}`

Cập nhật dish.

**Auth:** ADMIN

**Request body:** giống POST 3.3 (toàn bộ fields).

**Validation:** giống POST. Lưu ý `name` phải unique ngoại trừ chính dish đang sửa.

**Response 200:** trả về dish sau update.

### 3.5. DELETE `/api/admin/dishes/{id}`

Xóa 1 dish.

**Auth:** ADMIN

**Response 204:** không có body.

**Response 409 (nếu dish đang được sử dụng trong meal_log):**
```json
{
  "code": "DISH-003",
  "message": "Món ăn đang được sử dụng trong 15 bữa, không thể xóa",
  "data": null
}
```

> **Note cho backend:** Để đơn giản cho MVP, có thể implement hard delete nhưng add check `meal_log_dish` count trước. Nếu dish đã được dùng, có 2 lựa chọn:
> - Block delete và bắt admin xác nhận lại với checkbox "force delete"
> - Implement soft delete (thêm field `deleted_at`)
>
> **Khuyến nghị MVP:** Block delete, trả 409. Soft delete để v2.0.

### 3.6. DELETE `/api/admin/dishes/bulk`

Xóa nhiều dish cùng lúc.

**Auth:** ADMIN

**Request body:**
```json
{
  "ids": ["dish_001", "dish_002", "dish_003"]
}
```

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": {
    "deletedCount": 2,
    "skippedCount": 1,
    "skipped": [
      { "id": "dish_002", "reason": "Đang được sử dụng trong 15 bữa" }
    ]
  }
}
```

### 3.7. GET `/api/admin/dishes/food-groups`

Lấy danh sách food group để fill dropdown filter và form.

**Auth:** ADMIN (hoặc public — vì cần dùng cả ở user view sau này)

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": [
    { "code": "GIA_CAM",       "label": "Gia cầm" },
    { "code": "THIT_DO",       "label": "Thịt đỏ" },
    { "code": "HAI_SAN",       "label": "Hải sản" },
    { "code": "CA",            "label": "Cá" },
    { "code": "TRUNG",         "label": "Trứng" },
    { "code": "DAU_DO",        "label": "Đậu đỗ" },
    { "code": "RAU_LA",        "label": "Rau lá" },
    { "code": "RAU_CU",        "label": "Rau củ" },
    { "code": "TINH_BOT_GAO",  "label": "Tinh bột gạo" },
    { "code": "TINH_BOT_MI",   "label": "Tinh bột mì" },
    { "code": "COMBO",         "label": "Món combo" },
    { "code": "BUA_PHU",       "label": "Bữa phụ" }
  ]
}
```

### 3.8. GET `/api/admin/dishes/slot-categories`

Lấy danh sách slot category.

**Auth:** ADMIN

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": [
    { "code": "CHINH",    "label": "Món chính" },
    { "code": "RAU",      "label": "Rau/phụ" },
    { "code": "TINH_BOT", "label": "Tinh bột" },
    { "code": "COMBO",    "label": "Combo" },
    { "code": "BUA_PHU",  "label": "Bữa phụ" }
  ]
}
```

### 3.9. POST `/api/admin/dishes/upload-image`

Upload ảnh dish.

**Auth:** ADMIN

**Content-Type:** `multipart/form-data`

**Form data:**
- `file` (binary): ảnh

**Validation:**
- File size: max 2MB
- File type: jpg, jpeg, png, webp

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": {
    "imageUrl": "/api/files/dishes/9f8a7b6c5d4e.jpg",
    "filename": "9f8a7b6c5d4e.jpg",
    "sizeBytes": 145823
  }
}
```

**Response 400:**
```json
{
  "code": "DISH-004",
  "message": "Ảnh không đúng định dạng (jpg/png/webp)",
  "data": null
}
```

### 3.10. GET `/api/files/dishes/{filename}`

Serve ảnh dish (public, không cần auth — để FE render trực tiếp trong `<img>` tag).

**Auth:** Không yêu cầu (cần thêm vào public endpoint list trong Gateway và `SecurityConfig` của nutrition-service)

**Response 200:** binary image với content-type tương ứng.

**Response 404:** trả về placeholder image hoặc 404 chuẩn.

> **Note cho backend:** Static resource serving qua Spring Boot — cấu hình `spring.web.resources.static-locations=file:./uploads/`. URL pattern `/api/files/**` map vào folder `uploads/`.

---

## 4. INGREDIENT ENDPOINTS

> ⚠️ **DEFERRED — KHÔNG implement trong giai đoạn hiện tại.**
> Tương tự dishes. Defer.

Pattern giống dish nhưng đơn giản hơn.

### 4.1. GET `/api/admin/ingredients`

**Query params:** `page`, `size`, `sort`, `search`, `groupCode`

**Response data.content[i]:**
```json
{
  "id": "ingr_001",
  "name": "Gạo tẻ",
  "imageUrl": "/api/files/ingredients/abc.jpg",
  "groupCode": "TINH_BOT_GAO",
  "groupLabel": "Tinh bột gạo",
  "kcalPer100g": 130.0,
  "proteinPer100g": 2.7,
  "fatPer100g": 0.3,
  "carbPer100g": 28.0,
  "description": "Gạo tẻ trắng đã nấu chín"
}
```

### 4.2. GET `/api/admin/ingredients/{id}`

### 4.3. POST `/api/admin/ingredients`

**Request body:**
```json
{
  "name": "Thịt bò thăn",
  "imageUrl": "/api/files/ingredients/xyz.jpg",
  "groupCode": "THIT_DO",
  "kcalPer100g": 250.0,
  "proteinPer100g": 26.0,
  "fatPer100g": 17.0,
  "carbPer100g": 0.0,
  "description": "Thịt bò thăn tươi"
}
```

**Validation:** giống dish, không có `slotCode`, `unit`, `baseServingG`.

### 4.4. PUT `/api/admin/ingredients/{id}`

### 4.5. DELETE `/api/admin/ingredients/{id}`

### 4.6. DELETE `/api/admin/ingredients/bulk`

### 4.7. POST `/api/admin/ingredients/upload-image`

### 4.8. GET `/api/files/ingredients/{filename}`

---

## 5. USER MANAGEMENT ENDPOINTS

User-service đã có sẵn `/api/user/allUsers` nhưng chưa có RBAC. Cần tạo endpoint admin riêng với `@PreAuthorize`.

### 5.1. GET `/api/admin/users`

Lấy danh sách user (read-only).

**Auth:** ADMIN

**Query params:**
- `page`, `size`, `sort`
- `search` (optional): search theo username, email, hoặc full name
- `role` (optional): filter `ROLE_USER`, `ROLE_ADMIN`
- `hasProfile` (optional, bool): filter user đã/chưa cập nhật profile (birthDate + gender)

**Response data.content[i]:**
```json
{
  "userId": "019dcc71-368c-7657-82d9-cb47aba4cc60",
  "username": "chien0356",
  "email": "chien@example.com",
  "name": "Đỗ Văn Chiến",
  "role": "ROLE_USER",
  "hasProfile": true,
  "createdAt": "2026-04-27T13:45:00"
}
```

### 5.2. GET `/api/admin/users/{userId}`

Chi tiết 1 user.

**Auth:** ADMIN

**Response 200:**
```json
{
  "code": null,
  "message": "Success",
  "data": {
    "account": {
      "userId": "019dcc71-...",
      "username": "chien0356",
      "email": "chien@example.com",
      "role": "ROLE_USER",
      "createdAt": "2026-04-27T13:45:00"
    },
    "profile": {
      "name": "Đỗ Văn Chiến",
      "birthDate": "2003-04-01",
      "age": 23,
      "gender": "male",
      "phone": "0901234567"
    },
    "latestHealthMetrics": {
      "weight": 64.0,
      "height": 168.0,
      "bmi": 22.7,
      "bmr": 1530.0,
      "tdee": 2371.5,
      "pbf": 15.2,
      "whr": 0.85,
      "lastUpdatedAt": "2026-04-27T14:00:00"
    }
  }
}
```

> **Note backend:** Endpoint này tổng hợp data từ user-service (account + profile) và health-data-service (latestHealthMetrics). Có 2 cách:
> - **Cách A:** Đặt trong user-service, gọi health-data-service qua Feign/REST
> - **Cách B:** Frontend gọi 2 endpoint riêng (`/api/admin/users/{id}` và `/api/health-data/users/{id}/latest`) rồi merge
>
> **Khuyến nghị Cách B** cho MVP — đơn giản, không tạo coupling giữa các service. FE gọi 2 endpoint song song với `Promise.all`.

**Response nếu chưa có profile hoặc health metrics:**
```json
{
  "data": {
    "account": { ... },
    "profile": null,
    "latestHealthMetrics": null
  }
}
```

> Frontend hiển thị "Chưa cập nhật" cho các section null.

### 5.3. (Không có) POST/PUT/DELETE user

MVP không cho admin tạo/sửa/xóa user qua API. Đã chốt ở các phiên trước.

---

## 6. SUMMARY: BẢNG ENDPOINT

| Method | Endpoint | Mô tả | Service |
|---|---|---|---|
| GET | `/api/admin/dashboard/stats` | 4 stat card | nutrition |
| GET | `/api/admin/dashboard/user-growth` | Biểu đồ user mới | nutrition |
| GET | `/api/admin/dashboard/warnings` | List cảnh báo | nutrition |
| GET | `/api/admin/configs/goals` | List 3 goal config | nutrition |
| GET | `/api/admin/configs/goals/{code}` | 1 goal config | nutrition |
| PUT | `/api/admin/configs/goals/{code}` | Update goal config | nutrition |
| GET | `/api/admin/configs/meals` | Meal ratios cả 2 plan | nutrition |
| PUT | `/api/admin/configs/meals/{planType}` | Update meal ratios | nutrition |
| GET | `/api/admin/configs/penalties` | Penalty configs | nutrition |
| PUT | `/api/admin/configs/penalties` | Update penalty configs | nutrition |
| GET | `/api/admin/configs/scoring` | Scoring configs | nutrition |
| PUT | `/api/admin/configs/scoring` | Update scoring configs | nutrition |
| GET | `/api/admin/configs/system` | System configs | nutrition |
| PUT | `/api/admin/configs/system` | Update system configs | nutrition |
| GET | `/api/admin/dishes` | List dishes (paginated) | nutrition |
| GET | `/api/admin/dishes/{id}` | Detail dish | nutrition |
| POST | `/api/admin/dishes` | Tạo dish | nutrition |
| PUT | `/api/admin/dishes/{id}` | Update dish | nutrition |
| DELETE | `/api/admin/dishes/{id}` | Xóa 1 dish | nutrition |
| DELETE | `/api/admin/dishes/bulk` | Bulk delete | nutrition |
| GET | `/api/admin/dishes/food-groups` | Dropdown food groups | nutrition |
| GET | `/api/admin/dishes/slot-categories` | Dropdown slots | nutrition |
| POST | `/api/admin/dishes/upload-image` | Upload ảnh dish | nutrition |
| GET | `/api/files/dishes/{filename}` | Serve ảnh dish (public) | nutrition |
| GET | `/api/admin/ingredients` | List ingredients | nutrition |
| GET | `/api/admin/ingredients/{id}` | Detail ingredient | nutrition |
| POST | `/api/admin/ingredients` | Tạo ingredient | nutrition |
| PUT | `/api/admin/ingredients/{id}` | Update ingredient | nutrition |
| DELETE | `/api/admin/ingredients/{id}` | Xóa ingredient | nutrition |
| DELETE | `/api/admin/ingredients/bulk` | Bulk delete | nutrition |
| POST | `/api/admin/ingredients/upload-image` | Upload ảnh ingr | nutrition |
| GET | `/api/files/ingredients/{filename}` | Serve ảnh ingr (public) | nutrition |
| GET | `/api/admin/users` | List users | user |
| GET | `/api/admin/users/{userId}` | Detail user (account+profile) | user |

**Tổng cộng:** 33 endpoints. 27 trong nutrition-service, 6 trong user-service.

---

## 7. FRONTEND SERVICE STRUCTURE

Đề xuất cấu trúc file service trong frontend:

```
src/services/
├── axios.ts                          (đã có - apiClient với interceptor)
├── auth.service.ts                    (đã có)
├── healthData.service.ts              (đã có)
├── user.service.ts                    (đã có)
└── admin/
    ├── dashboard.admin.service.ts     (1.x endpoints)
    ├── goalConfig.admin.service.ts    (2.1)
    ├── mealConfig.admin.service.ts    (2.2)
    ├── penaltyConfig.admin.service.ts (2.3)
    ├── scoringConfig.admin.service.ts (2.4)
    ├── systemConfig.admin.service.ts  (2.5)
    ├── dishes.admin.service.ts        (3.x)
    ├── ingredients.admin.service.ts   (4.x)
    ├── users.admin.service.ts         (5.x)
    └── shared.admin.service.ts        (food-groups, slot-categories)
```

Mọi service trong `admin/` dùng `apiClient` (không phải `axios` trực tiếp) để có auth interceptor + refresh token.

**Ví dụ service:**

```typescript
// src/services/admin/goalConfig.admin.service.ts
import { apiClient } from '../axios';

export interface GoalConfig {
  goalCode: 'GIAM' | 'DUY_TRI' | 'TANG';
  calMultiplier: number;
  proteinRatio: number;
  fatRatio: number;
  carbRatio: number;
  slotMainRatio: number;
  slotVegRatio: number;
  slotCarbRatio: number;
  weightP: number;
  weightF: number;
  weightC: number;
  weightKcal: number;
  description: string;
  updatedAt: string;
  updatedBy: string;
}

export type GoalConfigUpdateRequest = Omit<GoalConfig, 'goalCode' | 'description' | 'updatedAt' | 'updatedBy'>;

interface DataResponse<T> {
  code: string | null;
  message: string;
  data: T;
}

export const getAllGoalConfigs = async (): Promise<GoalConfig[]> => {
  const res = await apiClient.get<DataResponse<GoalConfig[]>>('/api/admin/configs/goals');
  return res.data.data;
};

export const getGoalConfig = async (goalCode: string): Promise<GoalConfig> => {
  const res = await apiClient.get<DataResponse<GoalConfig>>(`/api/admin/configs/goals/${goalCode}`);
  return res.data.data;
};

export const updateGoalConfig = async (
  goalCode: string,
  data: GoalConfigUpdateRequest
): Promise<GoalConfig> => {
  const res = await apiClient.put<DataResponse<GoalConfig>>(
    `/api/admin/configs/goals/${goalCode}`,
    data
  );
  return res.data.data;
};
```

---

## 8. BACKEND CONTROLLER STRUCTURE

Đề xuất cấu trúc package trong nutrition-service. **Nguyên tắc:** gộp các controller có endpoint đơn giản (~2-3 endpoint mỗi nhóm) thành 1 file để giảm số file phải maintain. Tách những controller có nhiều endpoint riêng (CRUD entity).

```
nutrition-service/src/main/java/org/example/nutritionservice/
├── controller/
│   └── admin/
│       ├── AdminConfigController.java       (gộp 5 nhóm config - 11 endpoints)
│       ├── AdminDishController.java         (3.x - 10 endpoints, đặt cả lookup endpoint)
│       ├── AdminIngredientController.java   (4.x - 8 endpoints)
│       ├── AdminDashboardController.java    (1.x - 3 endpoints)
│       └── PublicFileController.java        (file serving 3.10, 4.8 - public, không có @PreAuthorize)
├── service/
│   ├── config/
│   │   ├── GoalConfigService.java           (giữ riêng từng service - business logic khác nhau)
│   │   ├── MealConfigService.java
│   │   ├── PenaltyConfigService.java
│   │   ├── ScoringConfigService.java
│   │   └── SystemConfigService.java
│   ├── DishService.java
│   ├── IngredientService.java
│   ├── DashboardService.java
│   └── FileStorageService.java
├── repository/
├── entity/
├── dto/
│   ├── request/admin/
│   └── response/admin/
└── ...
```

**Tổng:** 5 file controller (gộp từ 9 file) + service riêng cho từng nhóm.

**Lý do gộp/tách:**
- Gộp 5 config controller → 1 `AdminConfigController` (mỗi config 2-3 endpoint, gộp lại ~11 endpoint, ~90 dòng — vẫn dễ đọc với comment phân vùng)
- KHÔNG gộp Dish/Ingredient vào AdminConfigController — chúng là CRUD entity với logic phức tạp riêng (~10 endpoint mỗi cái)
- Tách `PublicFileController` riêng vì nó là endpoint **public** (không có `@PreAuthorize`), khác hoàn toàn với các controller admin.
- Service layer giữ riêng theo domain (5 service config) vì business logic khác nhau.

### 8.1. Pattern controller gộp với comment phân vùng

```java
@RestController
@RequestMapping("/api/admin/configs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminConfigController {

    private final GoalConfigService goalConfigService;
    private final MealConfigService mealConfigService;
    private final PenaltyConfigService penaltyConfigService;
    private final ScoringConfigService scoringConfigService;
    private final SystemConfigService systemConfigService;

    // ============================================================
    // ===== GOAL CONFIG (3 endpoints)
    // ============================================================

    @GetMapping("/goals")
    public DataResponse<List<GoalConfigResponse>> getAllGoalConfigs() {
        return DataResponse.success(goalConfigService.getAll());
    }

    @GetMapping("/goals/{goalCode}")
    public DataResponse<GoalConfigResponse> getGoalConfig(@PathVariable String goalCode) {
        return DataResponse.success(goalConfigService.getByCode(goalCode));
    }

    @PutMapping("/goals/{goalCode}")
    public DataResponse<GoalConfigResponse> updateGoalConfig(
            @PathVariable String goalCode,
            @RequestBody @Valid GoalConfigUpdateRequest request) {
        return DataResponse.success(goalConfigService.update(goalCode, request));
    }

    // ============================================================
    // ===== MEAL CONFIG (2 endpoints)
    // ============================================================

    @GetMapping("/meals")
    public DataResponse<MealConfigResponse> getMealConfigs() {
        return DataResponse.success(mealConfigService.getAll());
    }

    @PutMapping("/meals/{planType}")
    public DataResponse<List<MealRatioResponse>> updateMealConfig(
            @PathVariable String planType,
            @RequestBody @Valid MealConfigUpdateRequest request) {
        return DataResponse.success(mealConfigService.update(planType, request));
    }

    // ============================================================
    // ===== PENALTY CONFIG (2 endpoints)
    // ============================================================

    @GetMapping("/penalties")
    public DataResponse<PenaltyConfigResponse> getPenaltyConfig() {
        return DataResponse.success(penaltyConfigService.get());
    }

    @PutMapping("/penalties")
    public DataResponse<PenaltyConfigResponse> updatePenaltyConfig(
            @RequestBody @Valid PenaltyConfigUpdateRequest request) {
        return DataResponse.success(penaltyConfigService.update(request));
    }

    // ============================================================
    // ===== SCORING CONFIG (2 endpoints)
    // ============================================================

    @GetMapping("/scoring")
    public DataResponse<ScoringConfigResponse> getScoringConfig() {
        return DataResponse.success(scoringConfigService.get());
    }

    @PutMapping("/scoring")
    public DataResponse<ScoringConfigResponse> updateScoringConfig(
            @RequestBody @Valid ScoringConfigUpdateRequest request) {
        return DataResponse.success(scoringConfigService.update(request));
    }

    // ============================================================
    // ===== SYSTEM CONFIG (2 endpoints)
    // ============================================================

    @GetMapping("/system")
    public DataResponse<SystemConfigResponse> getSystemConfig() {
        return DataResponse.success(systemConfigService.get());
    }

    @PutMapping("/system")
    public DataResponse<SystemConfigResponse> updateSystemConfig(
            @RequestBody @Valid SystemConfigUpdateRequest request) {
        return DataResponse.success(systemConfigService.update(request));
    }
}
```

### 8.2. Class-level annotation chuẩn

Mọi controller admin có 3 annotation chuẩn:

```java
@RestController                                  // tu Spring Web
@RequestMapping("/api/admin/...")                // base path
@PreAuthorize("hasRole('ADMIN')")                // enforce role tai class level
@RequiredArgsConstructor                         // tu Lombok - constructor injection
```

Lợi: chỉ cần 1 dòng `@PreAuthorize` ở class, áp dụng cho mọi method bên trong, không phải lặp lại.

### 8.3. PublicFileController (không có @PreAuthorize)

```java
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class PublicFileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/dishes/{filename}")
    public ResponseEntity<Resource> serveDishImage(@PathVariable String filename) {
        return fileStorageService.serveImage("dishes", filename);
    }

    @GetMapping("/ingredients/{filename}")
    public ResponseEntity<Resource> serveIngredientImage(@PathVariable String filename) {
        return fileStorageService.serveImage("ingredients", filename);
    }
}
```

### 8.4. SecurityConfig cập nhật

Public endpoint (`/api/files/**`) phải permit trong `SecurityConfig`:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/**", "/error", "/api/files/**").permitAll()
    .anyRequest().authenticated()
)
```

Đồng thời thêm vào `JwtFilter.isPublicEndpoint()` của Gateway để Gateway không yêu cầu Bearer token cho path này.

---

## 9. CÁC QUYẾT ĐỊNH ĐÃ CHỐT

### 9.1. Image storage path ✅

- Path tương đối: `./uploads/dishes/<uuid>.ext` và `./uploads/ingredients/<uuid>.ext`
- File naming: UUID + extension (vd `9f8a7b6c-5d4e-3f2a.jpg`) — không dùng tên gốc
- Cleanup: **giữ file** khi DELETE dish, không xóa file vật lý. Có script cleanup riêng để chạy thủ công khi cần.
- Static resource serving: cấu hình `spring.web.resources.static-locations=file:./uploads/` trong `application.yml`

### 9.2. Cách lưu trữ system_config ✅

API expose dưới dạng **object structured** (filter, constraints, display) thay vì list key-value flat. Backend mapping:
- Đọc: query nhiều key từ bảng `system_config` → assemble thành object structured trong service layer
- Ghi: nhận object structured từ request → split thành nhiều INSERT/UPDATE keys

DB schema chi tiết sẽ define ở tài liệu DB schema riêng.

### 9.3. Cache strategy ✅

**Không cache.** Đọc DB mỗi lần. Đủ nhanh cho < 100 user trong scope MVP.

Nếu sau này có vấn đề performance, có thể add `@Cacheable` lên method service đọc config với TTL 5 phút và `@CacheEvict` lên method update — không cần thay đổi API contract.

### 9.4. Concurrent edit ✅

**Last-write-wins.** Hai admin sửa cùng 1 config cùng lúc → ai save sau ghi đè ai save trước. MVP không implement optimistic locking (version field).

Lý do chấp nhận:
- Số admin rất ít (1 người cho thesis)
- Tần suất sửa config thấp (vài lần/tuần)
- Risk thực tế gần như bằng 0

Nếu sau này có nhiều admin, thêm `@Version` field và xử lý `OptimisticLockException` trong service.

---

## 10. CHECKLIST IMPLEMENT THỨ TỰ

> ⚠️ **Checklist này phản ánh SCOPE THỰC TẾ của giai đoạn hiện tại** (xem Section 0.0).

Đề xuất thứ tự implement endpoint:

### Giai đoạn 1: Configs đơn giản (1 entity, 2-3 endpoints)
- [ ] 2.1 Goal config (3 endpoints) — **làm đầu tiên**, test thoroughly
- [ ] 2.4 Scoring config (2 endpoints)

### Giai đoạn 2: Configs phức tạp hơn (composite key hoặc nhiều bảng)
- [ ] 2.2 Meal ratio config (2 endpoints)
- [ ] 2.3 Penalty config (2 endpoints)
- [ ] 2.5 System config (2 endpoints)

### Giai đoạn 3: User management
- [ ] 5.1 list users
- [ ] 5.2 user detail (cần phối hợp 2 service — frontend gọi 2 endpoint song song)

### ~~Giai đoạn 4, 5~~ — DEFERRED
- ~~Dishes & Ingredients (3.x, 4.x)~~ → làm sau khi refactor schema
- ~~Dashboard (1.x)~~ → làm sau khi có đủ data từ dishes

---

## LỊCH SỬ THAY ĐỔI

| Phiên bản | Ngày | Thay đổi |
|---|---|---|
| v1.0 | 30/04/2026 | Tài liệu ban đầu, 33 endpoints, 6 nhóm chức năng |
| v1.1 | 30/04/2026 | Gộp 5 config controller thành AdminConfigController. Chốt 4 quyết định Section 9 (image path, system_config mapping, no cache, last-write-wins). Tách PublicFileController. |
| v1.2 | 30/04/2026 | Thêm scope notice — defer Dashboard (Section 1), Dishes (Section 3), Ingredients (Section 4) đến sau khi refactor schema dishes/ingredients. Giai đoạn hiện tại chỉ implement ~16 endpoints (Configs + Users). |
