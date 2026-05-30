# User Tu De Xuat Context

## Muc dich

Ghi lai context cho tinh nang nguoi dung tu de xuat/chon mon trong luong xay dung thuc don, dua tren tai lieu `doc/HuongDanXayDungTinhNangNguoiDungXayDungThucDon-BE.md` va code hien tai cua `nutrition-service`.

File nay la context dung chung cho:

- Backend: nam ro thay doi API, DTO, service, engine, DB seed va test.
- Frontend: nam ro contract request/response, field moi, behavior warning, fallback history va luong UX can goi API.

## Quy trinh lam viec

- Thuc hien tung step nho.
- Sau moi step phai cap nhat state vao file nay.
- Sau moi step phai dung lai de user review.
- Chi thuc hien step tiep theo khi user xac nhan OK.

## Da thuc hien trong buoc danh gia

### Step 1 - Doc tai lieu va convention du an

- Da doc `doc/HuongDanXayDungTinhNangNguoiDungXayDungThucDon-BE.md`.
- Da doc `CLAUDE.md`.
- Luu y convention tu `CLAUDE.md`: user la nguoi code chinh; khong tu sua code tinh nang khi chua duoc xac nhan. Phan tich/review bang tieng Viet. Code comment, commit message, ten bien bang tieng Anh.

### Step 2 - Doi chieu voi cau truc repo

- Repo co module `nutrition-service` voi cac class lien quan:
  - `RecommendationApiService`
  - `BruteForceEngine`
  - `DishRepository`
  - `FavoriteDishRepository`
  - `RecommendationController`
  - DTO `PinnedDish`, `DishSuggestionResponse`, `DishOptionResponse`, `SwapResultResponse`
  - Entity `Dish`, `SystemConfig`
- Gateway da co route cho `/api/nutrition/**`, `/api/recommendation/**`, `/api/meal-log/**`, `/api/favorite-dishes/**`, nen endpoint search moi `/api/nutrition/dishes/search` co the di qua gateway.

### Step 3 - Ket luan kha thi

Tinh nang co the thuc hien duoc tren code hien tai. Huong tiep can cua tai lieu la dung ve tong the:

- Mo rong `POST /api/recommendation/swap-dish` de ho tro `overrideGrams`.
- Them endpoint search mon theo slot.
- Them warning carb-bomb vao response swap.
- Bo sung `unit` va `baseServingG` vao response de frontend hien thi serving than thien hon.

Tuy nhien khong nen code y nguyen tai lieu; can dieu chinh mot so diem de khop voi code hien tai va convention du an.

## Van de / diem can chinh truoc khi code

1. `warn.carb_ratio_threshold` khong co default trong code hien tai.
   - `LoadedConfigs.getDecimal(key)` goi `new BigDecimal(systemConfigs.get(key))`, neu thieu key se loi null.
   - Tai lieu noi default `0.70`, nhung code khong co fallback.
   - Nen them row vao `nutrition-service/src/main/resources/db/data.sql` bang `INSERT IGNORE`, hoac them helper doc config voi default. Cach hop voi project hon la them seed vao `data.sql`.

2. Tai lieu mau cho `DishSearchService` goi `favoriteDishRepository.findDishIdsByUserId(userId)`, nhung repository hien tai khong co method nay.
   - Hien co `findByUserId(String userId)`.
   - Khi code search service, can map `FavoriteDish::getDishId` nhu pattern `RecommendationApiService.favoriteIds`.

3. Comment language trong tai lieu mau bi lech voi `CLAUDE.md`.
   - Tai lieu yeu cau comment Java tieng Viet.
   - `CLAUDE.md` yeu cau code comment, commit message, ten bien bang tieng Anh.
   - Khi code nen theo `CLAUDE.md`: comment Java moi bang tieng Anh, giai thich ben ngoai bang tieng Viet.

4. Logic search serving trong tai lieu hardcode stepper chua khop config hien tai.
   - Tai lieu comment `0.5 / 0.75 / 1.0 / 1.25 / 1.5`, nhung code mau snap theo step `0.5`, se khong sinh `0.75` hoac `1.25`.
   - Config hien tai trong `data.sql`: `filter.serving_steps = [0.5,0.75,1.0,1.5,2.0]`.
   - Nen dung config/system config hien co hoac thong nhat lai voi FE. Neu search endpoint chi can goi y ban dau, co the chon nearest trong `filter.serving_steps` thay vi hardcode.

5. Search endpoint nen co gioi han so ket qua.
   - Query `LIKE %q%` khong limit co the on voi seed nho, nhung nen them `Pageable`/`Top` de tranh payload lon khi du lieu tang.

6. Khi them `unit` va `baseServingG`, can update tat ca mapper co lien quan, khong chi hai mapper trong tai lieu.
   - Trong `RecommendationApiService` co `toDishResponse(DishWithServing...)`, `toDishOptionResponse(...)`, `toDishResponse(DishCandidate, BigDecimal, ...)`, va `copyDish(...)`.
   - Trong `RecommendationController.toDishResponse(MealLogDish)` history co the de null nhu tai lieu de xuat, nhung can biet FE phai fallback.

7. SQL trong tai lieu co cac column `editable`, `created_by`, `updated_by`; entity `SystemConfig` co cac column nay, nen hop schema Hibernate hien tai.
   - Tuy nhien `data.sql` hien dung insert column rut gon. Neu them vao `data.sql`, nen giu style hien co hoac them day du column nhat quan.

8. Engine override serving can giu backward compatibility.
   - Nen them overload moi cho `findBestServingCombo(..., fixedServingByIndex, ...)` va giu method cu goi method moi voi `Map.of()`/`null`, de giam thay doi call site.
   - `findTopK` nen truyen `null`/empty map vao `enumerateServings` de behavior cu khong doi.

