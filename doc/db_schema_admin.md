# DB SCHEMA — NUTRITION SERVICE (ADMIN MODULE)

> Đặc tả database schema cho admin panel của HealthCare app.
> Dành cho cả Claude Code (sinh entity/repository) và developer (review/migrate).
> Phiên bản: v1.0
> Ngày: 30/04/2026

---

## 0. NGUYÊN TẮC THIẾT KẾ

### 0.0. ⚠️ SCOPE THỰC TẾ CHO GIAI ĐOẠN HIỆN TẠI (v1.1)

Tài liệu này mô tả 10 bảng đầy đủ, nhưng **scope thực tế của giai đoạn hiện tại CHỈ tạo những bảng sau**:

**TẠO TRONG GIAI ĐOẠN NÀY (5 bảng config typed + 1 key-value):**
- Section 4.1: `goal_configs`
- Section 4.2: `meal_ratio_configs`
- Section 4.3: `penalty_configs`
- Section 4.4: `slot_configs`
- Section 4.5: `surplus_penalty_configs`
- Section 5: `system_config`

**TẠM HOÃN tạo trong giai đoạn này:**
- Section 2.1: `food_groups` — chưa cần (chưa làm dishes/ingredients UI)
- Section 2.2: `slot_categories` — chưa cần
- Section 3.1: `dishes` (mới) — schema cũ trong nutrition_db giữ nguyên, không động tới
- Section 3.2: `ingredients` (mới) — tương tự

**Hiện trạng `nutrition_db`:**
- Đã có 3 bảng cũ: `dishes`, `ingredients`, `dish_ingredients` — **GIỮ NGUYÊN**, không sửa, không xóa
- Sẽ thêm 6 bảng config mới — **độc lập** hoàn toàn với 3 bảng cũ
- Khi implement thuật toán đề xuất sau này, sẽ refactor 3 bảng cũ + tạo `food_groups`, `slot_categories`

Vì 6 bảng config không có FK với `food_groups` hay `slot_categories`, scope này hoạt động độc lập, không cần lookup tables.

---

### 0.1. Phạm vi

Tài liệu này định nghĩa schema cho database **`nutrition_db`** của nutrition-service. Schema cho `user_db` (auth, users) và `health_db` (metrics) đã có sẵn, không thuộc phạm vi tài liệu này.

### 0.2. Nguyên tắc cốt lõi

**Hạn chế Foreign Key cross-service.** Microservice principle: mỗi service quản lý DB riêng, không tạo FK trỏ sang DB của service khác. Cụ thể:

- Cột `user_id` trong nutrition_db **KHÔNG** có FK trỏ tới `user_db.users(id)`. Chỉ là plain `VARCHAR(36)` lưu UUID String. Validation tính tồn tại của user thực hiện ở **application layer** (gọi user-service qua REST nếu cần), không phải DB layer.
- Tương tự, không có FK trỏ tới `health_db`.

**FK trong cùng DB vẫn dùng**, nhưng tối thiểu:
- Chỉ tạo FK khi quan hệ thực sự bắt buộc và xóa cha → xóa con là behavior mong muốn
- Ưu tiên `ON DELETE RESTRICT` (block xóa nếu còn ràng buộc) thay vì `CASCADE` (auto xóa con) để tránh data loss bất ngờ
- KHÔNG dùng `@OneToOne` trừ khi cực kỳ cần thiết (Hibernate có nhiều quirk với @OneToOne)

**Hạn chế @ManyToOne, @OneToMany trong code Java.** Lý do:
- Các association tự động fetch dễ gây N+1 query
- LAZY loading throw `LazyInitializationException` ngoài transaction
- Khó debug khi data lớn

→ **Pattern khuyến nghị:** entity chỉ lưu **ID của bảng liên quan** (vd `food_group_code` thay vì `@ManyToOne FoodGroup foodGroup`). Khi cần data của bảng liên quan, query thủ công ở service layer.

### 0.3. Kiểu dữ liệu chuẩn

| Loại data | MySQL type | Lý do |
|---|---|---|
| Primary key UUID | `VARCHAR(36)` | UUID v7 String, đồng bộ với user-service refactor Phase 2 |
| Code/enum cố định | `VARCHAR(20)` đến `VARCHAR(30)` | Đủ cho các code dạng `GIA_CAM`, `TINH_BOT_GAO` |
| Tỷ lệ (0.0 - 1.0) | `DECIMAL(3,2)` | Precision đủ (0.00 - 9.99), tránh floating point error |
| Hệ số nhỏ (0-2) | `DECIMAL(4,3)` | Cho serving multipliers (0.500 - 2.000) |
| Penalty integer | `SMALLINT` | Tiết kiệm storage, đủ cho range 0-32K |
| Kcal, gram | `DECIMAL(6,2)` | Đủ cho 0 - 9999.99 |
| Tên dài | `VARCHAR(100)` | Tên món, nguyên liệu |
| Mô tả | `VARCHAR(500)` | Đủ cho mô tả ngắn |
| Timestamp | `DATETIME` | Standard cho audit field |

### 0.4. Field audit chuẩn (mọi bảng đều có)

```sql
created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
created_by  VARCHAR(36) NULL,
updated_by  VARCHAR(36) NULL,
```

**Note:**
- `created_by`, `updated_by` lưu `userId` (UUID String) của admin thực hiện thao tác. NULL nếu là seed data hoặc system action.
- KHÔNG có FK trỏ tới user-service (xem Section 0.2).
- KHÔNG dùng field `status` chung cho mọi bảng — chỉ thêm khi thực sự cần (vd `is_active` cho dishes).

### 0.5. Naming convention

