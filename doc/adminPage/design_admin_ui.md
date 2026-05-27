# THIẾT KẾ GIAO DIỆN ADMIN PANEL — DESIGN SPEC

> Tài liệu chi tiết để Claude Code và developer thực thi UI cho admin panel của HealthCare app.
> Dùng làm context khi viết code React/TypeScript cho frontend.
> Phiên bản: v1.0
> Ngày: 30/04/2026

---

## 0. SCOPE VÀ NGUYÊN TẮC

### 0.1. Phạm vi tài liệu

Tài liệu này mô tả **layout, components, và behavior** của admin panel. KHÔNG mô tả:
- DB schema (xem `db_schema_admin.md`)
- API contract (xem `api_contract_admin.md`)
- Business logic của thuật toán đề xuất thực đơn (xem `nghiep_vu_de_xuat_thuc_don_v3.md`)

### 0.1.1. ⚠️ SCOPE THỰC TẾ CHO GIAI ĐOẠN HIỆN TẠI (v1.1)

Tài liệu này mô tả 9 trang admin đầy đủ, nhưng **scope thực tế của giai đoạn hiện tại CHỈ làm 6 trang**:

**LÀM TRONG GIAI ĐOẠN NÀY:**
- Section 4: Cấu hình mục tiêu (`/admin/configs/goals`)
- Section 5: Cấu hình bữa ăn (`/admin/configs/meals`)
- Section 6: Cấu hình Penalty (`/admin/configs/penalties`)
- Section 7: Cấu hình Scoring (`/admin/configs/scoring`)
- Section 8: Cấu hình Hệ thống (`/admin/configs/system`)
- Section 11: Danh sách người dùng (`/admin/users`)
- Section 12: Chi tiết người dùng (`/admin/users/:id`)

**DEFERRED (làm sau khi implement xong thuật toán đề xuất thực đơn):**
- Section 3: Dashboard tổng quan (`/admin`) — cần aggregate data từ dishes, defer
- Section 9: Quản lý món ăn (`/admin/dishes`) — schema dishes cũ chưa khớp spec v3, defer
- Section 10: Quản lý nguyên liệu (`/admin/ingredients`) — tương tự, defer

**Lý do defer:** schema cũ của `dishes` và `ingredients` (đã tạo trước khi có spec v3) chưa khớp với yêu cầu thuật toán đề xuất. Sẽ refactor schema này khi implement thuật toán, sau đó mới quay lại làm UI quản lý cho khớp.

### 0.2. Nguyên tắc thiết kế

1. **Style đồng nhất với user view** — dùng tone xanh lá HealthCare (`brand-green`), font giống user view, không tạo theme riêng
2. **Banner cảnh báo admin mode** — thêm badge `[Admin]` màu cam ở header để admin biết đang ở chế độ quyền cao
3. **Tối thiểu hóa phức tạp** — MVP chỉ làm những gì cần thiết, không bulk edit, không audit log
4. **Form validation rõ ràng** — mỗi config có constraint (tổng = 1.00, range hợp lệ) phải hiển thị inline
5. **Helper text dày đặc** — admin không phải dinh dưỡng gia, cần context cho mỗi tham số
6. **Tái sử dụng component** — table, modal, form section nên là component riêng, dùng lại giữa các trang

### 0.3. Tech stack (đã có sẵn trong project)

- React 18+ với TypeScript
- React Router v6 cho routing
- Tailwind CSS với custom config `brand-green`
- Recharts cho biểu đồ (đã có)
- Zod cho schema validation (đã có)
- Axios với `apiClient` interceptor (đã có sau Phase auth)

---

## 1. KIẾN TRÚC ROUTING

### 1.1. Sitemap

```
/admin                              (Dashboard - tổng quan)
│
├── /admin/configs/goals            (Cấu hình theo mục tiêu - 3 tab)
├── /admin/configs/meals            (Cấu hình bữa ăn - 2 tab)
├── /admin/configs/penalties        (Cấu hình penalty)
├── /admin/configs/scoring          (Cấu hình scoring)
├── /admin/configs/system           (Cấu hình hệ thống)
│
├── /admin/dishes                   (Danh sách món ăn)
├── /admin/ingredients              (Danh sách nguyên liệu)
│
├── /admin/users                    (Danh sách user)
└── /admin/users/:id                (Chi tiết user - read-only)
```

### 1.2. Route protection

Mọi route `/admin/*` phải được bọc bằng `<AdminRoute>` component (đã có ở `src/components/common/AdminRoute.tsx`):
- Check `isAuthenticated` → nếu false redirect `/login`
- Check `user.roles.includes('ROLE_ADMIN')` → nếu false redirect `/dashboard`

### 1.3. Layout wrapper

