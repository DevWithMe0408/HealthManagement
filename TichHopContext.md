# Tich hop Model 1 - BE context

File nay ghi lai cac thay doi BE de session khac va FE nam duoc contract moi.

## Muc tieu tong the

- Chuan bi `health-data-service` cho Model 1 PBF.
- Buoc 1: doi input/body measurement tu `WAIST` sang `ABDOMEN`, them `THIGH`.
- Buoc 2: tach PBF theo nguon tinh qua `method`: `FORMULA` va `MODEL_1`.
- Chua tich hop/chay model ML trong cac buoc nay.

## Trang thai checkpoint

### Checkpoint 1 - Input schema va metric base

Trang thai: da thuc hien, compile thanh cong, cho review.

Thay doi trong checkpoint nay:

- `IndicatorType`: doi `WAIST` thanh `ABDOMEN`, them `THIGH`.
- `SubmitHealthDataRequest`: doi flat field `waist` thanh `abdomen`, them `thigh`.
- `HealthDataSubmitServiceImpl`: luu flat fields moi vao `ABDOMEN` va `THIGH`.
- `HealthCalculator`: doi ten tham so PBF/WHR tu waist sang abdomen, giu nguyen cong thuc.
- `CalculatedMetricServiceImpl`: PBF va WHR doc `ABDOMEN` thay cho `WAIST`; `THIGH` khong tham gia tinh Navy/BMI/WHR.

API contract sau checkpoint nay:

- Submit flat fields dung: `height`, `weight`, `abdomen`, `hip`, `neck`, `bust`, `thigh`, `activityFactor`, `age`, `gender`.
- `GET /api/health-data/latest-metrics` se tra key `ABDOMEN` va `THIGH` trong `baseMetrics` neu da co du lieu.
- `WAIST` la breaking change: client con gui `waist` hoac enum `WAIST` can duoc cap nhat.

DB migration can chay truoc/sau khi deploy checkpoint nay:

```sql
UPDATE base_metric_values
SET indicator_type = 'ABDOMEN'
WHERE indicator_type = 'WAIST';

UPDATE health_indicator_configs
SET indicator_type = 'ABDOMEN'
WHERE indicator_type = 'WAIST';
```

Luu y:

- `THIGH` la enum moi, khong can migrate du lieu cu trong `base_metric_values`.
- User cu co the thieu config `THIGH` trong `health_indicator_configs`; can backfill neu FE render input tu endpoint config.
- Verification: da chay `.\mvnw.cmd -q -pl health-data-service -am compile` thanh cong.
- Chua git commit. Commit chi thuc hien sau khi user review va xac nhan.