- Tên bảng: `snake_case`, danh từ số nhiều (`dishes`, `goal_configs`)
- Ngoại lệ: bảng config 1-row-per-key dùng số ít (`system_config`)
- Tên cột: `snake_case`, danh từ
- Tên index: `idx_<table>_<column>` (ví dụ `idx_dishes_food_group`)
- Tên FK constraint: `fk_<child_table>_<parent_table>` (ví dụ `fk_dishes_food_groups`)

---

## 1. ERD TỔNG THỂ

```
┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│   food_groups    │       │  slot_categories │       │     dishes       │
│   (12 rows seed) │       │   (5 rows seed)  │       │     (CRUD)       │
├──────────────────┤       ├──────────────────┤       ├──────────────────┤
│ code PK          │◄──────┤ code PK          │◄──────┤ id PK            │
│ label            │       │ label            │       │ name             │
│ sort_order       │       │ sort_order       │       │ food_group_code  │
└──────────────────┘       └──────────────────┘       │ slot_code        │
         ▲                          ▲                  │ kcal_per_100g    │
         │                          │                  │ ...              │
         │                          │                  └──────────────────┘
         └────────┐           ┌─────┘                          │
                  │           │                                 │
                  │           │              ┌──────────────────────┐
         ┌────────┴───────────┴─┐            │                      │
         │     ingredients      │     ┌──────┴──────┐    ┌──────────┴────────┐
         │     (CRUD)           │     │ slot_config │    │ dish_food_relations│
         ├──────────────────────┤     │ (4 rows)    │    │ (NOT IN MVP)      │
         │ id PK                │     ├─────────────┤    └───────────────────┘
         │ name                 │     │ slot_code PK│
         │ group_code           │     │ slot_factor │
         │ kcal_per_100g        │     │ min_g       │
         │ ...                  │     │ max_g       │
         └──────────────────────┘     └─────────────┘

═════════════════════════ CONFIGS ═════════════════════════

┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│   goal_configs   │       │meal_ratio_config │       │  penalty_config  │
│   (3 rows fixed) │       │ (8 rows fixed)   │       │ (6 rows fixed)   │
├──────────────────┤       ├──────────────────┤       ├──────────────────┤
│ goal_code PK     │       │ plan_type PK     │       │ layer PK         │
│ cal_multiplier   │       │ meal_code PK     │       │ distance_days PK │
│ protein_ratio    │       │ ratio            │       │ penalty_value    │
│ ...              │       │ sort_order       │       └──────────────────┘
└──────────────────┘       └──────────────────┘

┌──────────────────────┐         ┌──────────────────┐
│ surplus_penalty_cfg  │         │  system_config   │
│ (4 rows fixed)       │         │ (~14 keys)       │
├──────────────────────┤         ├──────────────────┤
│ macro_code PK        │         │ config_key PK    │
│ factor               │         │ config_value     │
└──────────────────────┘         │ value_type       │
                                 │ description      │
                                 └──────────────────┘
```

**Quan sát:**
- 4 bảng có FK nội bộ (`dishes` → `food_groups` + `slot_categories`, `ingredients` → `food_groups`)
- 5 bảng config độc lập, không có FK
- KHÔNG có bảng nào có FK tới user_db hay health_db

---

## 2. BẢNG LOOKUP (seed data, ít thay đổi)

> ⚠️ **DEFERRED — KHÔNG tạo trong giai đoạn hiện tại.**
> 2 bảng lookup này (`food_groups`, `slot_categories`) chỉ dùng cho dishes/ingredients (đã defer). Khi nào làm thuật toán đề xuất + refactor schema dishes/ingredients thì mới tạo.

### 2.1. Bảng `food_groups`

Danh mục nhóm thực phẩm. Dùng cho cả `dishes` và `ingredients`.

```sql
CREATE TABLE food_groups (
    code         VARCHAR(20)  NOT NULL,
    label        VARCHAR(100) NOT NULL,
    sort_order   SMALLINT     NOT NULL DEFAULT 999,
    
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   VARCHAR(36)  NULL,
    updated_by   VARCHAR(36)  NULL,
    
    PRIMARY KEY (code)
);
```

**Seed data:**
```sql
INSERT INTO food_groups (code, label, sort_order) VALUES
('GIA_CAM',       'Gia cầm',          1),
('THIT_DO',       'Thịt đỏ',          2),
('HAI_SAN',       'Hải sản',          3),
('CA',            'Cá',               4),
('TRUNG',         'Trứng',            5),
('DAU_DO',        'Đậu đỗ',           6),
('RAU_LA',        'Rau lá',           7),
('RAU_CU',        'Rau củ',           8),
('TINH_BOT_GAO',  'Tinh bột gạo',     9),
('TINH_BOT_MI',   'Tinh bột mì',      10),
('COMBO',         'Món combo',        11),
('BUA_PHU',       'Bữa phụ',          12);
```

**Note:**
- Admin có thể thêm food_group mới qua admin panel (v2.0). MVP cho phép thêm/sửa nhưng không bắt buộc UI.
- Code dùng `UPPER_SNAKE_CASE`, không có dấu/khoảng trắng.

### 2.2. Bảng `slot_categories`

Loại slot trong bữa ăn.

```sql
CREATE TABLE slot_categories (
    code         VARCHAR(20)  NOT NULL,
    label        VARCHAR(50)  NOT NULL,
    sort_order   SMALLINT     NOT NULL DEFAULT 999,
    
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   VARCHAR(36)  NULL,
    updated_by   VARCHAR(36)  NULL,
    
    PRIMARY KEY (code)
);
```

