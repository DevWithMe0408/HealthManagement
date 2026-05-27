# NGHIỆP VỤ ĐỀ XUẤT THỰC ĐƠN — SPECIFICATION CHI TIẾT v3.0

> Tài liệu mô tả chi tiết nghiệp vụ đề xuất thực đơn cho hệ thống "Website đề xuất thực đơn".
> Mọi trọng số, threshold, mapping đều configurable qua admin panel và lưu trữ trong database.

---

## 1. TỔNG QUAN

Luồng đề xuất đi từ thông tin cá nhân → tính target dinh dưỡng → filter món ăn → brute force tổ hợp tối ưu → scoring → hiển thị. Hệ thống đề xuất thực đơn **cả ngày** (3 hoặc 5 bữa), xử lý tuần tự từng bữa và ghi nhận mức độ tuân thủ của người dùng để cải thiện đề xuất.

---

## 2. ĐẦU VÀO

### 2.1. Input từ người dùng

| Tham số | Kiểu | Giá trị hợp lệ | Bắt buộc |
|---|---|---|---|
| Thể trạng | ENUM | `GẦY`, `CÂN_ĐỐI`, `THỪA_CÂN`, `BÉO_PHÌ` | ✓ (từ ML/PBF) |
| Mục tiêu | ENUM | `GIẢM_CÂN`, `DUY_TRÌ`, `TĂNG_CÂN` | ✓ |
| TDEE | FLOAT | 1000-5000 kcal | ✓ |
| Số bữa/ngày | ENUM | `3_BỮA`, `5_BỮA` | ✓ |
| Kiểu bữa (per bữa) | ENUM | `COMBO` (1 món), `NHIỀU_MÓN` | ✓ |
| Số món chính | INT | 1-3 | ✓ (nếu NHIỀU_MÓN) |
| Số món rau | INT | 0-2 | ✓ (nếu NHIỀU_MÓN) |
| Số món tinh bột | INT | 0-1 | ✓ (nếu NHIỀU_MÓN) |

### 2.2. Dữ liệu hệ thống

- Lịch sử bữa ăn **3 ngày gần nhất** (gồm ngày hiện tại) + trạng thái tuân thủ
- CSDL món ăn với thông tin dinh dưỡng per 100g, food_group, slot_category
- Bảng cấu hình admin (tất cả trọng số lưu trong DB)

---

## 3. LUỒNG XỬ LÝ

### 3.1. Validation thể trạng vs mục tiêu

**Matrix cảnh báo:**

| Thể trạng \ Mục tiêu | GIẢM_CÂN | DUY_TRÌ | TĂNG_CÂN |
|---|---|---|---|
| GẦY | ⚠️ Warning | ℹ️ Info | ✅ OK |
| CÂN_ĐỐI | ✅ OK | ✅ OK | ✅ OK |
| THỪA_CÂN | ✅ OK | ℹ️ Info | ⚠️ Warning |
| BÉO_PHÌ | ✅ OK | ⚠️ Warning | 🚫 Warning mạnh |

- `✅ OK`: Tiếp tục bình thường
- `ℹ️ Info`: Thông báo nhẹ, không chặn
- `⚠️ Warning`: Cảnh báo, yêu cầu xác nhận 1 lần
- `🚫 Warning mạnh`: Khuyến nghị đổi mục tiêu + xác nhận 2 lần + log disclaimer

**Sau xác nhận:** Luôn tiếp tục theo mục tiêu người dùng chọn.

### 3.2. Tính calorie cho ngày

```
calories_per_day = TDEE × multiplier[Mục tiêu]
```

| Mục tiêu | Multiplier | Ghi chú |
|---|---|---|
| GIẢM_CÂN | 0.80 | Deficit 20% |
| DUY_TRÌ | 1.00 | Cân bằng |
| TĂNG_CÂN | 1.15 | Surplus 15% |

### 3.3. Chia calo cho từng bữa

**Option 3 bữa:**

