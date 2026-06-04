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

## Current state for next session

Trang thai: da thuc hien, da commit.

Commits da tao cho phan BE Model 1 prep:

- `f6ac06d Prepare health data inputs for Model 1`
- `03722b6 Track PBF calculation methods`
- `9254f8d Test PBF method classification`
- `e3fc022 Add Model 1 DB migration runbook`
- `ee416f5 Record Model 1 BE verification`
- `d83d2f8 Document Model 1 handoff contract`

Verification gan nhat:

- Compile: `.\mvnw.cmd -q -pl health-data-service -am compile` thanh cong.
- Test: `.\mvnw.cmd -q -pl health-data-service test` thanh cong.
- Source code chinh/test khong con reference `WAIST|waist`; cac reference con lai chi nam trong context va SQL migration runbook.

Working tree luu y:

- Con cac thay doi `doc/` co san tu truoc, khong thuoc phan BE Model 1 prep va chua duoc commit boi cac checkpoint nay:
  - `doc/HuongDanXayDungTinhNangNguoiDungXayDungThucDon-BE.md` deleted.
  - `doc/UserTuDeXuatContext.md` deleted.
  - `doc/HuongDanNangCapTichHopModel1_Be.md` untracked.

Next action de deploy:

- Review/chay SQL runbook `health-data-service/src/main/resources/db/model1_pbf_migration.sql`.
- Cap nhat FE theo phan "Ban giao FE va deploy".
- Sau do moi sang buoc tich hop Model 1 ML that.

Commit: `Record Model 1 prep handoff state`.

## Step 3 - Danh gia tich hop FastAPI Model 1

Trang thai: da doc `doc/HuongDanThucHienBuoc3_TichHopModel1.md`, da danh gia kha thi, chua implement BE Step 3.

Phan A - Python/FastAPI:

- User da bao da thuc hien xong phan A trong project rieng `pbf-ml-service`.
- Truoc khi noi BE, can verify bang tay:
  - `GET http://localhost:8000/health` tra `status=ok` va `model_version`.
  - `POST http://localhost:8000/v1/predict/pbf` voi 1 dong test co ket qua PBF hop ly.
  - Xac nhan `Sex_M = 1` la nam, `0` la nu theo dung pipeline train.
  - Xac nhan metadata model co raw input columns dung 9 cot: `Sex_M`, `Age`, `Weight`, `Height`, `Neck`, `Chest`, `Abdomen`, `Hip`, `Thigh`.

Danh gia BE Step 3:

- Co the thuc hien duoc voi code hien tai.
- Storage path da san sang tu Step 2:
  - `CalculatedMetricSnapshot.method` da co.
  - `saveSystemCalculatedMetric(..., method)` da co trong `CalculatedMetricServiceImpl` va dang luu formula voi `method = "FORMULA"`.
  - `BodyClassificationServiceImpl` da doc `MODEL_1` dung method va fallback sang `FORMULA`.
- Khi co gia tri tu Python, chi can luu snapshot `IndicatorType.PBF` voi `method = "MODEL_1"`.

Diem can chinh so voi guide:

- Guide B4 co helper mau `baseMetricService.getLatestValue(...)`, nhung code hien tai khong co method nay.
- Can dung API thuc te:
  - `baseMetricService.getLatestBaseMetric(userId, type).map(BaseMetricValue::getValue).orElse(null)`.
- `saveSystemCalculatedMetric(...)` hien la `private` trong `CalculatedMetricServiceImpl`, nen method `predictAndSaveModel1Pbf(...)` nen dat trong cung impl de goi lai duoc, dung nhu guide.
- Nen config timeout that cho `RestClient` bang `ClientHttpRequestFactory`, khong chi khai bao `timeout-ms`.
- ML call phai boc try/catch trong submit service de submit khong fail khi Python service chet/timeout.
- Can xu ly loi DB enum MySQL truoc khi E2E:
  - DB hien co `indicator_type` dang la MySQL `enum(...)` cu, chua co `ABDOMEN`/`THIGH`.
  - Can migrate cac cot `indicator_type` sang `VARCHAR` hoac them enum values truoc khi tao user/submit du lieu moi.

Ke hoach thuc hien BE Step 3 neu duoc xac nhan:

1. Cap nhat `health-data-service/src/main/resources/application.yml`:
   - Them `app.ml.pbf-url`.
   - Them `app.ml.timeout-ms`.
2. Tao DTO client cho Python trong package moi, du kien `org.example.healthdataservice.dto.ml`:
   - `PbfPredictRequest` voi JSON snake_case `sex_m`.
   - `PbfPredictResponse` voi JSON snake_case `model_version`.