**Seed data:**
```sql
INSERT INTO slot_categories (code, label, sort_order) VALUES
('CHINH',    'Món chính',  1),
('RAU',      'Rau/phụ',    2),
('TINH_BOT', 'Tinh bột',   3),
('COMBO',    'Combo',      4),
('BUA_PHU',  'Bữa phụ',    5);
```

---

## 3. BẢNG ENTITY CHÍNH (CRUD)

> ⚠️ **DEFERRED — KHÔNG tạo trong giai đoạn hiện tại.**
> Schema `dishes`, `ingredients` mới ở Section này CHƯA tạo. Schema cũ trong `nutrition_db` (đã có sẵn) giữ nguyên, không sửa, không xóa. Sẽ refactor sau khi implement thuật toán đề xuất.

### 3.1. Bảng `dishes`

Món ăn — entity quan trọng nhất, dùng cho thuật toán đề xuất.

```sql
CREATE TABLE dishes (
    id                 VARCHAR(36)  NOT NULL,
    name               VARCHAR(100) NOT NULL,
    image_url          VARCHAR(255) NULL,
    description        VARCHAR(500) NULL,
    
    food_group_code    VARCHAR(20)  NOT NULL,
    slot_code          VARCHAR(20)  NOT NULL,
    
    kcal_per_100g      DECIMAL(6,2) NOT NULL,
    protein_per_100g   DECIMAL(5,2) NOT NULL DEFAULT 0,
    fat_per_100g       DECIMAL(5,2) NOT NULL DEFAULT 0,
    carb_per_100g      DECIMAL(5,2) NOT NULL DEFAULT 0,
    
    unit               VARCHAR(20)  NULL,
    base_serving_g     SMALLINT     NOT NULL,
    
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by         VARCHAR(36)  NULL,
    updated_by         VARCHAR(36)  NULL,
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_dishes_name (name),
    
    CONSTRAINT fk_dishes_food_groups 
        FOREIGN KEY (food_group_code) REFERENCES food_groups(code)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_dishes_slot_categories 
        FOREIGN KEY (slot_code) REFERENCES slot_categories(code)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    
    CONSTRAINT chk_dishes_kcal CHECK (kcal_per_100g >= 0),
    CONSTRAINT chk_dishes_macro CHECK (
        protein_per_100g >= 0 AND fat_per_100g >= 0 AND carb_per_100g >= 0
    ),
    CONSTRAINT chk_dishes_serving CHECK (base_serving_g > 0 AND base_serving_g <= 1000)
);

CREATE INDEX idx_dishes_food_group ON dishes(food_group_code);
CREATE INDEX idx_dishes_slot       ON dishes(slot_code);
CREATE INDEX idx_dishes_active     ON dishes(is_active);
CREATE INDEX idx_dishes_name       ON dishes(name);
```

**Note thiết kế:**
- `id` UUID String (như `user_id`) — thống nhất với pattern Phase 2 refactor
- `is_active`: thay vì hard delete khi dish đang được dùng (xem API contract Section 3.5), set `is_active = false`. Thuật toán đề xuất chỉ query `WHERE is_active = TRUE`.
- `unit` nullable: một số món không có "đơn vị tự nhiên" (ví dụ "rau muống 50g")
- 4 index: 3 cột filter chính (food_group, slot, active) + 1 cột search (name)
- KHÔNG có FK với ingredient — đã chốt Cách A (dish lưu dinh dưỡng tự thân, không tính từ ingredient)
- CHECK constraints: precision đủ cho kcal/macro reasonable, serving không > 1kg
- `name` UNIQUE để chặn trùng tên

### 3.2. Bảng `ingredients`

Nguyên liệu — tham khảo, không liên kết trực tiếp với dishes trong MVP.

```sql
CREATE TABLE ingredients (
    id                 VARCHAR(36)  NOT NULL,
    name               VARCHAR(100) NOT NULL,
    image_url          VARCHAR(255) NULL,
    description        VARCHAR(500) NULL,
    
    group_code         VARCHAR(20)  NOT NULL,
    
    kcal_per_100g      DECIMAL(6,2) NOT NULL,
    protein_per_100g   DECIMAL(5,2) NOT NULL DEFAULT 0,
    fat_per_100g       DECIMAL(5,2) NOT NULL DEFAULT 0,
    carb_per_100g      DECIMAL(5,2) NOT NULL DEFAULT 0,
    
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by         VARCHAR(36)  NULL,
    updated_by         VARCHAR(36)  NULL,
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_ingredients_name (name),
    
    CONSTRAINT fk_ingredients_food_groups 
        FOREIGN KEY (group_code) REFERENCES food_groups(code)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    
    CONSTRAINT chk_ingredients_kcal CHECK (kcal_per_100g >= 0),
    CONSTRAINT chk_ingredients_macro CHECK (
        protein_per_100g >= 0 AND fat_per_100g >= 0 AND carb_per_100g >= 0
    )
);

CREATE INDEX idx_ingredients_group  ON ingredients(group_code);
CREATE INDEX idx_ingredients_active ON ingredients(is_active);
CREATE INDEX idx_ingredients_name   ON ingredients(name);
```

**Note:**
- Tương tự dishes nhưng không có `slot_code`, `unit`, `base_serving_g`
- Không có quan hệ với dishes trong MVP. Nếu sau này cần (Cách B — dish được tạo từ ingredients), thêm bảng `dish_ingredients` riêng.

---

## 4. BẢNG CONFIG TYPED (Hybrid approach)

### 4.1. Bảng `goal_configs`

Cấu hình theo mục tiêu — 3 rows fixed (GIAM, DUY_TRI, TANG).