| Bữa | % kcal/ngày |
|---|---|
| SÁNG | 25% |
| TRƯA | 40% |
| TỐI | 35% |

**Option 5 bữa:**

| Bữa | % kcal/ngày |
|---|---|
| SÁNG | 20% |
| PHỤ_SÁNG | 10% |
| TRƯA | 30% |
| PHỤ_CHIỀU | 10% |
| TỐI | 30% |

### 3.4. Tính macro target cho bữa

```
macro_kcal = meal_kcal × macro_ratio
macro_gram = macro_kcal / kcal_per_gram
```

Chuyển đổi: 1g P = 4 kcal, 1g C = 4 kcal, 1g F = 9 kcal.

**Macro ratio theo mục tiêu:**

| Mục tiêu | Protein | Fat | Carb |
|---|---|---|---|
| GIẢM_CÂN | 35% | 30% | 35% |
| DUY_TRÌ | 25% | 30% | 45% |
| TĂNG_CÂN | 30% | 25% | 45% |

### 3.5. Chia target theo slot

**Tỷ lệ slot THEO MỤC TIÊU (khi đủ 3 slot):**

| Mục tiêu | Món chính | Rau/phụ | Tinh bột | Lý do |
|---|---|---|---|---|
| GIẢM_CÂN | 55% | 15% | 30% | Tăng protein slot, giảm tinh bột |
| DUY_TRÌ | 50% | 15% | 35% | Cân bằng |
| TĂNG_CÂN | 45% | 15% | 40% | Tăng carb/tinh bột cho surplus |

**Logic phân bổ lại khi thiếu slot:**

| User chọn | Redistribute từ | Món chính | Rau/phụ | Tinh bột |
|---|---|---|---|---|
| 0 tinh bột | Tinh bột → chính +70%, rau +30% | +slot_tb×0.7 | +slot_tb×0.3 | 0% |
| 0 rau | Rau → chính +60%, tinh bột +40% | +slot_rau×0.6 | 0% | +slot_rau×0.4 |
| 0 rau + 0 tinh bột | Tất cả → chính | 100% | 0% | 0% |

Ví dụ GIẢM_CÂN, user chọn 0 tinh bột: chính = 55% + 30%×0.7 = 76%, rau = 15% + 30%×0.3 = 24%.

**Kiểu COMBO:** 1 slot duy nhất = 100% kcal bữa.

**Lưu ý:** Tỷ lệ slot chỉ dùng cho **filter query**, không dùng cho scoring. Scoring luôn tính trên target toàn bữa.

### 3.6. Filter ứng viên cho từng slot

**Tham số cấu hình:**

| Tham số | Default | Mô tả |
|---|---|---|
| `kcal_tolerance` | 0.15 (15%) | Biên chấp nhận kcal |
| `serving_min_multiplier` | 0.5 | Serving nhỏ nhất |
| `serving_max_multiplier` | 2.0 | Serving lớn nhất |
| `serving_steps` | [0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0] | Bước duyệt discrete |

**Điều kiện filter cho món D thuộc slot S:**

```
target_slot_kcal = meal_kcal × slot_ratio[goal][slot]

kcal_at_min_serving = D.kcal_per_100g × serving_min_multiplier
kcal_at_max_serving = D.kcal_per_100g × serving_max_multiplier

pass = (kcal_at_min_serving ≤ target_slot_kcal × (1 + kcal_tolerance))
   AND (kcal_at_max_serving ≥ target_slot_kcal × (1 - kcal_tolerance))
```

Nghĩa là: tồn tại **ít nhất 1 serving** trong [0.5, 2.0] để kcal nằm trong biên tolerance.

**Constraint khối lượng tuyệt đối (sau khi nhân serving):**

| Slot | Min (g) | Max (g) |
|---|---|---|
| Món chính | 50 | 250 |
| Rau/phụ | 80 | 300 |
| Tinh bột | 80 | 250 |
| Combo | 100 | 400 |

