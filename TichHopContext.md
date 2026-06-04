# Tich hop Model 1 - BE context

File nay ghi lai cac thay doi BE de session khac va FE nam duoc contract moi.

## Muc tieu tong the

- Chuan bi `health-data-service` cho Model 1 PBF.
- Buoc 1: doi input/body measurement tu `WAIST` sang `ABDOMEN`, them `THIGH`.
- Buoc 2: tach PBF theo nguon tinh qua `method`: `FORMULA` va `MODEL_1`.
- Chua tich hop/chay model ML trong cac buoc nay.

## Trang thai checkpoint

### Checkpoint 1 - Input schema va metric base

Trang thai: da thuc hien, compile thanh cong, da commit.

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
- Da commit checkpoint nay.

Commit: `f6ac06d Prepare health data inputs for Model 1`.

### Checkpoint 2 - PBF method va response API

Trang thai: da thuc hien, compile thanh cong, da commit.

Thay doi trong checkpoint nay:

- `CalculatedMetricSnapshot`: them cot/entity field `method`.
- `CalculatedMetricSnapshotRepository`: them query latest snapshot theo `userId`, `indicatorType`, `method`.
- `CalculatedMetricService`: them `getLatestSnapshotByMethod(...)`.
- `CalculatedMetricServiceImpl`: PBF do cong thuc Navy luu `method = "FORMULA"`; BMI/BMR/TDEE/WHR luu `method = null`.
- `BodyClassificationServiceImpl`: doc preference `pbf_method`, lay dung PBF theo method; neu preference la `MODEL_1` nhung chua co snapshot model thi fallback sang `FORMULA` va tra warning.
- `ConstitutionResponse`: them `pbfFormula`, `pbfModel`; giu `pbf` la gia tri active dang dung.
- `DashboardMetricsResponse`: them `pbfFormula`, `pbfModel`; giu `pbf` cu de backward compatible.
- `HealthDataController /dashboard-metrics`: fetch PBF theo `FORMULA` va `MODEL_1` de set hai field moi.

API contract sau checkpoint nay:

- `GET /api/health-data/constitution` tra them:
  - `pbfFormula`: PBF Navy formula, nullable.
  - `pbfModel`: PBF Model 1, nullable.
  - `pbf`: gia tri PBF active theo preference/fallback.
  - `pbfSource`: method thuc su duoc dung, `FORMULA` hoac `MODEL_1`; null neu khong co PBF.
- `GET /api/health-data/dashboard-metrics` tra them:
  - `pbfFormula`: `MetricData`, nullable.
  - `pbfModel`: `MetricData`, nullable.
- Giai doan chua co model: `pbfModel` du kien null; neu user chon `MODEL_1` thi constitution fallback sang `FORMULA` khi co formula.

DB migration/backfill can chay:

```sql
UPDATE calculated_metric_snapshots
SET method = 'FORMULA'
WHERE indicator_type = 'PBF'
  AND method IS NULL
  AND source_category = 'CALCULATED';
```

Luu y:

- `ddl-auto=update` co the tu them cot `method`, nhung backfill du lieu cu van can SQL.
- PBF do user nhap (`source_category = 'USER_PROVIDED_CALCULATED'`) hien de `method = null`, khong duoc coi la `FORMULA` hay `MODEL_1`.
- Verification: da chay `.\mvnw.cmd -q -pl health-data-service -am compile` thanh cong.
Commit: `Track PBF calculation methods`.

### Checkpoint 3 - Test PBF method behavior

Trang thai: da thuc hien, test thanh cong, da commit.

Thay doi trong checkpoint nay:

- Them unit test `BodyClassificationServiceImplTest`.
- Test cac case:
  - Preference `FORMULA` thi `constitution.pbf` dung PBF formula, du `pbfModel` co ton tai.
  - Preference `MODEL_1` va co snapshot model thi `constitution.pbf` dung PBF model.
  - Preference `MODEL_1` nhung chua co snapshot model thi fallback sang `FORMULA` va tra warning `"Model AI chua san sang, dung cong thuc Navy"`.

Verification:

- Da chay `.\mvnw.cmd -q -pl health-data-service -Dtest=BodyClassificationServiceImplTest test` thanh cong.
- Da chay `.\mvnw.cmd -q -pl health-data-service test` thanh cong.

Luu y moi truong test:

- Test context hien co `HealthDataServiceApplicationTests` khoi dong Spring context that.
- Khi chay full test, service da ket noi MySQL/RabbitMQ local va co log Eureka connection refused nhung khong lam fail test.
- Do `ddl-auto=update`, Hibernate da chay `alter table calculated_metric_snapshots add column method varchar(255)` neu DB local chua co cot nay.
Commit: `Test PBF method classification`.

### Checkpoint 4 - DB migration runbook

Trang thai: da thuc hien, da commit.

Thay doi trong checkpoint nay:

- Them SQL runbook `health-data-service/src/main/resources/db/model1_pbf_migration.sql`.
- Script nay khong auto-run; dung de review/chay thu cong khi deploy.
- Script gom:
  - Them cot `calculated_metric_snapshots.method` neu cot chua ton tai.
  - Doi du lieu `base_metric_values.indicator_type`: `WAIST` -> `ABDOMEN`.
  - Doi config `health_indicator_configs.indicator_type`: `WAIST` -> `ABDOMEN`, co xu ly duplicate neu user da co san `ABDOMEN`.
  - Backfill config `THIGH` cho user hien huu trong `user_for_health_data` neu chua co.
  - Backfill PBF system-calculated cu thanh `method = 'FORMULA'`.
  - Cac query verification sau migration.

Luu y:

- Script target MySQL DB `health_db`.
- Can backup DB truoc khi chay production.
- Display name trong SQL dang de ASCII (`Vong bung`, `Vong dui`) de tranh loi encoding script; FE chu yeu nen dua vao key `ABDOMEN`/`THIGH` hoac label tu enum/API config sau khi sync.
Commit: `Add Model 1 DB migration runbook`.

### Checkpoint 5 - Final verification

Trang thai: da thuc hien, verification thanh cong, da commit.

Kiem tra da chay:

- `rg -n "WAIST|waist" health-data-service/src/main/java health-data-service/src/test/java common/src/main/java user-service/src/main/java`
  - Ket qua: khong con match trong source code chinh/test.
  - Cac match `WAIST` con lai nam trong `TichHopContext.md` va SQL runbook, dung de mo ta migration/verification.
- `rg -n "ABDOMEN|THIGH|pbfFormula|pbfModel|getLatestSnapshotByMethod|method = 'FORMULA'|method" ...`
  - Ket qua: contract moi co mat o enum, submit service, calculation service, classification service, controller, DTO, test, SQL runbook.
- `.\mvnw.cmd -q -pl health-data-service -am compile`
  - Ket qua: thanh cong.
- `.\mvnw.cmd -q -pl health-data-service test`
  - Ket qua: thanh cong.

Luu y moi truong:

- Full test van khoi dong Spring context that, ket noi MySQL/RabbitMQ local.
- Eureka connection refused xuat hien trong log do discovery-server local khong chay, nhung khong lam fail test.

Trang thai sau checkpoint 5:

- BE da hoan tat cac thay doi trong guide buoc 1 va buoc 2.
- Chua tich hop/chay Model 1 ML; `MODEL_1` chi moi duoc support o storage/API/selection path.
- SQL runbook chua duoc chay qua assistant; can review va chay thu cong khi deploy.
Commit: `Record Model 1 BE verification`.

## Ban giao FE va deploy

Trang thai: da thuc hien, da commit.

### Contract FE can cap nhat

- Submit flat fields moi:
  - Dung `abdomen` thay cho `waist`.
  - Them `thigh`.
  - Cac field con lai giu: `height`, `weight`, `hip`, `neck`, `bust`, `activityFactor`, `age`, `gender`, cac field `*New`.
- Neu FE gui `baseMetrics[]`:
  - Dung enum `ABDOMEN` thay cho `WAIST`.
  - Co the gui them enum `THIGH`.
- `GET /api/health-data/latest-metrics`:
  - `baseMetrics` co key `ABDOMEN` va `THIGH` neu da co du lieu.
  - Khong con key `WAIST` sau khi DB migration xong.
- `GET /api/health-data/constitution`:
  - Field cu `pbf` van la PBF active dang dung.
  - Field moi `pbfFormula`: PBF formula Navy, nullable.
  - Field moi `pbfModel`: PBF Model 1, nullable.
  - `pbfSource` la method thuc su duoc dung (`FORMULA` hoac `MODEL_1`), khong phai preference raw neu co fallback.
- `GET /api/health-data/dashboard-metrics`:
  - Field cu `pbf` van giu de backward compatible.
  - Field moi `pbfFormula`: `MetricData`, nullable.
  - Field moi `pbfModel`: `MetricData`, nullable.

### Thu tu deploy de tranh loi

1. Backup DB `health_db`.
2. Deploy BE code moi hoac dam bao schema da co cot `calculated_metric_snapshots.method`.
3. Chay SQL runbook `health-data-service/src/main/resources/db/model1_pbf_migration.sql`.
4. Verify DB bang cac query cuoi script:
   - `WAIST` khong con trong `base_metric_values`.
   - `WAIST` khong con trong `health_indicator_configs`.
   - User hien huu co config `THIGH` neu can render tu indicator configs.
   - PBF system-calculated cu co `method = 'FORMULA'`.
5. Deploy/cap nhat FE contract moi.
6. Test flow submit `abdomen`/`thigh`, latest metrics, constitution, dashboard.

### Viec con lai cho buoc tich hop ML that

- Tao luong sinh PBF Model 1 va luu snapshot `IndicatorType.PBF` voi `method = "MODEL_1"`.
- Xac dinh input bat buoc cho Model 1, du kien gom `ABDOMEN` va `THIGH` theo guide.
- Xac dinh thoi diem trigger model: sau submit base metrics, batch job, hay endpoint rieng.
- Xac dinh fallback/error khi model service timeout hoac input thieu.
- Them test cho luong luu `MODEL_1` sau khi co implementation model.

Commit: `Document Model 1 handoff contract`.