```sql
CREATE TABLE goal_configs (
    goal_code          VARCHAR(20)  NOT NULL,
    
    -- He so deficit/surplus
    cal_multiplier     DECIMAL(3,2) NOT NULL,
    
    -- Ty le macro (tong = 1.00)
    protein_ratio      DECIMAL(3,2) NOT NULL,
    fat_ratio          DECIMAL(3,2) NOT NULL,
    carb_ratio         DECIMAL(3,2) NOT NULL,
    
    -- Ty le slot (tong = 1.00)
    slot_main_ratio    DECIMAL(3,2) NOT NULL,
    slot_veg_ratio     DECIMAL(3,2) NOT NULL,
    slot_carb_ratio    DECIMAL(3,2) NOT NULL,
    
    -- Trong so scoring (tong = 1.00)
    weight_p           DECIMAL(3,2) NOT NULL,
    weight_f           DECIMAL(3,2) NOT NULL,
    weight_c           DECIMAL(3,2) NOT NULL,
    weight_kcal        DECIMAL(3,2) NOT NULL,
    
    description        VARCHAR(100) NOT NULL,
    
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by         VARCHAR(36)  NULL,
    updated_by         VARCHAR(36)  NULL,
    
    PRIMARY KEY (goal_code),
    
    -- Validation tong = 1.00 (precision 2 decimal)
    CONSTRAINT chk_goal_macro_sum CHECK (
        ABS(protein_ratio + fat_ratio + carb_ratio - 1.00) < 0.01
    ),
    CONSTRAINT chk_goal_slot_sum CHECK (
        ABS(slot_main_ratio + slot_veg_ratio + slot_carb_ratio - 1.00) < 0.01
    ),
    CONSTRAINT chk_goal_weight_sum CHECK (
        ABS(weight_p + weight_f + weight_c + weight_kcal - 1.00) < 0.01
    ),
    
    -- Range hop ly
    CONSTRAINT chk_goal_cal_mul CHECK (cal_multiplier BETWEEN 0.50 AND 1.50)
);
```

**Seed data:**
```sql
INSERT INTO goal_configs (
    goal_code, cal_multiplier, 
    protein_ratio, fat_ratio, carb_ratio,
    slot_main_ratio, slot_veg_ratio, slot_carb_ratio,
    weight_p, weight_f, weight_c, weight_kcal,
    description
) VALUES
('GIAM',    0.80, 0.35, 0.30, 0.35, 0.55, 0.15, 0.30, 0.45, 0.20, 0.25, 0.10, 'Giảm cân'),
('DUY_TRI', 1.00, 0.25, 0.30, 0.45, 0.50, 0.15, 0.35, 0.30, 0.25, 0.35, 0.10, 'Duy trì'),
('TANG',    1.15, 0.30, 0.25, 0.45, 0.45, 0.15, 0.40, 0.35, 0.20, 0.35, 0.10, 'Tăng cân');
```

**Note thiết kế:**
- 11 cột typed thay vì JSON → DB enforce CHECK constraint cho validation tổng
- Dùng `ABS(... - 1.00) < 0.01` thay vì `= 1.00` để tránh floating point precision issue
- `goal_code` là PK String (không có id auto-increment) — chỉ 3 rows, code chính là identifier tự nhiên

### 4.2. Bảng `meal_ratio_configs`

Tỷ lệ kcal theo bữa — 8 rows (3 cho plan 3 bữa + 5 cho plan 5 bữa).

```sql
CREATE TABLE meal_ratio_configs (
    plan_type    VARCHAR(10)  NOT NULL,
    meal_code    VARCHAR(20)  NOT NULL,
    ratio        DECIMAL(3,2) NOT NULL,
    sort_order   SMALLINT     NOT NULL,
    
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   VARCHAR(36)  NULL,
    updated_by   VARCHAR(36)  NULL,
    
    PRIMARY KEY (plan_type, meal_code),
    
    CONSTRAINT chk_meal_plan_type CHECK (plan_type IN ('3_BUA', '5_BUA')),
    CONSTRAINT chk_meal_ratio CHECK (ratio BETWEEN 0.05 AND 0.60)
);
```

**Seed data:**
```sql
INSERT INTO meal_ratio_configs (plan_type, meal_code, ratio, sort_order) VALUES
('3_BUA', 'SANG',      0.25, 1),
('3_BUA', 'TRUA',      0.40, 2),
('3_BUA', 'TOI',       0.35, 3),
('5_BUA', 'SANG',      0.20, 1),
('5_BUA', 'PHU_SANG',  0.10, 2),
('5_BUA', 'TRUA',      0.30, 3),
('5_BUA', 'PHU_CHIEU', 0.10, 4),
('5_BUA', 'TOI',       0.30, 5);
```

**Note:**
- Composite PK `(plan_type, meal_code)` — chống thêm duplicate, không cần auto id
- Validate tổng = 1.00 cho từng plan_type **không thực hiện ở DB** (CHECK constraint MySQL không support cross-row). Validate ở service layer khi PUT.
- `sort_order` để frontend sort hiển thị đúng thứ tự thời gian trong ngày

### 4.3. Bảng `penalty_configs`

Penalty Layer 1 và Layer 2 — 6 rows fixed.

```sql
CREATE TABLE penalty_configs (
    layer            TINYINT  NOT NULL,
    distance_days    TINYINT  NOT NULL,
    penalty_value    SMALLINT NOT NULL,
    
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       VARCHAR(36) NULL,
    updated_by       VARCHAR(36) NULL,
    
    PRIMARY KEY (layer, distance_days),
    
    CONSTRAINT chk_penalty_layer CHECK (layer IN (1, 2)),
    CONSTRAINT chk_penalty_distance CHECK (distance_days BETWEEN 0 AND 7),
    CONSTRAINT chk_penalty_value CHECK (penalty_value >= 0 AND penalty_value <= 100)
);
```