Mọi trang admin dùng layout `AdminLayout` chứa Header + Sidebar + ContentArea. Component này tách riêng với `MainLayout` của user view để dễ maintain.

---

## 2. LAYOUT CHUNG

### 2.1. Cấu trúc tổng thể

```
┌──────────────────────────────────────────────────────────────────┐
│ Header (height: 64px, sticky top)                                │
├────────────┬─────────────────────────────────────────────────────┤
│            │                                                     │
│ Sidebar    │  Content Area                                       │
│ (width:    │  (padding: 24px, max-width: 1200px, center)         │
│  240px)    │                                                     │
│            │                                                     │
│            │                                                     │
└────────────┴─────────────────────────────────────────────────────┘
```

### 2.2. Header

**Component:** `AdminHeader.tsx`

**Cấu trúc bên trong (left → right):**

```
[🏥 HealthCare] [Search bar (optional)]    [🟠 Admin] [🔔] [👤 username ▾]
```

**Specs:**
- Background: white
- Border bottom: 1px solid gray-200
- Logo "HealthCare" bên trái: text màu `brand-green`, font-bold, size lg
- **Badge "Admin"**: text "Admin" trong pill nền màu orange-100, text orange-700, padding 8px x 4px, rounded-full
- Avatar dropdown (right):
  - Hover hiển thị menu: "Hồ sơ" / "Chuyển sang user view" / "Đăng xuất"
  - Click "Chuyển sang user view" → navigate `/dashboard`
  - Click "Đăng xuất" → gọi `logout()` của AuthContext

### 2.3. Sidebar

**Component:** `AdminSidebar.tsx`

**Cấu trúc:**

```
🏥 HealthCare
─────────────
📊 Tổng quan          → /admin

⚙️ Cấu hình ▾         (group header, collapse được)
   Mục tiêu           → /admin/configs/goals
   Bữa ăn             → /admin/configs/meals
   Penalty            → /admin/configs/penalties
   Scoring            → /admin/configs/scoring
   Hệ thống           → /admin/configs/system

🍜 Món ăn             → /admin/dishes
🥬 Nguyên liệu        → /admin/ingredients

👥 Người dùng         → /admin/users

─────────────
🔄 User view          → /dashboard
🚪 Đăng xuất
```

**Specs:**
- Background: white
- Border right: 1px solid gray-200
- Width: 240px (fixed)
- Mỗi item: padding 12px x 8px, hover: bg-gray-50, active: bg-`brand-green`-50 + text-`brand-green` + border-left 3px `brand-green`
- Group "Cấu hình" có icon ▾ ở phải, click toggle collapse. Mặc định expand
- Sub-items của group: indent 32px
- Section dưới (User view, Đăng xuất): cách section trên bằng 1 đường kẻ

### 2.4. Content Area

- Padding: 24px
- Max-width: 1200px, center horizontally
- Background: gray-50 (để cards bên trong nổi bật)
- Mỗi trang nên có Page Title ở đầu (font-bold, size 2xl) + Page Description (text-gray-500, size sm)

---

## 3. PAGE: DASHBOARD (`/admin`)

> ⚠️ **DEFERRED — KHÔNG làm trong giai đoạn hiện tại.**
> Dashboard cần aggregate data từ dishes/ingredients/users. Vì dishes/ingredients chưa refactor theo spec v3, dashboard sẽ thiếu data → defer cho đến khi xong thuật toán đề xuất thực đơn.

### 3.1. Mục đích

Cho admin nhìn nhanh tình trạng hệ thống: số lượng user/dish/meal, biểu đồ tăng trưởng user, cảnh báo (CSDL món ăn ít, user thiếu profile...).

### 3.2. Layout

```
Page Title: "Tổng quan hệ thống"
Page Description: "Hôm nay: <day-of-week>, <date>"

Section 1: Stat Cards (grid 4 columns trên desktop, 2 trên tablet, 1 trên mobile)
- Card 1: Tổng số người dùng + delta (↑ N mới hôm nay)
- Card 2: Số món ăn trong DB + warning nếu < 100
- Card 3: Số bữa đề xuất + delta hôm nay
- Card 4: Số mục tiêu cấu hình (luôn = 3)

Section 2: Biểu đồ "Người dùng mới 7 ngày qua"
- Line chart (Recharts), x = date, y = số user mới
- Padding card 16px, background white, rounded-lg, shadow-sm

Section 3: Cảnh báo (list)
- Mỗi cảnh báo là 1 row với icon ⚠ + text + (optional) link "Xem chi tiết"
- Ví dụ:
  - "CSDL món ăn: chỉ có 87 món, dưới ngưỡng 100" → link tới /admin/dishes
  - "5 user chưa cập nhật profile" → link tới /admin/users với filter
```