Tổ hợp nào có serving tạo ra khối lượng ngoài khoảng này → loại.

### 3.7. Brute force & Scoring

#### 3.7.1. Sinh tổ hợp

```
For each combination (M_chinh × M_rau × M_tinh_bot):
    For each serving combination (7^N với N = số món):
        Compute Final Score
Sort desc → top 10
```

**Performance estimates (7 mức serving):**

| Cấu hình | Tổ hợp món | × Serving (7^N) | Total |
|---|---|---|---|
| 1+1+1 = 3 món | ~100 | 343 | ~34K |
| 2+1+1 = 4 món | ~336 | 2,401 | ~807K |
| 3+1+1 = 5 món | ~672 | 16,807 | ~11M |
| 3+2+1 = 6 món | ~1344 | 117,649 | ~158M |

**Kỹ thuật tối ưu (xử lý sau khi thuật toán đúng):**
- Early exit: Loại combo có kcal tổng vượt ±25% target trước khi tính full score
- Memoize penalty: Penalty chỉ phụ thuộc combo (dish list), không phụ thuộc serving → tính 1 lần per combo
- Prune: Nếu best_score_so_far - current_macro_score > PENALTY_CAP → skip (không thể thắng)

#### 3.7.2. Macro Score

**Bước 1 — Tính tổng thực tế:**
```
actual_weight_g = dish.base_serving_g × serving_multiplier
total_P = Σ (dish.P_per_100g × actual_weight_g / 100)
total_F = Σ (dish.F_per_100g × actual_weight_g / 100)
total_C = Σ (dish.C_per_100g × actual_weight_g / 100)
total_kcal = total_P × 4 + total_F × 9 + total_C × 4
```

**Bước 2 — Deviation cho mỗi macro + kcal:**
```
raw_deviation = |actual - target| / target

if target < 2g:
    // Edge case: macro target quá nhỏ, skip scoring cho macro này
    macro_score = 1.0
    // Re-normalize trọng số: chia w của macro bị skip đều cho các macro còn lại
    continue

if actual > target:
    deviation = raw_deviation × surplus_penalty_factor[macro]
else:
    deviation = raw_deviation
```

**Bảng `surplus_penalty_factor` (phạt bất đối xứng khi DƯ):**

| Macro | Factor | Lý do |
|---|---|---|
| Protein | 0.3 | Dư protein nhẹ = tốt (giữ cơ, tăng no) |
| Fat | 0.8 | Dư fat phạt nặng (9 kcal/g, dễ tích mỡ) |
| Carb | 0.5 | Dư carb phạt vừa |
| Kcal | 0.7 | Dư kcal phạt gần bằng thiếu |

**Bước 3 — Score mỗi macro:**
```
macro_score = max(0, 1 - deviation / threshold)
```

| Tham số | Default | Ghi chú |
|---|---|---|
| `threshold` | 0.20 | Lưu trong DB, có thể test và chỉnh |

**Bước 4 — Macro Score tổng:**
```
Macro Score = (w_p × p_score + w_f × f_score + w_c × c_score + w_kcal × kcal_score) × 100
```

**Trọng số theo mục tiêu (w_kcal giảm xuống 0.10, redistribute cho macro):**

| Mục tiêu | w_p | w_f | w_c | w_kcal | Tổng |
|---|---|---|---|---|---|
| GIẢM_CÂN | 0.45 | 0.20 | 0.25 | 0.10 | 1.00 |
| DUY_TRÌ | 0.30 | 0.25 | 0.35 | 0.10 | 1.00 |
| TĂNG_CÂN | 0.35 | 0.20 | 0.35 | 0.10 | 1.00 |

Lý do giữ w_kcal = 0.10: kcal là hệ quả của P+F+C (kcal = P×4 + F×9 + C×4), nên phần lớn đã được cover bởi 3 macro scores. w_kcal nhỏ chỉ phạt thêm cho trường hợp macro lệch bù trừ nhưng kcal tổng vẫn đúng/sai.