**Seed data:**
```sql
INSERT INTO penalty_configs (layer, distance_days, penalty_value) VALUES
(1, 0, 12),  -- Layer 1, cung ngay
(1, 1, 6),   -- Layer 1, 1 ngay truoc
(1, 2, 3),   -- Layer 1, 2 ngay truoc
(2, 0, 6),   -- Layer 2, cung ngay
(2, 1, 3),   -- Layer 2, 1 ngay truoc
(2, 2, 1);   -- Layer 2, 2 ngay truoc
```

### 4.4. Bảng `slot_configs`

Cấu hình theo slot (factor + min/max gram) — 4 rows fixed.

```sql
CREATE TABLE slot_configs (
    slot_code     VARCHAR(20)  NOT NULL,
    slot_factor   DECIMAL(2,1) NOT NULL,
    min_g         SMALLINT     NOT NULL,
    max_g         SMALLINT     NOT NULL,
    
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by    VARCHAR(36)  NULL,
    updated_by    VARCHAR(36)  NULL,
    
    PRIMARY KEY (slot_code),
    
    CONSTRAINT chk_slot_factor CHECK (slot_factor BETWEEN 0.0 AND 1.0),
    CONSTRAINT chk_slot_g_range CHECK (min_g > 0 AND max_g > min_g AND max_g <= 1000)
);
```

**Seed data:**
```sql
INSERT INTO slot_configs (slot_code, slot_factor, min_g, max_g) VALUES
('CHINH',    1.0, 50,  250),
('RAU',      0.5, 80,  300),
('TINH_BOT', 0.0, 80,  250),
('COMBO',    1.0, 100, 400);
```

**Note quan trọng:** Bảng này KHÔNG có FK trỏ tới `slot_categories(code)` mặc dù logic về slot chung. Lý do:
- `slot_categories` có 5 entries (gồm cả `BUA_PHU`)
- `slot_configs` chỉ có 4 entries (không config riêng cho `BUA_PHU` vì bữa phụ xử lý đặc biệt — xem spec v3 §5.4)
- Nếu add FK, phải hoặc thêm row `BUA_PHU` vào `slot_configs` (không cần thiết) hoặc làm phức tạp logic
- Quyết định: giữ tách biệt, validate ở application layer

### 4.5. Bảng `surplus_penalty_configs`

Hệ số phạt khi DƯ macro/kcal — 4 rows fixed.

```sql
CREATE TABLE surplus_penalty_configs (
    macro_code   VARCHAR(10)  NOT NULL,
    factor       DECIMAL(2,1) NOT NULL,
    
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   VARCHAR(36)  NULL,
    updated_by   VARCHAR(36)  NULL,
    
    PRIMARY KEY (macro_code),
    
    CONSTRAINT chk_surplus_macro CHECK (macro_code IN ('PROTEIN', 'FAT', 'CARB', 'KCAL')),
    CONSTRAINT chk_surplus_factor CHECK (factor BETWEEN 0.0 AND 1.0)
);
```

**Seed data:**
```sql
INSERT INTO surplus_penalty_configs (macro_code, factor) VALUES
('PROTEIN', 0.3),
('FAT',     0.8),
('CARB',    0.5),
('KCAL',    0.7);
```

---

## 5. BẢNG `system_config` (Key-Value cho scalar)

Các config đơn lẻ không có cấu trúc rõ ràng để typed.

```sql
CREATE TABLE system_config (
    config_key      VARCHAR(100) NOT NULL,
    config_value    VARCHAR(500) NOT NULL,
    value_type      VARCHAR(20)  NOT NULL,
    description     VARCHAR(255) NULL,
    editable        BOOLEAN      NOT NULL DEFAULT TRUE,
    
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(36)  NULL,
    updated_by      VARCHAR(36)  NULL,
    
    PRIMARY KEY (config_key),
    
    CONSTRAINT chk_system_value_type CHECK (
        value_type IN ('DECIMAL', 'INT', 'JSON_ARRAY', 'BOOLEAN')
    )
);
```

**Seed data (mapping với API contract Section 2.5):**
```sql
INSERT INTO system_config (config_key, config_value, value_type, description) VALUES
-- Filter group
('filter.kcal_tolerance',       '0.15',
    'DECIMAL', 'Bien loc kcal (15%)'),
('filter.serving_min',          '0.50',
    'DECIMAL', 'Muc serving nho nhat'),
('filter.serving_max',          '2.00',
    'DECIMAL', 'Muc serving lon nhat'),
('filter.serving_steps',        '[0.5,0.75,1.0,1.25,1.5,1.75,2.0]',
    'JSON_ARRAY', 'Cac muc serving cho mon thuong'),
('filter.combo_serving_steps',  '[0.75,1.0,1.25,1.5]',
    'JSON_ARRAY', 'Cac muc serving cho mon combo'),

-- Penalty group (cac scalar khong vao penalty_configs)
('penalty.cap',                 '40',
    'INT',     'Penalty toi da cho 1 to hop'),
('penalty.fav_discount',        '0.5',
    'DECIMAL', 'He so giam penalty cho mon yeu thich'),
('penalty.lookback_days',       '3',
    'INT',     'So ngay nhin lai lich su'),

-- Scoring group
('score.threshold',             '0.20',
    'DECIMAL', 'Nguong deviation macro'),
('reopt.score_threshold',       '50',
    'INT',     'Goi y khi Final Score < nguong nay'),
('reopt.score_drop',            '15',
    'INT',     'Goi y khi score giam > so nay'),

-- Display group
('display.top_k',               '10',
    'INT',     'So to hop hien thi top'),
('display.round_step_g',        '25',
    'INT',     'Lam tron serving (gam)');
```