### 3.3. Components

- `StatCard`: props {title, value, delta?, deltaType?: 'up' | 'down' | 'warning', icon?}
- `LineChartUserGrowth`: props {data: {date, count}[]}
- `WarningList`: props {warnings: {icon, message, linkTo?}[]}

### 3.4. API endpoints cần (sẽ define ở tài liệu API)

- `GET /api/admin/dashboard/stats` → trả về 4 số stat
- `GET /api/admin/dashboard/user-growth?days=7` → trả về data biểu đồ
- `GET /api/admin/dashboard/warnings` → trả về list cảnh báo

---

## 4. PAGE: CẤU HÌNH MỤC TIÊU (`/admin/configs/goals`)

### 4.1. Mục đích

Quản lý 3 row trong bảng `goal_config` (GIAM, DUY_TRI, TANG). Mỗi row có 11 fields được nhóm thành 4 sections.

### 4.2. Layout

```
Page Title: "Cấu hình theo mục tiêu"
Page Description: "Các tham số dùng cho thuật toán đề xuất thực đơn theo từng mục tiêu"

Tabs (horizontal, 3 tab):
[ Giảm cân ] [ Duy trì ] [ Tăng cân ]
Active tab có border-bottom 2px solid brand-green + text bold

Form (4 section cards xếp dọc):

Section 1: 🔥 Hệ số deficit/surplus
- Field: Hệ số nhân TDEE (number input, step 0.05)
- Helper text: 
  "Calo mục tiêu/ngày = TDEE × hệ số này
   • 0.80 = deficit 20% (giảm cân an toàn ~0.5kg/tuần)
   • Khuyến nghị: 0.75 - 0.85 cho giảm cân
   • Không nên < 0.70 vì có thể gây thiếu dinh dưỡng"
- Validation: 
  - Required, min 0.50, max 1.50
  - Warning (không chặn) nếu giảm cân < 0.70 hoặc tăng cân > 1.30

Section 2: 🥩 Tỷ lệ Macro
- 3 fields trên 1 hàng: Protein / Fat / Carb (number, step 0.05)
- Below: "Tổng: <X.XX>" với icon ✓ nếu = 1.00, ✗ nếu khác
- Helper text:
  "Phân bổ % calo mục tiêu cho 3 nhóm dinh dưỡng
   • Giảm cân: protein cao (giữ cơ), fat trung bình
   • 1g protein/carb = 4 kcal, 1g fat = 9 kcal"
- Validation: tổng phải = 1.00 (precision 2 decimal)

Section 3: 🍽 Phân bổ slot
- 3 fields: Món chính / Rau-phụ / Tinh bột
- Below: "Tổng: <X.XX>" với check tổng = 1.00
- Helper text similar

Section 4: ⚖️ Trọng số scoring thực đơn
- 4 fields trên 2 hàng: Weight P / Weight F / Weight C / Weight Kcal
- Below: "Tổng: <X.XX>" với check tổng = 1.00
- Helper text similar

Footer:
- Left: "ℹ️ Cập nhật cuối: <datetime> bởi <username>"
- Right: [Hủy] [Lưu thay đổi]
- "Lưu thay đổi" disable nếu form invalid hoặc chưa thay đổi (form pristine)
```

### 4.3. Behavior

**Khi click tab khác mà form đang dirty:**
- Hiển thị confirm dialog: "Bạn có thay đổi chưa lưu. Tiếp tục chuyển tab?"
- OK → switch tab + reset form về giá trị từ DB của tab mới
- Cancel → giữ tab hiện tại

**Khi click "Hủy":**
- Reset form về giá trị từ DB
- Form về pristine state

**Khi click "Lưu thay đổi":**
- Validate full form
- Nếu invalid: hiển thị error inline + scroll tới field đầu tiên lỗi
- Nếu valid: PUT API → success toast "Đã lưu" → cập nhật `updated_at`/`updated_by` ở footer
- Nếu API fail: error toast với message từ DataResponse

**Validation đặc biệt:**
- Tổng macro ≠ 1.00 → border đỏ ở 3 fields + error message dưới section
- `cal_multiplier` < 0.70 cho mục tiêu Giảm cân → confirm dialog "Hệ số 0.65 tạo deficit 35% — quá lớn. Bạn có chắc?"

### 4.4. Components

- `TabBar`: props {tabs: {key, label}[], activeKey, onChange}
- `FormSection`: props {icon, title, helperText?, children}
- `NumericInput`: props {value, onChange, min, max, step, placeholder, error?}
- `SumIndicator`: props {value, expected, label}
  - Hiển thị "Tổng: <value>" + ✓ nếu = expected, ✗ nếu khác