3. Tao `Model1PbfClient`:
   - Dung `RestClient`.
   - Base URL lay tu `${app.ml.pbf-url}`.
   - Timeout lay tu `${app.ml.timeout-ms}`.
   - POST `/v1/predict/pbf`.
   - Tra ve PBF va log can thiet; exception de caller quyet dinh fallback.
4. Cap nhat `CalculatedMetricService`:
   - Them `void predictAndSaveModel1Pbf(String userId, LocalDateTime now);`.
5. Cap nhat `CalculatedMetricServiceImpl`:
   - Inject `Model1PbfClient`.
   - Lay profile tu `userProfileMirrorService`.
   - Tinh age tu `birthDate`.
   - Lay latest base metrics bat buoc: `WEIGHT`, `HEIGHT`, `NECK`, `BUST`, `ABDOMEN`, `HIP`, `THIGH`.
   - Map `BUST -> chest`, `ABDOMEN -> abdomen`, `THIGH -> thigh`.
   - Neu thieu input/profile thi log va bo qua, van giu PBF formula.
   - Goi Python va luu `IndicatorType.PBF` voi `method = "MODEL_1"`.
6. Cap nhat `HealthDataSubmitServiceImpl`:
   - Sau `recalculateAndSaveDerivedMetrics(...)`, goi `predictAndSaveModel1Pbf(userId, now)` trong try/catch.
   - Khong lam fail `/submit` khi model service loi.
7. Them test:
   - Unit test cho `CalculatedMetricServiceImpl.predictAndSaveModel1Pbf(...)` voi du input thi goi client va save `MODEL_1`.
   - Test thieu `THIGH`/profile thi skip.
   - Test `HealthDataSubmitServiceImpl` khong fail khi `predictAndSaveModel1Pbf` throw exception.
8. Chay verification:
   - `.\mvnw.cmd -q -pl health-data-service -am compile`
   - Unit test lien quan.
   - Neu Python service dang chay va DB da migrate, test E2E theo Phan C cua guide.

Ket luan:

- OK de thuc hien BE Step 3 sau khi user xac nhan.
- Preconditions quan trong: Python service phan A phai pass sanity check va DB enum issue phai duoc migrate truoc E2E.

## Step 3 Checkpoint 1 - Config va DTO ML

Trang thai: da thuc hien, cho review, chua commit.

Thay doi:

- Cap nhat `health-data-service/src/main/resources/application.yml`:
  - `app.ml.pbf-url: ${ML_PBF_URL:http://localhost:8000}`
  - `app.ml.timeout-ms: ${ML_PBF_TIMEOUT_MS:3000}`
- Them package `org.example.healthdataservice.dto.ml`.
- Them `PbfPredictRequest`:
  - Co `@JsonProperty("sex_m")` cho `sexM`.
  - Cac field raw: `age`, `weight`, `height`, `neck`, `chest`, `abdomen`, `hip`, `thigh`.
- Them `PbfPredictResponse`:
  - `pbf`
  - `modelVersion` map JSON `model_version`.

Pham vi checkpoint nay:

- Chua tao HTTP client.
- Chua goi FastAPI trong submit.
- Chua luu `MODEL_1`.

Verification:

- Lan dau chay compile trong sandbox fail do Maven bi chan network khi resolve `spring-boot-starter-parent`.
- Chay lai voi quyen escalated: `.\mvnw.cmd -q -pl health-data-service -am compile` thanh cong.

Commit: `Add Model 1 ML config and DTOs`.

## Step 3 Checkpoint 2 - Model1PbfClient

Trang thai: da thuc hien, cho review, chua commit.

Thay doi:

- Them `org.example.healthdataservice.client.Model1PbfClient`.
- Client dung `RestClient`.
- Base URL lay tu `${app.ml.pbf-url}`.
- Connect/read timeout lay tu `${app.ml.timeout-ms}` qua `SimpleClientHttpRequestFactory`.
- Method `predictPbf(PbfPredictRequest)`:
  - POST `/v1/predict/pbf`.
  - Deserialize `PbfPredictResponse`.
  - Tra ve `Double pbf`.
  - Log warning neu response rong.

Pham vi checkpoint nay:

- Chua inject client vao `CalculatedMetricServiceImpl`.
- Chua luu `MODEL_1`.
- Chua goi model trong submit flow.

Verification:

- `.\mvnw.cmd -q -pl health-data-service -am compile` thanh cong.