**Note thiết kế:**
- Bảng key-value vì các config này **không có quan hệ với nhau**, không cần tạo bảng typed
- `value_type` để service layer biết cách parse (`Integer.parseInt`, `BigDecimal`, `JSON.parse`)
- `editable` để khóa một số config quan trọng không cho admin sửa qua UI (mặc dù mặc định all editable)
- Naming `<group>.<key>` — service layer dùng prefix để gom nhóm khi expose API

---

## 6. MAPPING DB ↔ API STRUCTURED OBJECT

API contract (Section 2.5) expose system_config dưới dạng object structured. Đây là mapping logic **ở service layer**.

### 6.1. Đọc (GET /api/admin/configs/system)

```java
// Service đọc tất cả keys, group theo prefix, parse value_type
public SystemConfigResponse getSystemConfig() {
    Map<String, SystemConfig> map = repository.findAll().stream()
        .collect(Collectors.toMap(SystemConfig::getConfigKey, c -> c));
    
    return SystemConfigResponse.builder()
        .filter(FilterConfig.builder()
            .kcalTolerance(parseDecimal(map.get("filter.kcal_tolerance")))
            .servingMin(parseDecimal(map.get("filter.serving_min")))
            .servingMax(parseDecimal(map.get("filter.serving_max")))
            .servingSteps(parseJsonArray(map.get("filter.serving_steps")))
            .comboServingSteps(parseJsonArray(map.get("filter.combo_serving_steps")))
            .build())
        .display(DisplayConfig.builder()
            .topK(parseInt(map.get("display.top_k")))
            .roundStepG(parseInt(map.get("display.round_step_g")))
            .build())
        // ... constraints lay tu slot_configs (xem 6.3)
        .build();
}
```

### 6.2. Ghi (PUT /api/admin/configs/system)

```java
@Transactional
public void updateSystemConfig(SystemConfigUpdateRequest req) {
    // Update tung key
    updateKey("filter.kcal_tolerance", req.getFilter().getKcalTolerance().toString());
    updateKey("filter.serving_min", req.getFilter().getServingMin().toString());
    // ... loop qua tat ca keys
    
    // Update slot_configs (constraints) - bang khac
    for (ConstraintItem item : req.getConstraints()) {
        slotConfigRepository.updateMinMax(item.getSlotCode(), item.getMinG(), item.getMaxG());
    }
}
```

### 6.3. Constraints field trong API

API field `constraints` trong response GET /system **không** lấy từ bảng `system_config`, mà từ bảng `slot_configs` (Section 4.4):

```java
.constraints(slotConfigRepository.findAll().stream()
    .map(sc -> ConstraintItem.builder()
        .slotCode(sc.getSlotCode())
        .minG(sc.getMinG())
        .maxG(sc.getMaxG())
        .build())
    .toList())
```

Lý do: `slot_configs` đã có `min_g/max_g` typed sẵn, không cần duplicate vào `system_config`. Service layer assemble lại trong response.

---

## 7. JAVA ENTITY PATTERN

### 7.1. Entity với code làm PK

```java
@Entity
@Table(name = "goal_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalConfig {

    @Id
    @Column(name = "goal_code", length = 20)
    private String goalCode;
    
    @Column(name = "cal_multiplier", precision = 3, scale = 2, nullable = false)
    private BigDecimal calMultiplier;
    
    @Column(name = "protein_ratio", precision = 3, scale = 2, nullable = false)
    private BigDecimal proteinRatio;
    
    // ... cac field khac
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 36)
    private String createdBy;
    
    @Column(name = "updated_by", length = 36)
    private String updatedBy;
}
```

**Note:**
- `@CreationTimestamp` và `@UpdateTimestamp` của Hibernate tự fill timestamp — không cần xử lý thủ công
- `BigDecimal` cho mọi DECIMAL trong DB (KHÔNG dùng `Double` để tránh floating point error)
- `@GeneratedValue` không cần khi PK là String code

### 7.2. Entity với composite PK

```java
@Entity
@Table(name = "meal_ratio_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(MealRatioConfigId.class)
public class MealRatioConfig {

    @Id
    @Column(name = "plan_type", length = 10)
    private String planType;
    
    @Id
    @Column(name = "meal_code", length = 20)
    private String mealCode;
    
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal ratio;
    
    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;
    
    // audit fields
}

// Composite PK class
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MealRatioConfigId implements Serializable {
    private String planType;
    private String mealCode;
}
```

### 7.3. Entity với UUID PK

```java
@Entity
@Table(name = "dishes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dish {

    @Id
    @Column(length = 36)
    private String id;     // UUID v7 String, generate trong service trước save
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(name = "image_url", length = 255)
    private String imageUrl;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "food_group_code", nullable = false, length = 20)
    private String foodGroupCode;       // KHONG dung @ManyToOne FoodGroup
    
    @Column(name = "slot_code", nullable = false, length = 20)
    private String slotCode;            // KHONG dung @ManyToOne SlotCategory
    
    @Column(name = "kcal_per_100g", precision = 6, scale = 2, nullable = false)
    private BigDecimal kcalPer100g;
    
    @Column(name = "protein_per_100g", precision = 5, scale = 2, nullable = false)
    private BigDecimal proteinPer100g;
    
    // ... cac field khac
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    // audit fields
}
```

**Note quan trọng — KHÔNG dùng @ManyToOne:**
- Lưu plain `foodGroupCode` String, KHÔNG dùng `@ManyToOne FoodGroup foodGroup`
- Khi cần label của food_group, query thủ công ở service:

```java
public DishResponse toResponse(Dish dish) {
    FoodGroup foodGroup = foodGroupRepository.findById(dish.getFoodGroupCode())
        .orElse(null);
    return DishResponse.builder()
        .id(dish.getId())
        .name(dish.getName())
        .foodGroupCode(dish.getFoodGroupCode())
        .foodGroupLabel(foodGroup != null ? foodGroup.getLabel() : null)
        .build();
}
```

Hoặc dùng JOIN trong query khi lấy list nhiều dishes:
```java
@Query("""
    SELECT new com.example.DishWithLabels(d, fg.label, sc.label)
    FROM Dish d
    LEFT JOIN FoodGroup fg ON fg.code = d.foodGroupCode
    LEFT JOIN SlotCategory sc ON sc.code = d.slotCode
    WHERE d.isActive = true
""")
List<DishWithLabels> findAllActiveWithLabels();
```

---

## 8. INDEX STRATEGY

### 8.1. Đã add trong DDL Section 3

Tóm tắt các index quan trọng:

| Index | Bảng | Cột | Lý do |
|---|---|---|---|
| `idx_dishes_food_group` | dishes | food_group_code | Filter theo food_group ở admin |
| `idx_dishes_slot` | dishes | slot_code | Filter theo slot trong thuật toán đề xuất |
| `idx_dishes_active` | dishes | is_active | Query `WHERE is_active = TRUE` |
| `idx_dishes_name` | dishes | name | Search by tên |
| `idx_ingredients_group` | ingredients | group_code | Filter |
| `idx_ingredients_active` | ingredients | is_active | Query active |
| `idx_ingredients_name` | ingredients | name | Search |

### 8.2. Index không cần

- Bảng config (goal_configs, meal_ratio_configs, ...): rows ít (3-8 rows), không cần index
- `system_config`: query toàn bộ rows trong 1 lần, không cần index theo key (PK đã có sẵn)
- Bảng lookup (food_groups, slot_categories): rows ít, PK đã đủ

### 8.3. Composite index (chưa add — cân nhắc khi cần)

Trong tương lai nếu thuật toán đề xuất chậm, có thể cân nhắc:
```sql
CREATE INDEX idx_dishes_active_slot_kcal 
    ON dishes(is_active, slot_code, kcal_per_100g);
```
Composite này phục vụ query: `WHERE is_active = TRUE AND slot_code = ? AND kcal_per_100g BETWEEN ? AND ?`. Hiện tại với < 500 món, full scan vẫn nhanh. KHÔNG add ngay để tránh over-engineering.

---

## 9. MIGRATION PLAN

### 9.1. Thứ tự tạo bảng cho GIAI ĐOẠN HIỆN TẠI

**Chỉ tạo 6 bảng config, không có FK giữa chúng → có thể tạo song song bất kỳ thứ tự:**

1. `goal_configs`
2. `meal_ratio_configs`
3. `penalty_configs`
4. `slot_configs`
5. `surplus_penalty_configs`
6. `system_config`

**Sau đó chạy seed data** cho từng bảng (3 + 8 + 6 + 4 + 4 + 13 = 38 rows).

### ~~9.1.1. Thứ tự tạo cho phase sau~~ — DEFERRED

Khi implement thuật toán đề xuất (sau này):

**Phase A — Lookup tables (không có FK):**
1. `food_groups`
2. `slot_categories`

**Phase B — Entity tables (có FK tới Phase A):**
3. `dishes` (FK tới food_groups, slot_categories)
4. `ingredients` (FK tới food_groups)

### 9.2. Approach migration

**MVP — Drop & recreate (như Phase 2 user-service refactor):**
- Vì chưa deploy production, không cần lo data loss
- Dùng `spring.jpa.hibernate.ddl-auto=create` lần đầu, sau đó chuyển sang `validate`
- Seed data chạy bằng SQL script `data.sql` trong `resources/`

```yaml
# application.yml cho nutrition-service
spring:
  jpa:
    hibernate:
      ddl-auto: validate    # khong tu dong tao/sua bang sau khi schema da on dinh
  sql:
    init:
      mode: always          # luon chay schema.sql + data.sql
      data-locations: classpath:db/data.sql
```

**v2.0 — Flyway/Liquibase:** Khi đi vào production, switch sang Flyway để track migration changes. Hiện tại bỏ qua.

### 9.3. File structure

```
nutrition-service/src/main/resources/db/
├── schema.sql       (DDL toan bo bang)
├── data.sql         (seed data: 12 food_groups, 5 slot_categories, 3 goal_configs, ...)
└── README.md        (huong dan setup DB lan dau)
```

### 9.4. Khi cần thêm field mới

Vì chưa dùng Flyway, mỗi lần thay đổi schema:
1. Sửa entity Java
2. Sửa file `schema.sql` (nếu admin cần re-create)
3. Test ở local: `ddl-auto=update` → Hibernate tự ALTER TABLE (chỉ cho dev)
4. Khi push lên môi trường khác: drop & recreate (chấp nhận mất data dev)

---

## 10. KẾT NỐI VỚI USER-SERVICE VÀ HEALTH-DATA-SERVICE

### 10.1. Cách lấy thông tin user

Khi admin xem chi tiết user (API 5.2), nutrition-service KHÔNG có thông tin user. Có 2 cách (đã chốt Cách B ở API contract Section 5.2):

**Cách B — Frontend gọi 2 endpoint song song:**
```typescript
const [userInfo, healthMetrics] = await Promise.all([
  apiClient.get('/api/admin/users/' + userId),       // user-service
  apiClient.get('/api/health-data/users/' + userId + '/latest')  // health-data-service
]);
```