`Macro Score ∈ [0, 100]`.

#### 3.7.3. Penalty 3 lớp

**Lớp 1 — Trùng chính xác (cùng `dish_id`):**

| Khoảng cách | Penalty |
|---|---|
| Cùng ngày (bữa khác) | 12 |
| 1 ngày trước | 6 |
| 2 ngày trước | 3 |
| > 2 ngày | 0 |

**Lớp 2 — Trùng nhóm thực phẩm (cùng `food_group`, khác `dish_id`):**

| Khoảng cách | Penalty |
|---|---|
| Cùng ngày | 6 |
| 1 ngày trước | 3 |
| 2 ngày trước | 1 |
| > 2 ngày | 0 |

**Lớp 3 — Hệ số slot (nhân vào penalty lớp 1 & 2):**

| Slot | Slot factor |
|---|---|
| Món chính | 1.0 |
| Rau/phụ | 0.5 |
| Tinh bột | 0.0 |
| Combo | 1.0 |

**Tính năng yêu thích (v1.0):**
- User đánh dấu món yêu thích → penalty của món đó × 0.5
- Ví dụ: Gà luộc yêu thích, cùng ngày penalty = 12 × 1.0 (slot) × 0.5 (yêu thích) = 6 thay vì 12
- User vẫn được khuyến khích đa dạng nhưng bớt bị phạt cho món ưa thích

**Danh sách food groups mặc định:**

| Code | Ví dụ |
|---|---|
| `GIA_CẦM` | Gà, vịt, chim cút |
| `THỊT_ĐỎ` | Bò, heo, trâu, dê |
| `HẢI_SẢN` | Tôm, cua, mực, ngao |
| `CÁ` | Cá basa, cá chép, cá hồi, cá rô |
| `TRỨNG` | Trứng gà, vịt, cút |
| `ĐẬU_ĐỖ` | Đậu phụ, các loại đậu |
| `RAU_LÁ` | Rau muống, cải, dền, mồng tơi |
| `RAU_CỦ` | Cà rốt, bí, su su, khoai |
| `TINH_BỘT_GẠO` | Cơm, cháo, xôi |
| `TINH_BỘT_MÌ` | Bún, phở, miến, mì, bánh mì |
| `COMBO` | Phở, bún bò, cơm tấm, bánh mì thịt |
| `BỮA_PHỤ` | Trái cây, sữa chua, bánh, hạt |

**Công thức tổng penalty:**
```
total_penalty = 0
for dish_i in tổ_hợp:
    fav_factor = 0.5 if dish_i.is_favorite else 1.0
    for history_h in lịch_sử_3_ngày:
        distance = compute_distance(current_meal, history_h)
        slot_factor = get_slot_factor(dish_i.slot)
        
        if dish_i.dish_id == history_h.dish_id:
            total_penalty += layer_1_penalty[distance] × slot_factor × fav_factor
        elif dish_i.food_group == history_h.food_group AND dish_i.slot == 'CHÍNH':
            total_penalty += layer_2_penalty[distance] × slot_factor × fav_factor

total_penalty = min(total_penalty, PENALTY_CAP)  // mặc định CAP = 40
```

**Lưu ý `compute_distance` — tính theo ngày lịch:**
- Đơn vị: **ngày lịch** (calendar day), không phải số bữa
- "Cùng ngày" = bữa khác trong cùng ngày lịch của bữa đang đề xuất
- "1 ngày trước" = bất kỳ bữa nào của ngày lịch trước ngày đang đề xuất
- "2 ngày trước" = bất kỳ bữa nào của 2 ngày lịch trước
- Công thức: `distance = target_meal_date - history_meal_date` (tính bằng ngày)
- Khi đề xuất cho **ngày mai** (user vào khuya): `target_meal_date = tomorrow` → hôm nay = "1 ngày trước", hôm qua = "2 ngày trước"
- Khi đề xuất cả ngày tuần tự: bữa đề xuất trước (sáng) được coi như lịch sử "cùng ngày" cho bữa sau (trưa, tối)

