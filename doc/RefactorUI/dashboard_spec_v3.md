# DASHBOARD SPECIFICATION — TRANG CHỦ SAU LOGIN

> **Mục đích tài liệu:** Mô tả đầy đủ "đầu vào/đầu ra" của Dashboard page để:
> 1. Claude Design render mockup chuẩn theo nghiệp vụ
> 2. Agent code implement React component đúng spec
> 3. Backend reference khi build endpoint còn thiếu
>
> **Scope:** Chỉ Dashboard (page `/health-stats` redesign). Page khác viết spec riêng khi đến lượt.
>
> **Tham chiếu:**
> - `nghiep_vu_de_xuat_thuc_don_v3.3.md` — spec nghiệp vụ chính
> - Repo BE: branch `feature-DeXuatThucDon`
> - Repo FE: master branch (chứa `apiClient`, components/admin reusable)

---

## 0. CONTEXT VÀ NGUYÊN TẮC

### 0.1. Mục đích Dashboard

Page user **thấy đầu tiên sau login**. Trả lời 3 câu hỏi cốt lõi của user:

1. **Tôi đang ở đâu?** → Widget Thể trạng hiện tại
2. **Tôi tiến bộ thế nào?** → Widget Thống kê lịch sử (weight chart + compliance + metrics)
3. **Tôi cần làm gì tiếp?** → Widget Nhắc nhở (3 loại priority với CTA)

Mỗi widget phải actionable hoặc informational — KHÔNG có widget chỉ để "trang trí".

### 0.2. Nguyên tắc thiết kế

- **Mobile-first.** Layout phải work tốt trên width 380px (mobile portrait).
- **Empty state ưu tiên.** User mới chưa có data → vẫn render được, không rỗng/lỗi.
- **Reliability over sophistication.** Logic tính toán phải dùng data có sẵn, không depend ML pipeline chưa wired.
- **Defensible.** Mọi chỉ số phải có nguồn academic/clinical, hội đồng hỏi defense được.
- **Reuse components.** Tận dụng `components/admin/*` (PageHeader, Card, NumericInput, etc.) đã có.

### 0.3. Brand & Style