→ nutrition-service **không cần** gọi sang user-service hoặc health-data-service. Đơn giản nhất.

### 10.2. Cách validate user_id tồn tại

Trong nutrition-service, các bảng tương lai (như `meal_log`, `user_favorites`) sẽ có cột `user_id`. Câu hỏi: validate user_id có thực sự tồn tại trong user_db không?

**MVP:** KHÔNG validate. Trust user_id từ JWT (đã verify ở Gateway). Lý do:
- User chỉ có thể tạo data cho chính mình (lấy user_id từ header `userId`)
- Admin tạo data tay (qua admin panel) thì hiếm
- Validate cross-service tốn thời gian + làm phức tạp code

**v2.0:** Có thể thêm Feign client tới user-service nếu cần.

---

## 11. SỐ LIỆU TỔNG

| Loại bảng | Số bảng | Số rows |
|---|---|---|
| Lookup | 2 | 17 (12 + 5) |
| Entity (CRUD) | 2 | 100-1000+ (mục tiêu seed 100 dishes) |
| Config typed | 5 | 25 (3+8+6+4+4) |
| Config key-value | 1 | 13 keys |
| **Tổng** | **10** | **~150 rows seed + dynamic dishes/ingredients** |

So sánh với approach EAV bạn đề xuất ban đầu (2 bảng `option_set` + `option_set_value`, ~50-80 rows):
- EAV: 2 bảng, 50-80 rows, code phức tạp
- Hybrid: 10 bảng, 150 rows, code đơn giản

→ Nhiều bảng hơn nhưng code maintainability tốt hơn nhiều.

---

## 12. CHECKLIST IMPLEMENT

> ⚠️ **Checklist này phản ánh SCOPE THỰC TẾ của giai đoạn hiện tại** (xem Section 0.0).

### Giai đoạn 1: Setup
- [ ] Verify database `nutrition_db` đã tồn tại trong MySQL container (đã có)
- [ ] Cập nhật `application.yml` của nutrition-service trỏ tới `nutrition_db`
- [ ] Cấu hình `spring.jpa.hibernate.ddl-auto=update` cho dev (cẩn thận: chỉ ALTER ADD, không động bảng cũ)
- [ ] Test: start service → log không có error, kết nối DB OK

### Giai đoạn 2: Config tables (đơn giản trước)
- [ ] Entity `GoalConfig` (single-column PK String)
- [ ] Entity `SurplusPenaltyConfig` (single-column PK)
- [ ] Entity `SlotConfig` (single-column PK)
- [ ] Entity `MealRatioConfig` + `MealRatioConfigId` (composite PK)
- [ ] Entity `PenaltyConfig` + `PenaltyConfigId` (composite PK)
- [ ] Entity `SystemConfig` (single-column PK String)
- [ ] Repositories cho 6 entity
- [ ] Seed data trong `resources/db/data.sql`
- [ ] Test: start service → check 6 bảng tạo + seed data có (38 rows tổng)

### Giai đoạn 3: Service layer
- [ ] 5 service config (`GoalConfigService`, `MealConfigService`, `PenaltyConfigService`, `ScoringConfigService`, `SystemConfigService`)
- [ ] Validation logic ở service (tổng macro=1.00, tổng meal ratio=1.00, etc.)

### Giai đoạn 4: Controller + DTOs
- [ ] DTO request/response cho 5 nhóm config
- [ ] `AdminConfigController` với 11 endpoints
- [ ] Test với Postman / REST Client

### ~~Giai đoạn 5, 6~~ — DEFERRED
- ~~Lookup tables (food_groups, slot_categories)~~ → tạo sau
- ~~Entity tables (dishes, ingredients) refactor~~ → sau khi xong thuật toán đề xuất

---

## 13. TÓM TẮT QUYẾT ĐỊNH KIẾN TRÚC ĐÃ CHỐT

| Quyết định | Lý do |
|---|---|
| Hybrid schema (typed + key-value) | Type safety + DB validation cho config phức tạp, key-value cho scalar đơn lẻ |
| KHÔNG có FK cross-service | Microservice principle — mỗi service tự quản DB |
| FK trong cùng DB nhưng tối thiểu | Chỉ tạo cho lookup→entity (food_groups, slot_categories) |
| KHÔNG dùng @ManyToOne | Tránh N+1, LazyInitException; query thủ công khi cần |
| `BigDecimal` cho mọi DECIMAL | Tránh floating point error trên ratio/macro |
| UUID String PK cho entity (dish, ingredient) | Đồng bộ với Phase 2 refactor user-service |
| Code String PK cho config/lookup | Code chính là identifier tự nhiên, ít rows |
| Soft delete (`is_active`) cho dishes | Tránh xóa nhầm khi dish đang được dùng trong lịch sử bữa ăn |
| Hard delete cho ingredients | Ingredients không liên kết entity nào trong MVP |
| Drop & recreate migration | MVP, không có production data |
| KHÔNG cache config | < 100 user, đủ nhanh |
| KHÔNG validate user_id cross-service | Trust JWT đã verify ở Gateway |

---

## LỊCH SỬ THAY ĐỔI

| Phiên bản | Ngày | Thay đổi |
|---|---|---|
| v1.0 | 30/04/2026 | Tài liệu ban đầu, 10 bảng, hybrid schema |
| v1.1 | 30/04/2026 | Thêm scope notice — chỉ tạo 6 bảng config trong giai đoạn này, defer food_groups/slot_categories/dishes/ingredients đến sau khi xong thuật toán. Schema cũ trong nutrition_db giữ nguyên. |