### 4.5. State management

- 1 state object cho toàn bộ form: `goalConfigs: Record<GoalCode, GoalConfig>`
- Khi load: GET `/api/admin/configs/goals` → fill state
- Khi user gõ: update state cục bộ, mark form dirty
- Khi Lưu: PUT `/api/admin/configs/goals/{goalCode}` với data của tab hiện tại

---

## 5. PAGE: CẤU HÌNH BỮA ĂN (`/admin/configs/meals`)

### 5.1. Mục đích

Quản lý bảng `meal_ratio_config` — phần trăm kcal cho mỗi bữa, theo plan 3 bữa hoặc 5 bữa.

### 5.2. Layout

```
Page Title: "Tỷ lệ phân bổ kcal theo bữa"
Page Description: "Phần trăm tổng calo mục tiêu cho mỗi bữa trong ngày"

Tabs (horizontal, 2 tab):
[ 3 bữa ] [ 5 bữa ]

Form (1 section card):

Tab "3 bữa":
🌅 Sáng        [ 25 ]%
🌞 Trưa        [ 40 ]%
🌙 Tối         [ 35 ]%
─────────
Tổng: 100% ✓

Tab "5 bữa":
🌅 Sáng        [ 20 ]%
🍎 Phụ sáng    [ 10 ]%
🌞 Trưa        [ 30 ]%
🍪 Phụ chiều   [ 10 ]%
🌙 Tối         [ 30 ]%
─────────
Tổng: 100% ✓

Footer giống trang Goals
```

### 5.3. Behavior

- Input là %, lưu DB là decimal (25% → 0.25)
- Validation: tổng = 100% (cho phép sai số ±0.01% do floating point)
- Click tab khác mà dirty → confirm tương tự trang Goals

---

## 6. PAGE: CẤU HÌNH PENALTY (`/admin/configs/penalties`)

### 6.1. Mục đích

Quản lý bảng `penalty_config` (Layer 1 + Layer 2), `slot_config` (slot factors + min/max gram đã ở Section khác), và scalar configs liên quan penalty (cap, fav_discount, lookback_days).

### 6.2. Layout

```
Page Title: "Cấu hình Penalty"
Page Description: "Tham số phạt khi đề xuất món trùng lặp với lịch sử"

Section 1: 🔁 Layer 1 — Trùng món chính xác (cùng dish_id)
- 3 fields:
  - Cùng ngày (bữa khác): integer input
  - 1 ngày trước: integer input
  - 2 ngày trước: integer input
- Helper: "Phạt khi tổ hợp đề xuất chứa món đã ăn gần đây..."

Section 2: 🍱 Layer 2 — Trùng nhóm thực phẩm
- 3 fields tương tự
- Helper: "Phạt khi món khác nhưng cùng food_group..."

Section 3: 📊 Hệ số theo slot
- 4 fields trên 2 hàng:
  - Món chính / Rau phụ / Tinh bột / Combo
  - Decimal input, step 0.1
- Helper

Section 4: ⚙️ Tham số khác
- Penalty cap (integer)
- Hệ số món yêu thích (decimal 0-1)
- Lookback ngày (integer)
- Mỗi field 1 dòng với label, input, và inline helper text

Footer giống các trang khác
```

### 6.3. Validation

- Penalty values: integer >= 0
- Slot factors: decimal 0.0 - 1.0
- Penalty cap: integer >= 10
- Fav discount: decimal 0.0 - 1.0
- Lookback days: integer 1 - 7

### 6.4. State

State object phẳng `penaltyConfig: { layer1: {...}, layer2: {...}, slotFactors: {...}, others: {...} }`. Save bằng 1 API endpoint duy nhất `PUT /api/admin/configs/penalties` với toàn bộ data.

---

## 7. PAGE: CẤU HÌNH SCORING (`/admin/configs/scoring`)

### 7.1. Mục đích

Quản lý các tham số chấm điểm KHÔNG phụ thuộc goal (threshold, surplus penalty factors, re-optimize thresholds).

### 7.2. Layout

```
Page Title: "Cấu hình Scoring"
Page Description: "Tham số chấm điểm tổ hợp món ăn theo độ lệch macro/kcal"

Section 1: 📐 Threshold deviation
- 1 field: Threshold (decimal, step 0.01)
- Helper text dài giải thích công thức

Section 2: ⚠️ Hệ số phạt khi DƯ macro/kcal
- 4 fields trên 2 hàng: Protein / Fat / Carb / Kcal
- Decimal 0.0 - 1.0

Section 3: 🎯 Re-optimize gợi ý
- 2 fields:
  - Score threshold (integer)
  - Score drop (integer)

Footer
```