#### 3.7.4. Final Score

```
Final Score = max(0, Macro Score - total_penalty)
```

---

## 4. LUỒNG ĐỀ XUẤT CẢ NGÀY

### 4.1. Xác định bữa cần đề xuất

Dựa trên **thời điểm user vào app** + lịch sử ghi nhận trong ngày:

| Thời điểm vào | Bữa đã qua | Bữa cần đề xuất |
|---|---|---|
| 5:00 - 8:00 (sáng sớm) | Chưa bữa nào | Sáng → Trưa → Tối (+ phụ nếu 5 bữa) |
| 8:00 - 11:00 (giữa sáng) | Sáng đã ăn? | Nếu đã ghi nhận sáng → Trưa → Tối. Nếu chưa → Sáng → Trưa → Tối |
| 11:00 - 14:00 (trưa) | Sáng | Trưa → Tối |
| 14:00 - 17:00 (chiều) | Sáng, Trưa | Tối |
| 17:00 - 21:00 (tối) | Sáng, Trưa | Tối |
| 21:00 - 5:00 (khuya) | Cả ngày | Đề xuất cho ngày mai: Sáng → Trưa → Tối |

**Logic:**
- Hệ thống kiểm tra bảng `meal_log` xem bữa nào trong ngày đã có record
- Bữa có record (dù `status = FOLLOWED` hay `SKIPPED` hay `CUSTOM`) → đã qua → không đề xuất lại
- Bữa chưa có record → đề xuất

### 4.2. Xử lý tuần tự

```
function recommend_full_day(user):
    meals_to_suggest = determine_remaining_meals(user, current_time)
    daily_plan = []
    
    for meal_type in meals_to_suggest (theo thứ tự thời gian):
        # Lịch sử = DB 3 ngày + các bữa đã gen trước trong daily_plan
        effective_history = db_history + daily_plan
        
        result = recommend_single_meal(
            user, meal_type, effective_history
        )
        
        daily_plan.append(result.top_1)  // Top 1 làm lịch sử cho bữa sau
    
    return daily_plan  // Trả về cả ngày, user có thể thay đổi từng bữa
```

**Quan trọng:** Khi gen bữa trưa, bữa sáng (dù mới chỉ là đề xuất, chưa xác nhận) đã được tính là lịch sử "cùng ngày" → penalty hoạt động đúng.

### 4.3. Ghi nhận tuân thủ (Meal Logging)

**MVP:** Bỏ qua phần ghi nhận tuân thủ chi tiết. Hệ thống **assume user ăn theo đề xuất**. Mọi bữa được đề xuất sẽ tự động lưu vào lịch sử với status = `SUGGESTED` và dùng cho penalty.

**Post-MVP (v2.0):** Mở rộng thành 5 trạng thái (SUGGESTED / FOLLOWED / MODIFIED / CUSTOM / SKIPPED) với ghi nhận thực tế để penalty chính xác hơn.

---

## 5. EDGE CASES

### 5.1. User mới / lâu ngày không dùng app
- Không có lịch sử trong 3 ngày → penalty = 0 cho tất cả khoảng cách > 0
- Chỉ áp penalty "cùng ngày" (khi đề xuất tuần tự sáng→trưa→tối trong cùng ngày)
- Chấp nhận: vài ngày đầu có thể đề xuất trùng → penalty tự kick in khi có đủ dữ liệu lịch sử

### 5.2. Không có tổ hợp nào đạt ngưỡng
- Nếu `max(Final Score) < 30`: Hiển thị thông báo *"CSDL chưa đủ đa dạng cho mục tiêu này, vui lòng phản hồi admin"*
- Vẫn hiển thị top 3 (dù score thấp) để user không bị chặn hoàn toàn