- Primary: `brand-green` (#059669)
- Neutral: `brand-gray` (#6b7280)
- Background: `green-50` (#F0FDF4) cho hero, `white` cho card
- Font family: theo Tailwind config có sẵn của project
- Border radius: card = `rounded-2xl`, button = `rounded-lg`
- Shadow: card = `shadow-sm`, hover = `shadow-md`

---

## 1. WIDGET 1 — THỂ TRẠNG HIỆN TẠI

### 1.1. Mục đích nghiệp vụ

Hiển thị phân loại thể trạng (`GAY` / `CAN_DOI` / `THUA_CAN` / `BEO_PHI`) dựa trên BMI + PBF combined rule. Đây là chỉ số bản chất nhất user quan tâm để biết "tôi đang ở đâu".

Hỗ trợ 2 method tính PBF (user chọn qua setting):
- **FORMULA** (mặc định): Công thức Hải quân Mỹ, không cần ML
- **MODEL_1** (advanced): Dùng ML model predict PBF

Phân loại constitution:
- Nếu PBF từ FORMULA → dùng **rule-based** kết hợp BMI + PBF (worst case)
- Nếu PBF từ MODEL_1 → dùng **Model 2** classifier

### 1.2. Data nguồn (input)

> **Architecture decision v2:** Logic phân loại thể trạng đã chuyển sang BE. FE CHỈ gọi endpoint và render. Lý do: single source of truth, defensibility cho thesis, tránh duplicate logic giữa Dashboard / Recommendation / Onboarding.

#### Endpoint chính

**`GET /api/health-data/constitution`** — service: `health-data-service`

Status: ❌ **CẦN BUILD MỚI** (xem `backend_endpoint_spec.md` cho implementation chi tiết)

Response:
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
    "warning": null,
    "computedAt": "2026-05-25T14:30:00"
  }
}
```

Field detail:

| Field | Type | Mô tả |
|---|---|---|
| `constitution` | enum | `GAY` / `CAN_DOI` / `THUA_CAN` / `BEO_PHI` |
| `method` | string | `RULE_BMI_PBF` (FORMULA) hoặc `MODEL_2` (khi ML wired) |
| `bmi` | number | Compute từ height + weight |
| `pbf` | number / null | Compute từ Navy formula hoặc Model 1; null nếu thiếu số đo |
| `pbfSource` | string | `FORMULA` (Navy) hoặc `MODEL_1` (user preference từ user_preferences table) |
| `bmiClass` | int (0-3) | Phân loại theo BMI thuần |
| `pbfClass` | int (0-3) / null | Phân loại theo PBF; null nếu pbf null |
| `finalClass` | int (0-3) | `max(bmiClass, pbfClass)` hoặc bằng bmiClass nếu pbfClass null |
| `warning` | string / null | Vd: "Thiếu vòng eo/cổ — chỉ phân loại theo BMI" |
| `computedAt` | datetime | Timestamp |

#### Endpoint phụ (data bổ sung)

**`GET /api/health-data/dashboard-metrics`** (existing) — chỉ dùng cho `updatedAt` hiển thị "Cập nhật N ngày trước" (lấy field `bmi.recordedAt` hoặc tương đương).

**`GET /api/user-preferences`** (mới — xem `backend_endpoint_spec.md`) — đọc setting `pbfMethod` để show toggle "Đổi sang Model 1" nếu user đang dùng FORMULA.

#### Error states FE cần handle

| HTTP status | Body code | Behavior FE |
|---|---|---|
| 200 | null | Render normal |
| 422 | `MISSING_BASIC_DATA` | Empty state, CTA "Cập nhật cân nặng + chiều cao" |
| 422 | `MISSING_GENDER` | Empty state, CTA "Cập nhật profile" |
| 200 (có `warning`) | null | Incomplete state, badge "Cần thêm số đo" |
| 500 | any | Error state, button retry |

### 1.3. Compute logic

> **Logic compute thể trạng đã chuyển sang BE.** Xem chi tiết tại `backend_endpoint_spec.md` §2.
>
> FE **KHÔNG** implement compute logic. Chỉ gọi endpoint và render kết quả.

**Tóm tắt logic BE (để hiểu, không phải để code FE):**
- BMI: WHO Asian (cutoff: <18.5 GẦY, <23 CÂN_ĐỐI, <25 THỪA_CÂN, ≥25 BÉO_PHÌ)
- PBF Navy formula (Nam: waist/neck/height; Nữ: waist/hip/neck/height)
- PBF Pasco 2024 gender-specific cutoff
- Final class = max(bmiClass, pbfClass) — worst case principle (bắt Normal Weight Obesity)

#### Edge cases (BE đã handle, FE chỉ display)

| BE response state | FE behavior |
|---|---|
| `constitution=*`, `warning=null` | Render normal — show BMI + PBF |
| `constitution=*`, `warning="..."` | Render với badge warning bên dưới |
| 422 `MISSING_BASIC_DATA` | Empty state CTA "Cập nhật chỉ số" |
| 422 `MISSING_GENDER` | Empty state CTA "Cập nhật profile" |

### 1.4. Display output

#### Layout (desktop)

```
┌──────────────────────────────────────────┐
│ THỂ TRẠNG HIỆN TẠI                   ⓘ  │
│                                          │
│ 🎯  CÂN ĐỐI                              │
│     BMI 22.1 · PBF 15.4%                 │
│                                          │
│ ▓▓▓▓▓▓░░░░░░░░░░░░░ (BMI scale bar)     │
│ Cập nhật: 2 ngày trước                   │
└──────────────────────────────────────────┘
```

#### Color coding theo constitution

| Constitution | Color hex | Tailwind | Icon emoji |
|---|---|---|---|
| GAY | `#F59E0B` | `amber-500` | 🌱 hoặc icon thin person |
| CAN_DOI | `#059669` | `brand-green` | 🎯 hoặc check |
| THUA_CAN | `#EA580C` | `orange-600` | ⚠️ |
| BEO_PHI | `#DC2626` | `red-600` | ⚠️ red |

Constitution label (text):
- `GAY` → "GẦY"
- `CAN_DOI` → "CÂN ĐỐI"
- `THUA_CAN` → "THỪA CÂN"
- `BEO_PHI` → "BÉO PHÌ"

#### States

**LOADING:**
- Skeleton card với placeholder text mờ + animate-pulse

**EMPTY** (user chưa có cân nặng/chiều cao):
```
┌──────────────────────────────────────────┐
│ THỂ TRẠNG HIỆN TẠI                       │
│                                          │
│ 📋  Chưa có dữ liệu                      │
│ Cập nhật cân nặng + chiều cao để xem    │
│                                          │
│ [Cập nhật ngay →]                        │
└──────────────────────────────────────────┘
```
CTA → navigate `/health-data` (page Cập nhật chỉ số)

**INCOMPLETE** (có BMI nhưng không có PBF do thiếu số đo):
```
┌──────────────────────────────────────────┐
│ THỂ TRẠNG HIỆN TẠI                       │
│                                          │
│ 🎯  CÂN ĐỐI (dựa trên BMI)              │
│     BMI 22.1                             │
│                                          │
│ 💡 Bổ sung vòng eo/cổ để có kết quả     │
│    chính xác hơn   [Cập nhật →]         │
└──────────────────────────────────────────┘
```

**NORMAL** (có đủ data):
- Hiển thị như layout chính ở §1.4

**WARNING — Model 2 unavailable** (khi `pbfMethod=MODEL_1` mà ML chưa wired):
- Same layout normal nhưng có dải nhỏ phía dưới: "⚠ Model 1 dự đoán đang bảo trì, hiển thị tạm theo công thức"

#### BMI scale bar (subtle visualization)

Thanh ngang chia 4 vùng (GAY/CAN_DOI/THUA_CAN/BEO_PHI) với marker chỉ vị trí user. Hover marker tooltip "Bạn đang ở 22.1 (CÂN ĐỐI)".

Mục đích: trực quan, không phải decoration. User scan 0.5s là biết đang ở đâu trong scale.

### 1.5. Interaction

| Hành động | Behavior |
|---|---|
| Click icon `ⓘ` | Tooltip giải thích "Thể trạng dựa trên BMI và PBF kết hợp, lấy nhóm tệ hơn" + link "Đổi method tính PBF" |
| Click vào card body | Mở modal "Chi tiết thể trạng" với BMI scale bar full size + giải thích từng vùng |
| Click "Cập nhật ngay" (empty state) | Navigate `/health-data` |
| Click "Cập nhật →" (incomplete state) | Navigate `/health-data` với anchor scroll xuống vòng eo |

---

## 2. WIDGET 2 — THỐNG KÊ LỊCH SỬ (3 sub-widgets)

### 2.1. Sub-widget 2A — Biểu đồ cân nặng 30 ngày

#### Mục đích nghiệp vụ

User theo dõi tiến độ cân theo thời gian. Đây là chỉ số quan trọng nhất user check định kỳ.

#### Data nguồn

**Endpoint:** `GET /api/health-data/history?days=30&field=weight`
Status: ❓ cần verify với BE. Nếu chưa có → cần build endpoint trả về list `{date, weightKg}` 30 ngày gần nhất.

Response:
```json
{
  "data": [
    { "date": "2026-04-25", "weightKg": 64.5 },
    { "date": "2026-04-28", "weightKg": 64.3 },
    ...
  ]
}
```

#### Display

**Layout:**
```
┌──────────────────────────────────────────┐
│ CÂN NẶNG 30 NGÀY                         │
│                                          │
│   65.0 ─────────                         │
│        ●                                 │
│   64.5 ──●────●──                        │
│              ●  ●                        │
│   64.0 ─────────●●─                     │
│                                          │
│   63.5 ─────────────                     │
│        25/4    10/5   25/5               │
└──────────────────────────────────────────┘
```

Library suggestion: **Recharts** (đã có trong package.json FE) — dùng `LineChart` đơn giản.

Config:
- X axis: date, format "DD/M"
- Y axis: weightKg, auto-scale có padding ±0.5kg
- Line: brand-green, smooth curve, dot ở mỗi data point
- Tooltip: format "DD/MM/YYYY: XX.X kg"

#### States

**LOADING:** skeleton chart placeholder.

**EMPTY** (< 2 data points):
```
┌──────────────────────────────────────────┐
│ CÂN NẶNG 30 NGÀY                         │
│                                          │
│ 📊 Cần ít nhất 2 lần cân                 │
│                                          │
│ Cập nhật cân nặng để bắt đầu             │
│ theo dõi tiến độ                         │
│                                          │
│ [Cập nhật cân nặng →]                    │
└──────────────────────────────────────────┘
```

**NORMAL:** chart như layout.

#### Interaction

- Hover point → tooltip ngày + cân
- Click "Cập nhật cân nặng" → `/health-data`

---

### 2.2. Sub-widget 2B — Đã có thực đơn (7 ngày gần nhất)

#### Mục đích nghiệp vụ

User thấy mình đã active dùng app bao nhiêu ngày trong tuần. Tracking gián tiếp engagement.

**Lưu ý label:** Dùng "Đã có thực đơn" thay vì "Tuân thủ" vì MVP `meal_log.status` chỉ có `SUGGESTED`. Khi v2.0 có FOLLOWED/SKIPPED thực sự → đổi label sau.

#### Data nguồn

**Endpoint:** `GET /api/meal-log/history?days=7`
Status: ✅ đã có (verify với agent BE)

Compute ở FE:
```javascript
const expectedMeals = planType === '3_BUA' ? 3 : 5;
const totalExpected = 7 * expectedMeals;
const totalLogged = mealLogs.length;
const compliancePercent = (totalLogged / totalExpected) * 100;

// Group by date để render daily breakdown
const dailyBreakdown = groupByDate(mealLogs);
```

#### Display

**Layout:**
```
┌──────────────────────────────────────────┐
│ ĐÃ CÓ THỰC ĐƠN — 7 NGÀY GẦN NHẤT         │
│                                          │
│ ████████████░░░░░░░░  17/21 bữa (81%)    │
│                                          │
│  T2   T3   T4   T5   T6   T7   CN        │
│  ✓✓✓  ✓✓✓  ✓✓✗  ✓✓✓  ✓✓✓  ✓✗✗  ✓✓✓     │
│                                          │
└──────────────────────────────────────────┘
```

Mỗi cột là 1 ngày, mỗi ô là 1 bữa:
- `✓` (xanh): có meal_log record
- `✗` (xám): không có
- Số lượng ô = `planType` (3 hoặc 5)

#### States

**EMPTY** (chưa có meal_log nào):
```
┌──────────────────────────────────────────┐
│ ĐÃ CÓ THỰC ĐƠN — 7 NGÀY                 │
│                                          │
│ 🍽 Chưa có thực đơn nào                  │
│ Tạo thực đơn đầu tiên để bắt đầu        │
│                                          │
│ [Tạo thực đơn →]                         │
└──────────────────────────────────────────┘
```
CTA → navigate `/nutrition-plan`

#### Interaction

- Hover ô bữa → tooltip "Bữa Trưa T4: Gà luộc + Canh rau dền + Bánh phở"
- Click ô bữa có data → mở modal hoặc navigate `/nutrition-plan/history?date=...`
- Click ô bữa empty → CTA "Tạo cho bữa này"

---

### 2.3. Sub-widget 2C — Metric tổng kết (3 card nhỏ)

#### Mục đích

3 con số nhỏ tạo cảm giác app track đầy đủ, không tốn nhiều space.

#### Data nguồn

Tận dụng data đã load cho 2A và 2B:
- Cân tuần này = weightLatest − weight7DaysAgo
- Đề xuất tuần này = count meal_log trong 7 ngày
- Yêu thích = `GET /api/favorite-dishes` count

#### Display

```
┌───────────┐ ┌───────────┐ ┌───────────┐
│ 📉        │ │ 📊        │ │ ⭐        │
│ -0.5 kg   │ │ 17 bữa    │ │ 3 món     │
│ Tuần này  │ │ Tuần này  │ │ Yêu thích │
└───────────┘ └───────────┘ └───────────┘
```

#### States

- Cân tuần này = null nếu < 2 data point trong 7 ngày → hiển thị "—"
- Đề xuất tuần này = 0 nếu chưa có → hiển thị "0 bữa"
- Yêu thích = 0 nếu chưa có → hiển thị "0 món"

#### Interaction

- Click card "Cân tuần này" → scroll xuống widget 2A
- Click card "Đề xuất tuần này" → navigate `/nutrition-plan/history`
- Click card "Yêu thích" → navigate `/favorites` (chưa build, có thể link tới page chứa list favorite dishes)

---

## 3. WIDGET 3 — NHẮC NHỞ

### 3.1. Mục đích nghiệp vụ

Cung cấp 1-3 hành động tiếp theo user nên làm. Mỗi nhắc phải actionable (có CTA cụ thể).

### 3.2. Data nguồn

**Phương án A (MVP — khuyến nghị):** FE tự compute trigger từ data đã có (profile, health-data, meal-log).

**Phương án B (v2.0):** BE expose endpoint `GET /api/notifications/dashboard` trả về list nhắc đã được pre-computed.

→ Spec này dùng Phương án A.

### 3.3. Logic trigger 3 loại

#### Loại 1 — HIGH PRIORITY (Action cần thiết)

```javascript
const triggers = [];

// 1a. Profile thiếu năm sinh hoặc giới tính
if (!user.birthDate || !user.gender) {
    triggers.push({
        priority: 'HIGH',
        type: 'PROFILE_INCOMPLETE',
        title: 'Bổ sung thông tin profile',
        message: 'Cần năm sinh và giới tính để tính TDEE và PBF chính xác',
        ctaText: 'Cập nhật profile',
        ctaPath: '/profile',
        icon: '⚠'
    });
}

// 1b. Chưa cân trong 7 ngày
const lastWeightDate = healthData.weightUpdatedAt;
const daysSinceWeight = daysBetween(lastWeightDate, today);
if (daysSinceWeight > 7) {
    triggers.push({
        priority: 'HIGH',
        type: 'WEIGHT_OUTDATED',
        title: `Đã ${daysSinceWeight} ngày chưa cân`,
        message: 'Cập nhật để hệ thống đề xuất chính xác hơn',
        ctaText: 'Cập nhật cân nặng',
        ctaPath: '/health-data',
        icon: '⚖'
    });
}

// 1c. Chưa có thực đơn hôm nay (sau 6h sáng)
const now = new Date();
if (now.getHours() >= 6) {
    const todayMeals = mealLogs.filter(m => m.mealDate === todayISO);
    if (todayMeals.length === 0) {
        triggers.push({
            priority: 'HIGH',
            type: 'NO_MEAL_TODAY',
            title: 'Chưa có thực đơn hôm nay',
            message: 'Tạo thực đơn để bắt đầu ngày mới',
            ctaText: 'Tạo thực đơn',
            ctaPath: '/nutrition-plan',
            icon: '🍽'
        });
    }
}
```

#### Loại 2 — MEDIUM PRIORITY (Thông tin tiến độ)

```javascript
// Chỉ trigger khi có ≥ 7 ngày data cân
if (weightHistory.length >= 7) {
    const weightTrend = computeWeightTrend(weightHistory);  // kg/week
    const goal = currentGoal?.goalCode;  // GIAM | DUY_TRI | TANG (từ GET /api/user-goals/current)

    // 2a. Đang DUY_TRI mà cân giảm bất thường
    if (goal === 'DUY_TRI' && weightTrend < -0.7) {
        triggers.push({
            priority: 'MEDIUM',
            type: 'WEIGHT_DROPPING',
            title: 'Cân nặng đang giảm',
            message: `Đã giảm ${Math.abs(weightTrend).toFixed(1)}kg/tuần. Kiểm tra mục tiêu?`,
            ctaText: 'Xem mục tiêu',
            ctaPath: '/profile/goals',
            icon: '📉'
        });
    }

    // 2b. Đang GIAM mà cân tăng
    if (goal === 'GIAM' && weightTrend > 0.5) {
        triggers.push({
            priority: 'MEDIUM',
            type: 'WEIGHT_RISING_VS_GOAL',
            title: 'Cân nặng tăng ngoài kỳ vọng',
            message: `Đã tăng ${weightTrend.toFixed(1)}kg/tuần. Xem lại chế độ?`,
            ctaText: 'Xem chi tiết',
            ctaPath: '/nutrition-plan/history',
            icon: '📈'
        });
    }

    // 2c. Đạt target (nếu có set targetWeight)
    if (user.targetWeight && Math.abs(currentWeight - user.targetWeight) < 0.5) {
        triggers.push({
            priority: 'MEDIUM',
            type: 'TARGET_REACHED',
            title: '🎉 Đã đạt mục tiêu cân nặng!',
            message: 'Cập nhật mục tiêu mới để tiếp tục',
            ctaText: 'Đặt mục tiêu mới',
            ctaPath: '/profile/goals',
            icon: '🎉'
        });
    }
}
```

#### Loại 3 — LOW PRIORITY (Educational nudges)

```javascript
// Chỉ hiện nếu KHÔNG có HIGH/MEDIUM trigger nào
if (triggers.length === 0) {
    // 3a. Chưa có favorite sau 7 ngày dùng
    const daysActive = daysBetween(user.createdAt, today);
    if (daysActive >= 7 && favoriteDishes.length === 0) {
        triggers.push({
            priority: 'LOW',
            type: 'NO_FAVORITES',
            title: 'Đánh dấu món yêu thích',
            message: 'Hệ thống sẽ ưu tiên các món bạn thích trong đề xuất',
            ctaText: 'Xem món đã ăn',
            ctaPath: '/nutrition-plan/history',
            icon: '💡'
        });
    }

    // 3b. Chưa swap dish bao giờ
    if (daysActive >= 14 && !hasSwapHistory) {
        triggers.push({
            priority: 'LOW',
            type: 'NEVER_SWAPPED',
            title: 'Bạn có thể đổi món trong đề xuất',
            message: 'Click "Đổi món" trong thực đơn để chọn theo sở thích',
            ctaText: 'Tạo thực đơn',
            ctaPath: '/nutrition-plan',
            icon: '💡'
        });
    }
}
```

### 3.4. Sort & limit

```javascript
// Sort: HIGH trước, MEDIUM sau, LOW cuối
const priorityOrder = { HIGH: 0, MEDIUM: 1, LOW: 2 };
triggers.sort((a, b) => priorityOrder[a.priority] - priorityOrder[b.priority]);

// Take top 3
const displayed = triggers.slice(0, 3);

// Filter các trigger user đã dismiss (lưu localStorage)
const dismissed = JSON.parse(localStorage.getItem('dismissedNotifs') || '[]');
const final = displayed.filter(t => !dismissed.includes(t.type));
```

### 3.5. Display

**Container layout:**
```
┌──────────────────────────────────────────┐
│ NHẮC NHỞ                                 │
│                                          │
│ ⚠ Bạn chưa cân tuần này                  │
│   Cập nhật để hệ thống đề xuất chính xác│
│                          [Cập nhật →] ✕  │
│                                          │
│ 🍽 Chưa có thực đơn hôm nay              │
│   Tạo thực đơn để bắt đầu ngày mới      │
│                       [Tạo ngay →] ✕     │
│                                          │
│ 💡 Đánh dấu món yêu thích                │
│   Hệ thống sẽ ưu tiên trong đề xuất     │
│                    [Xem món →] ✕         │
└──────────────────────────────────────────┘
```

**Color theo priority:**

| Priority | Background | Border | Tailwind |
|---|---|---|---|
| HIGH | `amber-50` | `amber-300` | `bg-amber-50 border-amber-300` |
| MEDIUM | `blue-50` | `blue-300` | `bg-blue-50 border-blue-300` |
| LOW | `gray-50` | `gray-200` | `bg-gray-50 border-gray-200` |

### 3.6. States

**EMPTY** (không có trigger nào active):
```
┌──────────────────────────────────────────┐
│ NHẮC NHỞ                                 │
│                                          │
│ ✨ Tuyệt vời! Bạn đang theo dõi đầy đủ.  │
│ Tiếp tục duy trì để đạt mục tiêu.        │
└──────────────────────────────────────────┘
```

→ Một số UI vẫn show widget này (không hide hẳn) để user biết hệ thống đang active.

### 3.7. Interaction

- Click CTA button → navigate theo `ctaPath`
- Click `✕` → add type vào localStorage `dismissedNotifs`, ẩn nhắc trong 24h
- Sau 24h: clear dismissed nếu trigger vẫn active → re-show

---

## 4. LAYOUT TỔNG THỂ

### 4.1. Desktop (≥ 1024px)

```
┌────────────────────────────────────────────────────────────┐
│ Xin chào, Chiến!                                            │
│ 🏃 Đang theo mục tiêu: GIẢM CÂN     [Đổi mục tiêu]         │
└────────────────────────────────────────────────────────────┘

┌──────────────────────────┐  ┌────────────────────────────┐
│                          │  │                            │
│  WIDGET 1                │  │  WIDGET 3                  │
│  THỂ TRẠNG HIỆN TẠI      │  │  NHẮC NHỞ                  │
│                          │  │                            │
│  [Card chi tiết]         │  │  [3 nhắc với CTA]          │
│                          │  │                            │
└──────────────────────────┘  └────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│  WIDGET 2A — CÂN NẶNG 30 NGÀY                              │
│  [Line chart]                                              │
└────────────────────────────────────────────────────────────┘

┌──────────────────────────────┐  ┌────────────────────────┐
│                              │  │                        │
│  WIDGET 2B                   │  │  WIDGET 2C             │
│  ĐÃ CÓ THỰC ĐƠN 7 NGÀY      │  │  3 METRIC NHỎ          │
│                              │  │                        │
└──────────────────────────────┘  └────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ ▼ Chi tiết chỉ số y khoa (collapsed)                       │
│   BMR 1597 · TDEE 1917 · PBF 15.4% · WHR 0.9              │
└────────────────────────────────────────────────────────────┘
```

### 4.2. Mobile (< 768px)

Stack tất cả widget thành 1 cột theo thứ tự:
1. Header greeting + goal
2. Widget 1 (Thể trạng)
3. Widget 3 (Nhắc nhở)
4. Widget 2A (Weight chart)
5. Widget 2B (Compliance)
6. Widget 2C (3 metric)
7. Chi tiết chỉ số y khoa (collapsed)

Lý do ưu tiên thứ tự: Thể trạng + Nhắc nhở ưu tiên hiển thị trên đầu (user thấy đầu tiên khi mở app).

### 4.3. Spacing

- Padding page: `p-4` mobile, `p-8` desktop
- Gap giữa widget: `gap-4` mobile, `gap-6` desktop
- Padding trong card: `p-4` mobile, `p-6` desktop

---

## 5. API CONTRACT SUMMARY

| Endpoint | Service | Status | Used by | Note |
|---|---|---|---|---|
| `GET /api/health-data/constitution` | health-data-service | ❌ **CẦN BUILD** | Widget 1 | Logic compute toàn bộ ở BE. Xem `backend_endpoint_spec.md` §6 |
| `GET /api/health-data/dashboard-metrics` | health-data-service | ✅ existing | Widget 1 (timestamp), section "Chi tiết chỉ số" | Trả 7 chỉ số. Dùng `bmi.recordedAt` cho timestamp |
| `GET /api/health-data/query/history/WEIGHT?from=&to=&granularity=DAILY` | health-data-service | ✅ existing | Widget 2A | 30 ngày qua. Xem `backend_endpoint_spec.md` §13 |
| `GET /api/user/currentUser` | user-service | ✅ existing | Header greeting, Widget 3 (logic trigger) | Verify trả `birthDate`, `gender`, `profileCompleted` |
| `GET /api/user-preferences` | user-service | ❌ **CẦN BUILD** | Widget 1 (toggle PBF method) | Xem `backend_endpoint_spec.md` §2 |
| `PUT /api/user-preferences/{key}` | user-service | ❌ **CẦN BUILD** | Modal "Đổi method PBF" | Xem `backend_endpoint_spec.md` §2 |
| `GET /api/user-goals/current` | user-service | ❌ **CẦN BUILD** | Header "Đang theo mục tiêu: X", Widget 3 trigger | Xem `backend_endpoint_spec.md` §7 |
| `PUT /api/user-goals/current` | user-service | ❌ **CẦN BUILD** | Modal đổi mục tiêu | Xem `backend_endpoint_spec.md` §7 |
| `GET /api/meal-log/history?days=7` | nutrition-service | ✅ existing | Widget 2B, 2C, Widget 3 | OK |
| `GET /api/favorite-dishes` | nutrition-service | ✅ existing | Widget 2C, Widget 3 | OK |

**Cross-service sync:** `health-data-service` đã có sẵn pattern mirror data qua **RabbitMQ event** (`UserCreatedEvent`, `UserProfileUpdatedEvent` đã có cho `gender`/`birthDate`). Thêm event mới `UserPreferencesUpdatedEvent` cho `pbf_method`. KHÔNG dùng Feign (xem `backend_endpoint_spec.md` §3).

**Action items cho BE (theo thứ tự ưu tiên):**

1. **`common` module** — thêm event class `UserPreferencesUpdatedEvent`
2. **`user-service`** — build `user_preferences` table + 4 endpoint + seed default `pbf_method=FORMULA` khi register
3. **`user-service`** — build `user_goals` table + 3 endpoint (GET current/PUT current/history) — xem `backend_endpoint_spec.md` §7
4. **`user-service`** — add column `profile_completed` vào `User` entity + endpoint `PUT /api/user/profile-completed` — xem §8
5. **`user-service`** — publish `UserPreferencesUpdatedEvent` khi user update pref
6. **`health-data-service`** — build mirror table `user_preference_mirror` + service + listener handle event mới
7. **`health-data-service`** — build util `BodyClassifier` + `BodyClassificationService` + endpoint `GET /api/health-data/constitution` (response include `suggestedGoal` — xem §6.4)
8. **Gateway** — add routes `/api/user-preferences/**` và `/api/user-goals/**`
9. **(Optional)** — fix `HealthCalculator.calculatePBF()` bỏ check `ageYears` (xem §9)

---

## 6. CHECKLIST CHO CLAUDE DESIGN

Khi prompt Claude Design với spec này, ưu tiên hỏi mockup theo thứ tự:

- [ ] Phác wireframe tổng thể desktop layout (§4.1)
- [ ] Wireframe mobile stack (§4.2)
- [ ] High-fi Widget 1 với 4 variant (GAY / CAN_DOI / THUA_CAN / BEO_PHI) để verify color coding
- [ ] High-fi Widget 1 empty state + incomplete state
- [ ] High-fi Widget 2A (weight chart) với data và empty state
- [ ] High-fi Widget 2B (compliance 7 ngày)
- [ ] High-fi Widget 2C (3 metric card)
- [ ] High-fi Widget 3 với 3 nhắc đầy đủ (HIGH/MEDIUM/LOW)
- [ ] Widget 3 empty state
- [ ] Interactive prototype: click nhắc → simulate navigate

Mỗi mockup ra → screenshot, paste vào chat Claude (mình) để review trước khi sang mockup tiếp theo.

---

## 7. CHECKLIST CHO AGENT IMPLEMENT (sau khi mockup approved)

- [ ] Tạo file `src/pages/DashboardPage.tsx` mới (hoặc rename `HealthStatsPage` cũ)
- [ ] Tạo 3 sub-component: `<ConstitutionCard />`, `<WeightChart />`, `<ComplianceWeek />`, `<MetricCards />`, `<NotificationList />`
- [ ] Reuse `<Card />`, `<Button />` từ `components/admin/`
- [ ] Service layer: `dashboard.service.ts` aggregate data từ 5 endpoint
- [ ] Compute helper: `constitution.ts` (classifyByRule, classifyByModel)
- [ ] Notification trigger: `notifications.ts` (3 loại trigger logic)
- [ ] LocalStorage helper cho dismiss state
- [ ] Test responsive mobile + desktop

---

## VERSION HISTORY

| Ngày | Version | Thay đổi |
|---|---|---|
| 25/05/2026 | v1.0 | Spec ban đầu — Widget 1 (Thể trạng BMI+PBF rule-based), Widget 2 (3 sub-widget), Widget 3 (3 loại nhắc) |
| 25/05/2026 | v3.1 | Sync với onboarding: (1) Header greeting đọc goal từ `GET /api/user-goals/current`; (2) Widget 3 notification trigger dùng `currentGoal.goalCode` thay vì `user.currentGoal`; (3) API contract thêm 2 endpoint user-goals; (4) Endpoint history weight chart dùng `query/history/WEIGHT` (existing) thay vì endpoint mới. |
| 25/05/2026 | v3.0 | **Sync với backend_endpoint_spec.md v3.0** sau khi clone repo verify kiến trúc thực tế. Đổi: (1) Bỏ Feign — dùng RabbitMQ event pattern hiện có; (2) Đổi endpoint timestamp từ `health-data/latest` sang `health-data/dashboard-metrics` (có sẵn); (3) Update action items BE phản ánh đúng pattern. |
| 25/05/2026 | v2.0 | **Architecture change:** Logic phân loại thể trạng chuyển từ FE sang BE. Thêm endpoint `GET /api/health-data/constitution` (health-data-service) và `user_preferences` table (user-service). Cross-service call qua Feign. Xem chi tiết tại `backend_endpoint_spec.md`. |