## Ke hoach thuc hien de xuat

1. Cap nhat DB seed/config
   - Them `warn.carb_ratio_threshold = 0.70` vao `nutrition-service/src/main/resources/db/data.sql`.
   - Can verify DB that co row nay neu moi truong da seed truoc do vi `INSERT IGNORE` chi them khi chua co.

2. Cap nhat DTO
   - `PinnedDish`: them `BigDecimal overrideGrams` nullable voi validation positive.
   - `DishSuggestionResponse`: them `String unit`, `Integer baseServingG`.
   - `DishOptionResponse`: them `String unit`, `Integer baseServingG`; `expectedScore` hien da nullable duoc.
   - `SwapResultResponse`: them `List<WarningResponse> warnings`.
   - Tao `WarningResponse`.

3. Cap nhat repository
   - Them method search active dish theo `slotCode` va name.
   - Can can nhac limit result va collation tieng Viet/accent-insensitive.

4. Cap nhat engine
   - Them support `fixedServingByIndex` trong `BruteForceEngine.findBestServingCombo`.
   - Trong `enumerateServings`, neu index co fixed grams thi tinh serving multiplier tu `fixedGrams / baseServingG`, them mot nhanh duy nhat, skip weight constraint cho slot override.
   - Giu kcal deviation check cu de truong hop override qua vo ly tra ve khong tim duoc combo.

5. Cap nhat `RecommendationApiService.swapDish`
   - Build `fixedServingByIndex` tu `request.pinnedDishes` dua tren `slotKey`.
   - Truyen map vao engine.
   - Tinh `warnings` sau khi co `bestCombo`.
   - Gan `warnings` vao `SwapResultResponse`.
   - Sua message suggestion sang tieng Viet co dau neu can theo UX, nhung code comment van nen bang English.

6. Cap nhat response mapper
   - Populate `unit` va `baseServingG` tu `DishCandidate.getDish()` trong full-day, swap, alternatives.
   - Cap nhat `copyDish` de khong mat field moi.
   - History mapping co the de null theo scope hien tai, ghi TODO neu can snapshot sau.

7. Them search service/controller
   - Tao `DishSearchService`.
   - Tao `DishController` path `/api/nutrition/dishes/search`.
   - Resolve user id theo pattern `X-User-Id`/`userId`.
   - Dung `findByUserId` de lay favorites.
   - Tinh `expectedServing` theo config serving steps neu co the, khong hardcode sai voi config hien tai.

8. Test/verify
   - Chay `mvn test -pl nutrition-service` hoac `mvnw.cmd test -pl nutrition-service`.
   - Smoke test:
     - `GET /api/nutrition/dishes/search?slotCode=TINH_BOT&q=com&slotKcalTarget=400`
     - `POST /api/recommendation/swap-dish` voi `overrideGrams`
     - Case override cao de trigger `CARB_BOMB`
     - Case override qua cao de verify response loi bat kha thi

## Trang thai hien tai

- Step hien tai: Step 1 - Cap nhat DB seed/config.
- Trang thai Step 1: DONE, dang cho user review.
- Da xac nhan file context nam tai `doc/UserTuDeXuatContext.md`.
- Da cap nhat muc dich de file nay phuc vu ca BE va FE.
- Da ghi rule lam viec: sau moi step dung lai de user review, chi lam tiep khi user OK.
- Da commit Step 0: `90d2283 docs(nutrition): add user proposed meal context`.
- Da them seed `warn.carb_ratio_threshold = 0.70` vao `nutrition-service/src/main/resources/db/data.sql`.
- Chua sua Java code tinh nang.
- Chua chay test.

## Nhat ky step

### Step 0 - Dong bo context va quy trinh lam viec

Status: DONE

Noi dung da lam:

- Kiem tra `git status --short`.
- Xac nhan `doc/UserTuDeXuatContext.md` ton tai trong thu muc `doc`.
- Doc lai context hien tai.
- Cap nhat file context de ghi ro dung cho ca Backend va Frontend.
- Cap nhat rule lam viec tung step va dung sau moi step.

Files changed:

- `doc/UserTuDeXuatContext.md`

Review can user xac nhan:

- Context file da dung vi tri va dung muc dich BE/FE.
- Quy trinh lam viec tung step da dung y user.

### Step 1 - Cap nhat DB seed/config

Status: DONE

Noi dung da lam:

- Them system config `warn.carb_ratio_threshold` vao block `system_config` trong `nutrition-service/src/main/resources/db/data.sql`.
- Dung `INSERT IGNORE` theo pattern seed hien tai cua project.
- Gia tri them:
  - `config_key`: `warn.carb_ratio_threshold`
  - `config_value`: `0.70`
  - `value_type`: `DECIMAL`
  - `description`: `Nguong ti le kcal tu carb de canh bao carb-bomb`

Files changed:

- `nutrition-service/src/main/resources/db/data.sql`
- `doc/UserTuDeXuatContext.md`

Ghi chu cho BE:

- Config nay tranh loi khi `LoadedConfigs.getDecimal("warn.carb_ratio_threshold")` duoc goi o buoc service sau.
- Neu database local da seed truoc do, can khoi dong lai service hoac chay SQL insert tuong duong de row duoc them vao DB hien co.

Ghi chu cho FE:

- Nguong warning mac dinh la 70% kcal tu carb tren tong kcal cua bua.
- FE khong can gui config nay; BE se doc tu `system_config`.

Review can user xac nhan:

- Key config va default `0.70` dung mong muon.
- Mo ta ASCII trong `data.sql` chap nhan duoc theo style seed hien tai.