### 5.3. Target macro quá nhỏ (< 2g)
- Skip macro đó khỏi scoring (set score = 1.0)
- Re-normalize trọng số: chia phần w của macro bị skip đều cho các macro còn lại
- Ví dụ: nếu target carb < 2g (bữa phụ chủ yếu protein), w_c = 0 → w_p, w_f, w_kcal mỗi cái cộng thêm w_c/3

### 5.4. Bữa phụ/snack
- Không có khái niệm slot → tất cả món = 100% kcal target
- Thường chỉ 1 món → scoring = Macro Score − Penalty
- CSDL cần category riêng `BỮA_PHỤ` (bánh, trái cây, sữa chua, hạt, smoothie...)
- Filter chỉ lấy món có slot_category = `BỮA_PHỤ`

### 5.5. Món combo (phở, bún bò, cơm tấm...)
- Serving range hẹp hơn: [0.75, 1.0, 1.25, 1.5] thay vì [0.5 - 2.0]
- Lý do: combo thường ăn nguyên phần, 0.5 hay 2.0 phần đều phi thực tế
- Slot_category = `COMBO`, áp riêng serving_steps cho category này

### 5.6. Macro có giá trị 0 trong món ăn
- Ức gà có Carb = 0 → khi tính total bữa vẫn cộng bình thường (+0)
- Không ảnh hưởng công thức deviation vì target bữa ≠ 0

### 5.7. User thay đổi mục tiêu giữa chừng
- Mục tiêu thay đổi → TDEE multiplier + macro ratio + slot ratio + scoring weights đều thay đổi
- Lịch sử bữa ăn KHÔNG reset (penalty vẫn tính đúng — mon đã ăn thì đã ăn)
- Đề xuất từ bữa tiếp theo sẽ dùng config mới

### 5.8. Ngày cuối tuần / ngày lễ
- Không có logic đặc biệt cho MVP
- Post-MVP: có thể thêm option "cheat day" (nới TDEE multiplier lên 1.0 cho giảm cân)

---

## 6. RE-OPTIMIZE KHI USER THAY ĐỔI

### 6.1. Quy tắc

1. User swap 1 món → các món khác (dish + serving) **giữ nguyên hoàn toàn**
2. Tính lại Final Score
3. Kiểm tra điều kiện hiển thị gợi ý:
   - `Final Score < 50` HOẶC
   - `Final Score giảm > 15` so với tổ hợp tối ưu ban đầu

### 6.2. Gợi ý serving

Nếu điều kiện gợi ý thỏa mãn:
- Grid search 7 mức serving cho các món **không bị user thao tác** (fix dish, vary serving)
- Tìm serving nào làm Final Score cao nhất
- Hiển thị: *"Điều chỉnh [Rau muống xào] từ 100g xuống 75g sẽ tăng score lên 68/100"*
- User tự quyết định apply hay không

### 6.3. Nguyên tắc bất biến

- **Không bao giờ auto-change** món hoặc serving của bất kỳ món nào
- Mọi thay đổi đều do user chủ động

---

## 7. TỔNG HỢP THAM SỐ CẤU HÌNH ADMIN

> Tất cả tham số dưới đây lưu trong bảng `system_config` (key-value), có thể chỉnh qua admin panel.