### 7.3. Validation

- Threshold: decimal 0.05 - 0.50
- Surplus factors: decimal 0.0 - 1.0
- Re-opt scores: integer >= 0

---

## 8. PAGE: CẤU HÌNH HỆ THỐNG (`/admin/configs/system`)

### 8.1. Mục đích

Các scalar configs còn lại của thuật toán đề xuất.

### 8.2. Layout

```
Page Title: "Cấu hình hệ thống"
Page Description: "Các tham số đơn lẻ điều khiển hành vi thuật toán"

Section 1: 🔍 Filter ứng viên
- Kcal tolerance (decimal 0.05 - 0.30)
- Serving min (decimal 0.25 - 1.0)
- Serving max (decimal 1.0 - 3.0)
- Serving steps (text input - comma-separated decimals, vd "0.5, 0.75, 1.0...")
- Serving steps cho COMBO (text input tương tự)

Section 2: 📦 Constraint khối lượng
- Bảng 4 hàng x 3 cột:
  - Slot | Min (g) | Max (g)
  - Món chính / Rau-phụ / Tinh bột / Combo
- Mỗi cell Min/Max là input integer

Section 3: 🖥 Display
- Top K (integer 5-20)
- Round step (integer 5-50, gram)

Footer
```

### 8.3. Validation đặc biệt cho Serving steps

- Parse text → array của decimals
- Validate: 
  - Mỗi giá trị trong khoảng [serving_min, serving_max]
  - Có giá trị 1.0 (default)
  - Sorted ascending
  - Không trùng
- Hiển thị error inline nếu sai format

---

## 9. PAGE: DANH SÁCH MÓN ĂN (`/admin/dishes`)

> ⚠️ **DEFERRED — KHÔNG làm trong giai đoạn hiện tại.**
> Schema bảng `dishes` cũ chưa khớp với spec v3 (thiếu `slot_code`, dùng `total_calories` thay vì `kcal_per_100g`, etc.). Sẽ refactor schema khi implement thuật toán đề xuất thực đơn, sau đó mới làm UI này.

### 9.1. Mục đích

CRUD bảng `dishes`. Hiển thị danh sách với search, filter, pagination. Sửa qua modal, xóa với confirm.

### 9.2. Layout

```
Page Title: "Quản lý món ăn"
Page Description: "Tổng: <N> món"

Right side of title: [ + Thêm món mới ] (button primary)

Toolbar:
[🔍 Tìm theo tên món...]                    Đã chọn: 0
Food group: [ Tất cả ▾ ]   Slot: [ Tất cả ▾ ]

Table:
┌────┬──────┬──────────────┬─────────┬───────┬──────┬──────┬────────┐
│ ☐  │ Ảnh  │ Tên món      │ Group   │ Slot  │ Kcal │ Đơn  │ Action │
│    │      │              │         │       │ /100g│ vị   │        │
├────┼──────┼──────────────┼─────────┼───────┼──────┼──────┼────────┤
│ ☐  │[40px]│ Phở bò       │ Combo   │ Combo │ 105  │ tô   │ ✏️ 🗑️ │
│ ...                                                              │
└────┴──────┴──────────────┴─────────┴───────┴──────┴──────┴────────┘

Footer:
[Xóa đã chọn (N)] (disabled khi N=0)    Trang 1/9 [< 1 2 3 ... 9 >]
```

### 9.3. Behavior

**Search bar:**
- Debounce 300ms khi user gõ
- Search by tên món (case-insensitive)
- Trigger refetch API với query param `?search=...`

**Filters:**
- Food group dropdown: lấy danh sách từ API (`GET /api/admin/dishes/food-groups`)
- Slot dropdown: hardcode 4 option (Tất cả / Món chính / Rau-phụ / Tinh bột / Combo)
- Khi đổi filter → trigger refetch

**Sorting:**
- Click vào header cột → sort asc/desc theo cột đó
- Default: sort theo `name` asc

**Pagination:**
- 10 dòng/trang
- Hiển thị "Trang X/Y" + nút prev/next + jump tới page

**Bulk delete:**
- Checkbox header toggle all current page
- Click "Xóa đã chọn (N)" → confirm dialog "Xóa N món đã chọn?"
- API: `DELETE /api/admin/dishes/bulk` body `{ ids: [...] }`

**Add new:**
- Click "+ Thêm món mới" → mở modal trống (giống modal sửa, nhưng tất cả field empty)

**Edit:**
- Click ✏️ → mở modal sửa với data prefilled

**Delete single:**
- Click 🗑️ → confirm "Xóa món <tên món>?"
- API: `DELETE /api/admin/dishes/{id}`

### 9.4. Modal sửa món

**Component:** `DishEditModal.tsx`

