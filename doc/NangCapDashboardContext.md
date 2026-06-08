# Context nâng cấp Dashboard

Ngày ghi nhận: 2026-06-08  
Phạm vi: BE cho widget "Lộ trình khuyến nghị" trên Dashboard.

## Kết luận BE

Sau khi đối chiếu `doc/GUIDE_BE_recommendation_roadmap.md` với code hiện tại, BE đã đủ dữ liệu cho FE triển khai widget. Không cần thêm bảng, cột DB, DTO, endpoint, hoặc service mới cho MVP.

Các endpoint FE cần dùng đã tồn tại:

| Mục đích | Endpoint | Service |
|---|---|---|
| Lấy phân loại thể trạng | `GET /api/health-data/constitution` | `health-data-service` |
| Lấy chỉ số Dashboard | `GET /api/health-data/dashboard-metrics` | `health-data-service` |
| Đặt mục tiêu hiện tại | `PUT /api/user-goals/current` | `user-service` |

Tất cả endpoint đi qua API Gateway port `8080` và cần token như các API Dashboard hiện tại.

## Contract cho FE

### `GET /api/health-data/constitution`

Response được bọc trong `DataResponse`, nên payload thực nằm trong `data`.

Các field FE nên đọc:

| Field | Kiểu | Ghi chú |
|---|---|---|
| `data.bmi` | number | BMI hiện tại |
| `data.pbf` | number hoặc null | Tỷ lệ mỡ đang được chọn theo preference |
| `data.bmiClass` | number hoặc null | 0: gầy, 1: cân đối, 2: thừa cân, 3: béo phì |
| `data.pbfClass` | number hoặc null | Có thể null nếu thiếu số đo để tính PBF |
| `data.finalClass` | number hoặc null | BE lấy mức nặng hơn giữa BMI và PBF |
| `data.constitution` | string hoặc null | `GAY`, `CAN_DOI`, `THUA_CAN`, `BEO_PHI` |
| `data.suggestedGoal` | string hoặc null | `TANG`, `DUY_TRI`, hoặc `GIAM` |
| `data.warning` | string hoặc null | Cảnh báo thiếu dữ liệu hoặc fallback PBF |

Lưu ý: endpoint này cần profile có `gender`. Nếu thiếu gender hoặc thiếu BMI, BE có thể trả lỗi nghiệp vụ. FE cần có empty/error state.

### `GET /api/health-data/dashboard-metrics`

Response được bọc trong `DataResponse`.

Các field FE nên đọc:

| Field | Kiểu | Ghi chú |
|---|---|---|
| `data.weight.value` | number | Cân nặng hiện tại, đơn vị `kg` |
| `data.height.value` | number | Chiều cao hiện tại, đơn vị `cm` |
| `data.tdee.value` | number | TDEE hiện tại, đơn vị `kcal/day` |
| `data.bmi.value` | number | BMI snapshot mới nhất |
| `data.pbf.value` | number | PBF snapshot mới nhất nếu có |

`weight`, `height`, hoặc `tdee` có thể null nếu user chưa submit đủ chỉ số. FE không nên assume các field này luôn có giá trị.

### `PUT /api/user-goals/current`

Body FE gửi:

```json
{
  "goalCode": "GIAM",
  "targetWeightKg": 66.5,
  "targetDurationMonths": 6,
  "note": "Dat tu Dashboard roadmap"
}
```

Contract quan trọng:

| Field | Bắt buộc | Ghi chú |
|---|---:|---|
| `goalCode` | Có | Chỉ nhận `GIAM`, `DUY_TRI`, `TANG` |
| `targetWeightKg` | Không | Có thể null, ví dụ mục tiêu duy trì hoặc chưa chọn target cụ thể |
| `targetDurationMonths` | Không | Nếu null, BE mặc định 6 tháng; hợp lệ từ 1 đến 24 |
| `note` | Không | Text tự do |

Response trả về `data.startWeightKg` và `data.targetWeightKg`. `startWeightKg` do BE tự lấy từ cân nặng hiện tại, FE không cần gửi.

## Hành vi cần FE biết

`PUT /api/user-goals/current` hiện luôn:

1. Deactivate goal active hiện tại.
2. Tạo một goal mới với `startDate` là ngày hiện tại.
3. Snapshot `startWeightKg` từ `/api/health-data/dashboard-metrics`.

Vì vậy, nếu user đang có tiến độ theo goal cũ rồi bấm "Dùng mức này làm mục tiêu", tiến độ sẽ được tính lại từ hôm nay. Với MVP, hướng xử lý là FE hiển thị confirm trước khi gọi API.

Không nên gửi `goalCode` ngoài enum BE. Đặc biệt, không gửi `DAC_BIET`; BE hiện không có enum này và request sẽ lỗi.

## Cách FE tính roadmap

BE không trả sẵn roadmap. FE tự tính từ dữ liệu có sẵn:

- Dải cân đối theo BMI: `[18.5, 23] * (height / 100)^2`.
- Mode gợi ý từ `data.finalClass` hoặc `data.suggestedGoal`.
- Delta cân nặng từ `weight.value` đến target.
- Tốc độ thay đổi cân nặng có thể ước lượng từ TDEE, ví dụ 15-20% TDEE và quy đổi khoảng `7700 kcal = 1 kg`.

Không nên đẩy logic roadmap này xuống BE trong MVP vì sẽ tạo phụ thuộc chéo giữa health-data-service, nutrition-service và user-service.

## Checklist tích hợp nhanh

Trước khi ghép UI, FE nên kiểm tra 3 tình huống:

1. User đủ dữ liệu: `constitution`, `dashboard-metrics`, và `PUT user-goals/current` đều thành công.
2. User thiếu một phần dữ liệu: widget hiển thị empty state, không crash khi `weight`, `height`, `tdee`, hoặc `pbf` null.
3. User đã có active goal: bấm đặt target phải có confirm vì BE sẽ tạo goal mới và reset tiến độ.

## Quyết định đã thực hiện

- Không sửa BE code.
- Không thêm migration hoặc field DB.
- Không thêm endpoint roadmap riêng.
- Ghi lại API contract trong file này để FE dùng khi nâng cấp Dashboard.