| Nhóm | Key | Default | Mô tả |
|---|---|---|---|
| **Mục tiêu** | `cal_mul.GIAM` | 0.80 | Hệ số TDEE giảm cân |
| | `cal_mul.DUY_TRI` | 1.00 | Hệ số TDEE duy trì |
| | `cal_mul.TANG` | 1.15 | Hệ số TDEE tăng cân |
| **Bữa 3** | `meal_ratio.3.SANG` | 0.25 | % kcal bữa sáng |
| | `meal_ratio.3.TRUA` | 0.40 | % kcal bữa trưa |
| | `meal_ratio.3.TOI` | 0.35 | % kcal bữa tối |
| **Bữa 5** | `meal_ratio.5.SANG` | 0.20 | |
| | `meal_ratio.5.PHU_SANG` | 0.10 | |
| | `meal_ratio.5.TRUA` | 0.30 | |
| | `meal_ratio.5.PHU_CHIEU` | 0.10 | |
| | `meal_ratio.5.TOI` | 0.30 | |
| **Macro ratio** | `macro.GIAM` | P:0.35, F:0.30, C:0.35 | |
| | `macro.DUY_TRI` | P:0.25, F:0.30, C:0.45 | |
| | `macro.TANG` | P:0.30, F:0.25, C:0.45 | |
| **Slot ratio** | `slot.GIAM` | chính:0.55, rau:0.15, tb:0.30 | Giảm cân: tăng chính, giảm tb |
| | `slot.DUY_TRI` | chính:0.50, rau:0.15, tb:0.35 | |
| | `slot.TANG` | chính:0.45, rau:0.15, tb:0.40 | Tăng cân: tăng tb |
| **Filter** | `filter.kcal_tolerance` | 0.15 | Biên lọc kcal |
| | `filter.serving_min` | 0.5 | |
| | `filter.serving_max` | 2.0 | |
| | `filter.serving_steps` | 0.5,0.75,1.0,1.25,1.5,1.75,2.0 | |
| | `filter.combo_serving_steps` | 0.75,1.0,1.25,1.5 | Serving hẹp cho combo |
| **Constraint** | `constraint.min_g.CHINH` | 50 | Min gram món chính |
| | `constraint.max_g.CHINH` | 250 | Max gram món chính |
| | `constraint.min_g.RAU` | 80 | |
| | `constraint.max_g.RAU` | 300 | |
| | `constraint.min_g.TINH_BOT` | 80 | |
| | `constraint.max_g.TINH_BOT` | 250 | |
| | `constraint.min_g.COMBO` | 100 | |
| | `constraint.max_g.COMBO` | 400 | |
| **Scoring** | `score.threshold` | 0.20 | Ngưỡng deviation |
| | `score.surplus_pf.P` | 0.3 | Penalty factor dư protein |
| | `score.surplus_pf.F` | 0.8 | Penalty factor dư fat |
| | `score.surplus_pf.C` | 0.5 | Penalty factor dư carb |
| | `score.surplus_pf.KCAL` | 0.7 | Penalty factor dư kcal |
| **Weight** | `weight.GIAM` | P:0.45, F:0.20, C:0.25, K:0.10 | w_kcal giảm xuống 0.10 |
| | `weight.DUY_TRI` | P:0.30, F:0.25, C:0.35, K:0.10 | |
| | `weight.TANG` | P:0.35, F:0.20, C:0.35, K:0.10 | |
| **Penalty L1** | `penalty.L1.same_day` | 12 | Trùng dish cùng ngày |
| | `penalty.L1.1_day` | 6 | Trùng dish 1 ngày trước |
| | `penalty.L1.2_day` | 3 | Trùng dish 2 ngày trước |
| **Penalty L2** | `penalty.L2.same_day` | 6 | Trùng group cùng ngày |
| | `penalty.L2.1_day` | 3 | |
| | `penalty.L2.2_day` | 1 | |
| **Slot factor** | `penalty.slot.CHINH` | 1.0 | |
| | `penalty.slot.RAU` | 0.5 | |
| | `penalty.slot.TINH_BOT` | 0.0 | |
| | `penalty.slot.COMBO` | 1.0 | |
| **Favorite** | `penalty.fav_discount` | 0.5 | Nhân penalty khi món yêu thích |
| **Cap** | `penalty.cap` | 40 | Penalty tối đa |
| **Display** | `display.top_k` | 10 | Số tổ hợp hiển thị |
| | `display.round_step_g` | 25 | Làm tròn serving (g) |
| **Re-optimize** | `reopt.score_threshold` | 50 | Gợi ý khi score < 50 |
| | `reopt.score_drop` | 15 | Gợi ý khi score giảm > 15 |
| **History** | `history.lookback_days` | 3 | Số ngày nhìn lại cho penalty |