```
┌────────────────────────────────────────────┐
│  Sửa món ăn                            ✕   │
│  ID: <dish_id>                             │
├────────────────────────────────────────────┤
│                                            │
│  Tên món *                                 │
│  [ Phở bò                            ]     │
│                                            │
│  Ảnh                                       │
│  [📷 thumbnail 100×100]  [Đổi ảnh]         │
│                                            │
│  Mô tả                                     │
│  ┌──────────────────────────────────┐      │
│  │ ...                              │      │
│  └──────────────────────────────────┘      │
│                                            │
│  ─ Phân loại ───────────────                │
│  Food group *  [ Combo            ▾ ]      │
│  Slot *        [ Combo            ▾ ]      │
│                                            │
│  ─ Dinh dưỡng (per 100g) ───────             │
│  Kcal     [ 105.0  ]                       │
│  Protein  [ 6.5    ] g                     │
│  Fat      [ 2.3    ] g                     │
│  Carb     [ 15.8   ] g                     │
│                                            │
│  ─ Phục vụ ────────────────────             │
│  Đơn vị        [ tô              ]         │
│  Base serving  [ 350    ] g                │
│                                            │
│  ─────────────────────────                  │
│           [ Hủy ]  [ Lưu ]                 │
└────────────────────────────────────────────┘
```

**Specs:**
- Width: 600px (responsive: 90% on mobile)
- Max-height: 80vh, scroll trong modal nếu form dài
- Click overlay hoặc ✕ → confirm "Hủy thay đổi?" nếu form dirty
- "Lưu" disable nếu form invalid hoặc chưa thay đổi

**Field validation:**
- Tên món: required, max 100 chars, unique (validate ở backend)
- Ảnh: optional, file < 2MB, format jpg/png/webp
- Food group, Slot: required (dropdown)
- Kcal/Protein/Fat/Carb: required, decimal >= 0
- Base serving: required, integer >= 1, max 1000 (g)

**Upload ảnh:**
- POST multipart `/api/admin/dishes/upload-image` → trả URL relative
- Lưu URL vào `image_url` field

### 9.5. Components

- `DataTable<T>`: generic table component, props {columns, data, sortable?, selectable?, onSort, onSelect}
- `Toolbar`: props {searchValue, onSearchChange, filters: {label, value, options, onChange}[]}
- `Pagination`: props {currentPage, totalPages, onPageChange}
- `ConfirmDialog`: props {title, message, onConfirm, onCancel}
- `DishEditModal`: props {dish?: Dish, open: boolean, onClose, onSave}

---

## 10. PAGE: DANH SÁCH NGUYÊN LIỆU (`/admin/ingredients`)

> ⚠️ **DEFERRED — KHÔNG làm trong giai đoạn hiện tại.**
> Tương tự dishes, schema ingredients cũ chưa khớp spec v3. Defer.

### 10.1. Mục đích

CRUD bảng `ingredients`. Pattern giống dishes.

### 10.2. Layout

Y hệt trang dishes, chỉ khác:
- Cột table: Ảnh | Tên nguyên liệu | Nhóm | Kcal/100g | Protein | Fat | Carb | Action
- Filter: chỉ có 1 dropdown Group (không có Slot)
- Modal sửa nguyên liệu có ít field hơn (không có serving, không có slot)

### 10.3. Câu hỏi cần admin/dev cân nhắc

Quan hệ giữa `ingredient` và `dish` cần làm rõ ở DB schema. Có 2 cách:
- Cách A: dish có dinh dưỡng tự lưu (per 100g), không liên kết ingredient → ingredient chỉ để tham khảo
- Cách B: dish được tạo từ nhiều ingredient, dinh dưỡng tính tự động

MVP đề xuất Cách A — đơn giản, ít validation, đủ cho thuật toán đề xuất.

---

## 11. PAGE: DANH SÁCH NGƯỜI DÙNG (`/admin/users`)

### 11.1. Mục đích

Read-only list of users. Click 1 row → xem chi tiết. KHÔNG có sửa/xóa cho MVP.

### 11.2. Layout

```
Page Title: "Quản lý người dùng"
Page Description: "Tổng: <N> người dùng (<N_admin> admin, <N_user> user)"

Toolbar:
[🔍 Tìm theo username, email, tên...]
Role: [ Tất cả ▾ ]   Status: [ Tất cả ▾ ]

Table:
┌──────────┬──────────────┬──────────┬─────────┬──────────┬───────┐
│ Username │ Họ tên       │ Email    │ Role    │ Tạo lúc  │ Action│
├──────────┼──────────────┼──────────┼─────────┼──────────┼───────┤
│ admin    │ Administrator│ admin@.. │ ADMIN   │ 01/04    │ 👁    │
│ ...                                                              │
└──────────┴──────────────┴──────────┴─────────┴──────────┴───────┘

Footer: pagination
```