---

## 8. PSEUDOCODE LUỒNG CHÍNH

```
function recommend_full_day(user):
    # Xác định bữa cần đề xuất
    meals_to_suggest = determine_remaining_meals(user, current_time)
    daily_plan = []
    
    for meal_type in meals_to_suggest:
        effective_history = db.get_history(user.id, days=3) + daily_plan
        result = recommend_single_meal(user, meal_type, effective_history)
        daily_plan.append({
            meal_type: meal_type,
            dishes: result.top_1.combo,
            servings: result.top_1.servings,
            status: 'SUGGESTED'
        })
    
    return daily_plan


function recommend_single_meal(user, meal_type, history):
    # Step 1: Validate
    warning = check_constitution_vs_goal(user.constitution, user.goal)
    if warning.level >= STRONG and not user.confirmed:
        return warning

    # Step 2: Tính target
    daily_kcal = user.TDEE × config.cal_mul[user.goal]
    meal_kcal = daily_kcal × config.meal_ratio[user.num_meals][meal_type]
    macro_target = compute_macro_target(meal_kcal, user.goal)
    slot_targets = split_by_slot(meal_kcal, user.goal, user.dish_config)
    
    # Step 3: Filter candidates
    candidates = {}
    for slot in slot_targets:
        candidates[slot] = db.query_dishes(
            slot_category=slot,
            kcal_range=slot_targets[slot].kcal ± config.filter.kcal_tolerance,
            serving_range=config.filter.serving_steps
        )
        candidates[slot] = apply_weight_constraints(candidates[slot], slot)
    
    # Step 4: Brute force
    results = []
    
    # Pre-compute penalty per combo (independent of serving)
    for combo in product(candidates.values()):
        penalty = compute_penalty(combo, history, user.favorites)
        
        for servings in serving_grid(combo):  # 7^N
            totals = compute_totals(combo, servings)
            
            # Early exit
            if abs(totals.kcal - meal_kcal) / meal_kcal > 0.25:
                continue
            if any(violates_weight_constraint(dish, serving)):
                continue
            
            macro_score = compute_macro_score(totals, macro_target, user.goal)
            final_score = max(0, macro_score - penalty)
            
            results.append({combo, servings, final_score, totals})
    
    # Step 5: Sort & return top K
    results.sort(by=final_score, desc=True)
    return results[:config.display.top_k]


function on_user_swap(current_plan, swapped_slot, new_dish, history):
    # Giữ nguyên tất cả món + serving khác
    updated = current_plan.copy()
    updated[swapped_slot] = new_dish  // serving giữ default hoặc tối ưu
    
    new_score = compute_final_score(updated, history)
    original_score = current_plan.final_score
    
    suggestion = null
    if new_score < config.reopt.score_threshold 
       or (original_score - new_score) > config.reopt.score_drop:
        # Tìm serving tốt hơn cho các món KHÔNG bị swap
        suggestion = grid_search_serving_for_others(
            updated, fixed_slot=swapped_slot, history
        )
    
    return {
        plan: updated,
        score: new_score,
        suggestion: suggestion  // null nếu không cần gợi ý
    }
```

---

## 9. LỊCH SỬ THAY ĐỔI

| Ngày | Phiên bản | Thay đổi |
|---|---|---|
| 16/04/2026 | v2.0 | Bổ sung toàn bộ giá trị mặc định, edge cases, pseudocode |
| 17/04/2026 | v3.0 | Cập nhật: w_kcal giảm 0.10, slot ratio theo mục tiêu, luồng đề xuất cả ngày, tính năng yêu thích, edge case mở rộng, pseudocode full day |
| 17/04/2026 | v3.1 | MVP scope: bỏ meal logging chi tiết (v2.0), clarify compute_distance theo ngày lịch, xử lý đề xuất ngày mai |