### 11.3. Behavior

- Search: theo username, email, hoặc full name
- Filter Role: ALL / ROLE_USER / ROLE_ADMIN
- Filter Status: ALL / ACTIVE / INACTIVE (nếu DB có field status)
- Click 👁 → navigate `/admin/users/:id`
- KHÔNG có ✏️ và 🗑️

---

## 12. PAGE: CHI TIẾT NGƯỜI DÙNG (`/admin/users/:id`)

### 12.1. Mục đích

Read-only view 3 nhóm thông tin: account / profile / health metrics gần nhất.

### 12.2. Layout

```
Breadcrumb: ← Quay về danh sách

Page Title: "Chi tiết người dùng: <username>"

Card 1: Thông tin tài khoản
- Username, Email, Role, Tạo lúc

Card 2: Hồ sơ cá nhân
- Họ tên, Ngày sinh (tuổi), Giới tính, SĐT

Card 3: Chỉ số sức khỏe gần nhất
- Cân nặng, Chiều cao
- BMI, BMR, TDEE, PBF, WHR
- Cập nhật cuối: <datetime>

(Optional cho MVP) Card 4: Lịch sử thực đơn (10 bữa gần nhất)
- Bảng nhỏ: ngày, bữa, các món, tổng kcal
```

### 12.3. Behavior

- Read-only hoàn toàn, không có form chỉnh sửa
- Nếu user thiếu profile (birthDate/gender null) → hiển thị "Chưa cập nhật" thay vì để trống
- Nếu user chưa có health metrics → Card 3 hiển thị "Chưa có dữ liệu sức khỏe"

---

## 13. SHARED COMPONENTS

Components dùng chung giữa nhiều trang admin. Đặt ở `src/components/admin/`.

### 13.1. AdminLayout

```tsx
interface AdminLayoutProps {
  children: React.ReactNode;
}
```

Wrap content với Header + Sidebar. Sticky header, scroll content area.

### 13.2. PageHeader

```tsx
interface PageHeaderProps {
  title: string;
  description?: string;
  rightAction?: React.ReactNode;
}
```

Render title + description + optional button bên phải.

### 13.3. FormSection

```tsx
interface FormSectionProps {
  icon?: string; // emoji
  title: string;
  helperText?: string | string[];
  children: React.ReactNode;
}
```

Card chứa 1 nhóm field cùng chủ đề.

### 13.4. NumericInput

```tsx
interface NumericInputProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  step?: number;
  unit?: string; // "%, g, kcal..." hiển thị bên phải
  error?: string;
  helperText?: string;
  disabled?: boolean;
}
```

Wrapper của input type="number" + label + error + helper.

### 13.5. SumIndicator

```tsx
interface SumIndicatorProps {
  values: number[];
  expected: number;
  label?: string;
  precision?: number; // default 2
}
```

Hiển thị "Tổng: <X.XX> ✓" hoặc "Tổng: <X.XX> ✗" + cảnh báo đỏ nếu sai.

### 13.6. DataTable

```tsx
interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  selectable?: boolean;
  selectedIds?: string[];
  onSelectionChange?: (ids: string[]) => void;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  onSort?: (key: string) => void;
  loading?: boolean;
  emptyMessage?: string;
}

interface Column<T> {
  key: string;
  header: string;
  render: (row: T) => React.ReactNode;
  sortable?: boolean;
  width?: string;
}
```

### 13.7. ConfirmDialog

```tsx
interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: string;
  confirmText?: string; // default "Xác nhận"
  cancelText?: string;  // default "Hủy"
  variant?: 'default' | 'danger'; // danger: nút màu đỏ
  onConfirm: () => void;
  onClose: () => void;
}
```

### 13.8. Toast notifications

Dùng library hiện có hoặc tự implement. Vị trí: bottom-right. Tự động đóng sau 3s.

```tsx
// Sử dụng trong code:
toast.success('Đã lưu thay đổi');
toast.error('Lỗi: ...');
toast.warning('...');
```

---

## 14. STYLE GUIDE

### 14.1. Color tokens (sử dụng class Tailwind có sẵn)

| Use case | Class |
|---|---|
| Primary action button | `bg-brand-green text-white` |
| Danger action (xóa) | `bg-red-500 text-white` |
| Secondary button | `bg-gray-100 text-gray-700` |
| Border | `border-gray-200` |
| Card background | `bg-white` |
| Page background | `bg-gray-50` |
| Admin badge | `bg-orange-100 text-orange-700` |
| Success | `text-green-600` |
| Warning | `text-yellow-600` |
| Error | `text-red-600` |

### 14.2. Typography

- Page title: `text-2xl font-bold`
- Page description: `text-sm text-gray-500`
- Section title: `text-lg font-semibold`
- Helper text: `text-xs text-gray-500`
- Field label: `text-sm font-medium text-gray-700`
- Error message: `text-xs text-red-600`

### 14.3. Spacing

- Card padding: `p-4` hoặc `p-6` (cho card lớn)
- Section gap: `space-y-4` (giữa các sections)
- Field gap: `space-y-3` (giữa các field trong section)
- Inline gap: `space-x-2` (giữa label và input)

### 14.4. Responsive breakpoints

- Mobile: < 640px → Sidebar collapse thành menu, content full width
- Tablet: 640-1024px → Sidebar narrow, content adjusted
- Desktop: > 1024px → Layout đầy đủ như mô tả

---

## 15. ERROR HANDLING

### 15.1. API errors

Mọi API trả về `DataResponse<T>` với format:
```typescript
interface DataResponse<T> {
  code: string | null;
  message: string;
  data: T;
}
```

Khi API fail (status 4xx/5xx):
- 401: `apiClient` interceptor tự refresh token, không cần handle ở component
- 403: Toast "Bạn không có quyền thực hiện thao tác này"
- 404: Toast "Không tìm thấy dữ liệu"
- 422 (validation): Hiển thị field errors từ `data` của response
- 500: Toast "Lỗi hệ thống. Vui lòng thử lại sau"

### 15.2. Loading states

- Trang đang load data lần đầu: hiển thị skeleton hoặc spinner full page
- Đang submit form: button chuyển sang loading state, disable
- Đang fetch table: overlay nhẹ trên table, không che hoàn toàn

### 15.3. Empty states

- Table không có data: hiển thị message + icon + (optional) CTA "Thêm mới"
- Search không có kết quả: "Không tìm thấy kết quả phù hợp"

---

## 16. ACCESSIBILITY (NICE-TO-HAVE)

Cho MVP, chỉ cần đảm bảo:
- Mọi input có label rõ ràng
- Button có text hoặc aria-label (icon button)
- Modal có focus trap (focus tự động vào field đầu khi mở, đóng khi escape)
- Color không phải kênh duy nhất truyền tải info (icon ✓/✗ kèm text)

---

## 17. CHECKLIST IMPLEMENT THỨ TỰ

> ⚠️ **Checklist này phản ánh SCOPE THỰC TẾ của giai đoạn hiện tại** (không bao gồm Dashboard, Dishes, Ingredients — xem Section 0.1.1).

Đề xuất thứ tự code để tránh phụ thuộc rối:

### Giai đoạn 1: Infrastructure
- [ ] Cài đặt routing structure `/admin/*`
- [ ] Tạo `AdminLayout`, `AdminHeader`, `AdminSidebar`
- [ ] Cập nhật `App.tsx` với các route admin và `AdminRoute` guard

### Giai đoạn 2: Shared components
- [ ] `PageHeader`, `FormSection`, `NumericInput`, `SumIndicator`
- [ ] `DataTable`, `Pagination`, `Toolbar`
- [ ] `ConfirmDialog`, toast notification

### Giai đoạn 3: Configs (đơn giản hơn CRUD)
- [ ] Goals config (3 tabs)
- [ ] Meals config (2 tabs)
- [ ] Penalties config
- [ ] Scoring config
- [ ] System config

### Giai đoạn 4: Users (read-only)
- [ ] Users list
- [ ] User detail page (read-only)

### Giai đoạn 5: Polish
- [ ] Loading states
- [ ] Error handling
- [ ] Toast notifications
- [ ] Responsive (nếu có thời gian)

### ~~Giai đoạn 6 và 7~~ — DEFERRED
- ~~Dashboard~~ → làm sau thuật toán đề xuất
- ~~Dishes CRUD~~ → làm sau khi refactor schema dishes
- ~~Ingredients CRUD~~ → làm sau khi refactor schema ingredients

---

## 18. REFERENCES

- `nghiep_vu_de_xuat_thuc_don_v3.md` — business logic của các tham số config
- `auth-system-documentation.md` — RBAC pattern, AdminRoute usage
- `design_admin_ui.md` (this file) — UI/UX spec

---

## LỊCH SỬ THAY ĐỔI

| Phiên bản | Ngày | Thay đổi |
|---|---|---|
| v1.0 | 30/04/2026 | Tài liệu ban đầu, gồm 9 trang admin chính |
| v1.1 | 30/04/2026 | Thêm scope notice — defer Dashboard, Dishes, Ingredients đến sau khi xong thuật toán đề xuất |
