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

- Step hien tai: Step 8 - Test va kiem tra tong the.
- Trang thai Step 8: DONE, dang cho user review.
- Da xac nhan file context nam tai `doc/UserTuDeXuatContext.md`.
- Da cap nhat muc dich de file nay phuc vu ca BE va FE.
- Da ghi rule lam viec: sau moi step dung lai de user review, chi lam tiep khi user OK.
- Da commit Step 0: `90d2283 docs(nutrition): add user proposed meal context`.
- Da them seed `warn.carb_ratio_threshold = 0.70` vao `nutrition-service/src/main/resources/db/data.sql`.
- Da commit Step 1: `afec322 chore(nutrition): seed carb ratio warning config`.
- Da cap nhat DTO contract cho request/response cua luong user tu de xuat mon.
- Da chay `.\mvnw.cmd -pl nutrition-service -DskipTests compile`: BUILD SUCCESS.
- Da commit Step 2: `14d933e feat(nutrition): extend meal proposal dto contract`.
- Da cap nhat `DishRepository.searchByName(...)` cho endpoint search mon sau nay.
- Da commit Step 3: `1b254e7 feat(nutrition): add dish search repository query`.
- Da cap nhat `BruteForceEngine` de ho tro fixed serving theo index.
- Da commit Step 4: `63d0876 feat(nutrition): support fixed serving optimization`.
- Da cap nhat `RecommendationApiService.swapDish` de doc `overrideGrams`, truyen fixed serving vao engine va tra carb-bomb warnings.
- Da commit Step 5: `721a5a5 feat(nutrition): apply override grams in swap flow`.
- Da populate `unit` va `baseServingG` trong response mapper cua `RecommendationApiService`.
- Da commit Step 6: `764a6fd feat(nutrition): include serving unit metadata`.
- Da them `DishSearchService` va `DishController` cho endpoint search mon.
- Da commit Step 7: `93ef9bc feat(nutrition): add dish search endpoint`.
- Da chay `.\mvnw.cmd -pl nutrition-service test`: BUILD SUCCESS, 6 tests pass.

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

### Step 2 - Cap nhat DTO contract

Status: DONE

Noi dung da lam:

- Cap nhat `PinnedDish`:
  - Them `overrideGrams: BigDecimal`.
  - Them validation `@Positive`.
  - Field nullable de giu backward compatibility voi request cu.
- Cap nhat `DishSuggestionResponse`:
  - Them `unit: String`.
  - Them `baseServingG: Integer`.
- Cap nhat `DishOptionResponse`:
  - Them `unit: String`.
  - Them `baseServingG: Integer`.
  - Giu `expectedScore: BigDecimal` nullable theo mac dinh Java object.
- Cap nhat `SwapResultResponse`:
  - Them `warnings: List<WarningResponse>`.
- Tao DTO moi `WarningResponse`:
  - `type: String`.
  - `message: String`.

Files changed:

- `nutrition-service/src/main/java/org/example/nutritionservice/dto/request/PinnedDish.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/DishSuggestionResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/DishOptionResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/SwapResultResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/WarningResponse.java`
- `doc/UserTuDeXuatContext.md`

Ghi chu cho BE:

- Step nay moi them contract DTO, chua populate `unit`, `baseServingG`, `warnings`.
- Cac field moi se duoc gan gia tri o cac step service/mapper sau.
- `overrideGrams` moi duoc accept vao request, engine chua dung field nay o Step 2.

Ghi chu cho FE:

- `PinnedDish.overrideGrams` la optional. Khong gui field nay thi behavior cu duoc giu.
- `DishSuggestionResponse.unit` va `baseServingG` co the tam thoi null cho den khi BE mapper duoc cap nhat o step sau.
- `DishOptionResponse.expectedScore` co the null, dac biet voi search endpoint sau nay.
- `SwapResultResponse.warnings` co the null/empty cho den khi service warning duoc cap nhat.

Verification:

- Da chay `.\mvnw.cmd -pl nutrition-service -DskipTests compile`.
- Ket qua: BUILD SUCCESS.

Review can user xac nhan:

- DTO contract dung voi nhu cau FE/BE.
- Chap nhan viec cac field moi tam thoi chua duoc populate cho den cac step tiep theo.

### Step 3 - Cap nhat repository search mon

Status: DONE

Noi dung da lam:

- Cap nhat `DishRepository`.
- Them method `searchByName(SlotCode slotCode, String name, Pageable pageable)`.
- Query loc:
  - Cung `slotCode`.
  - `isActive = TRUE`.
  - Ten mon match theo `LOWER(d.name) LIKE LOWER('%name%')`.
  - Sap xep theo `d.name`.
- Them `Pageable` de service/controller sau nay gioi han so ket qua, tranh query search tra qua nhieu item.

Files changed:

- `nutrition-service/src/main/java/org/example/nutritionservice/repository/catalog/DishRepository.java`
- `doc/UserTuDeXuatContext.md`

Ghi chu cho BE:

- Search khong dau/co dau phu thuoc collation cua column `dishes.name`.
- Nen verify DB bang `SHOW FULL COLUMNS FROM dishes WHERE Field='name';`.
- Service sau nen goi repository voi `PageRequest.of(0, limit)` va trim query truoc khi search.

Ghi chu cho FE:

- Endpoint search sau nay se tim trong cung slot mon dang swap, khong tra mon slot khac.
- BE se gioi han so ket qua; FE nen xu ly list rong.

Verification:

- Da chay `.\mvnw.cmd -pl nutrition-service -DskipTests compile`.
- Ket qua: BUILD SUCCESS.

Review can user xac nhan:

- Signature repository dung huong cho service search sau nay.
- Chap nhan dung `Pageable` thay vi query khong gioi han nhu tai lieu ban dau.

### Step 4 - Cap nhat engine fixed serving

Status: DONE

Noi dung da lam:

- Cap nhat `BruteForceEngine`.
- Giu method cu `findBestServingCombo(List<DishCandidate>, MealTarget, LoadedConfigs, BigDecimal)` de backward compatibility.
- Them overload moi `findBestServingCombo(List<DishCandidate>, Map<Integer, BigDecimal>, MealTarget, LoadedConfigs, BigDecimal)`.
- `fixedServingByIndex` la map:
  - Key: index mon trong `pinnedDishes`.
  - Value: grams user override.
- Khi index co fixed grams:
  - Engine tinh serving multiplier = `overrideGrams / baseServingG`.
  - Engine chi enumerate 1 nhanh cho index do.
  - Engine skip `violatesWeightConstraint` cho index fixed.
  - Engine van giu check kcal deviation cu trong `scoreServingCombination`, nen override qua vo ly van co the khong tim duoc combo.
- Cap nhat prune logic:
  - Neu slot con lai la fixed serving, prune tinh kcal con lai bang exact fixed kcal.
  - Tranh prune sai khi fixed serving nam ngoai min/max serving config.
- `findTopK` tiep tuc truyen `null` cho fixed map, giu behavior recommend full-day nhu cu.

Files changed:

- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/BruteForceEngine.java`
- `doc/UserTuDeXuatContext.md`

Ghi chu cho BE:

- Step nay moi cap nhat engine. `RecommendationApiService.swapDish` chua build/truyen `fixedServingByIndex`.
- Step service sau se doc `PinnedDish.overrideGrams`, map slotKey sang index, roi goi overload moi.
- Backward compatibility duoc giu vi method cu van ton tai.

Ghi chu cho FE:

- Sau khi service duoc noi o step sau, `overrideGrams` se ep BE giu dung gram user chon cho mon pinned.
- Engine se khong tu doi mon o slot fixed; cac slot khac van chi toi uu serving trong luong swap.

Verification:

- Da chay `.\mvnw.cmd -pl nutrition-service -DskipTests compile`.
- Ket qua: BUILD SUCCESS.

Review can user xac nhan:

- Engine fixed serving dung voi mo hinh C: giu mon, chi toi uu serving cac slot khac.
- Chap nhan prune logic tinh exact kcal cho fixed serving de tranh cat nhanh sai.

### Step 5 - Noi overrideGrams va warnings vao swap service

Status: DONE

Noi dung da lam:

- Cap nhat `RecommendationApiService`.
- Trong `swapDish`:
  - Build `fixedServingByIndex` tu `request.pinnedDishes[*].overrideGrams`.
  - Map theo `slotKey` cua `currentDishes` sang index trong `pinnedCandidates`.
  - Goi overload moi cua `bruteForceEngine.findBestServingCombo(...)` voi `fixedServingByIndex`.
  - Sau khi co `bestCombo`, tinh carb-bomb warning.
  - Gan `warnings` vao `SwapResultResponse`.
- Them helper `buildFixedServingByIndex(...)`.
- Them helper `buildWarnings(...)`.
- Cap nhat message cua `findBestSwapSuggestion(...)` sang tieng Viet co dau.

Files changed:

- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/RecommendationApiService.java`
- `doc/UserTuDeXuatContext.md`

Ghi chu cho BE:

- `overrideGrams` chi co tac dung khi nam trong `pinnedDishes`.
- Neu `overrideGrams` khong null, slot tuong ung duoc fixed serving trong engine.
- Engine van giu kcal deviation final check, nen override qua cao/thap co the tra loi "Khong tim duoc serving thoa man sau khi doi mon".
- Warning carb-bomb doc threshold tu `warn.carb_ratio_threshold`.
- `warnings` tra ve list rong neu khong co warning.

Ghi chu cho FE:

- De ep serving, FE gui:
  - `pinnedDishes[].slotKey`
  - `pinnedDishes[].dishId`
  - `pinnedDishes[].overrideGrams`
- Neu user chi pin mon nhung khong ep gram, bo qua `overrideGrams` hoac gui null.
- `SwapResultResponse.warnings` co the la `[]`; FE nen render banner khi co item type `CARB_BOMB`.
- Message warning da la text tieng Viet san de render.

Verification:

- Da chay `.\mvnw.cmd -pl nutrition-service -DskipTests compile`.
- Ket qua: BUILD SUCCESS.

Review can user xac nhan:

- Behavior override serving dung mong muon.
- Warning carb-bomb nen duoc tinh trong Step 5, con populate `unit/baseServingG` de Step 6.

### Step 6 - Populate unit va baseServingG trong mapper

Status: DONE

Noi dung da lam:

- Cap nhat cac mapper trong `RecommendationApiService`.
- `toDishResponse(DishWithServing, ...)`:
  - Populate `unit` tu `dish.getCandidate().getDish().getUnit()`.
  - Populate `baseServingG` tu `dish.getCandidate().getDish().getBaseServingG()`.
- `toDishOptionResponse(SlotAlternative, ...)`:
  - Populate `unit`.
  - Populate `baseServingG`.
- `toDishResponse(DishCandidate, BigDecimal, ...)`:
  - Populate `unit`.
  - Populate `baseServingG`.
- `copyDish(...)`:
  - Preserve `unit`.
  - Preserve `baseServingG`.

Files changed:

- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/RecommendationApiService.java`
- `doc/UserTuDeXuatContext.md`

Ghi chu cho BE:

- Full-day recommendation, swap result va slot alternatives da co `unit/baseServingG` khi du lieu di qua `RecommendationApiService`.
- Meal-log history mapper trong `RecommendationController.toDishResponse(MealLogDish)` chua populate 2 field nay theo scope hien tai; se null neu response history dung DTO nay.
- Khong them query moi, lay truc tiep tu `DishCandidate.getDish()`.

Ghi chu cho FE:

- FE co the render serving dang `servingMultiplier unit (actualGrams g)` cho response full-day/swap/alternatives.
- Voi meal-log history, `unit/baseServingG` van co the null; FE can fallback hien thi grams.

Verification:

- Da chay `.\mvnw.cmd -pl nutrition-service -DskipTests compile`.
- Ket qua: BUILD SUCCESS.

Review can user xac nhan:

- Pham vi mapper Step 6 dung: populate duong full-day/swap, de history fallback null.
- FE fallback history null la chap nhan duoc.

### Step 7 - Them endpoint search mon

Status: DONE

Noi dung da lam:

- Tao `DishSearchService`.
- Tao `DishController`.
- Them endpoint `GET /api/nutrition/dishes/search`.
- Sau review cua user, da chinh lai phan tang:
  - `DishController` chi map HTTP request, resolve user id va goi service.
  - Validation request search chuyen xuong `DishSearchService`.
- Request params:
  - `slotCode`: enum `SlotCode`.
  - `q`: keyword search, trim, 1-50 ky tu.
  - `slotKcalTarget`: kcal target cua slot, phai > 0.
- Headers:
  - `X-User-Id` hoac `userId` de resolve favorite.
- Search behavior:
  - Chi tim mon active trong cung `slotCode`.
  - Gioi han toi da 20 ket qua bang `PageRequest.of(0, 20)`.
  - `expectedScore = null`.
  - `expectedServing` duoc chon theo serving steps tu system config:
    - `filter.combo_serving_steps` neu `slotCode = COMBO`.
    - `filter.serving_steps` cho slot con lai.
  - `expectedActualGrams = baseServingG * expectedServing`.
  - Populate `unit`, `baseServingG`, `favorite`.

Files changed:

- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/DishSearchService.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/controller/DishController.java`
- `doc/UserTuDeXuatContext.md`

Ghi chu cho BE:

- Search service dung `DishRepository.searchByName(...)` da them o Step 3.
- Favorite dung repository hien co `findByUserId`, khong them method moi.
- Neu khong co user id, `favorite` se false cho tat ca ket qua.
- Khong hardcode serving grid; doc config truc tiep tu `ConfigLoaderService`.
- Controller khong chua business validation/search logic; cac validate request nam trong service.

Ghi chu cho FE:

- Goi API qua gateway/service path:
  - `GET /api/nutrition/dishes/search?slotCode=TINH_BOT&q=com&slotKcalTarget=400`
- Response la `DataResponse<List<DishOptionResponse>>`.
- Moi item co:
  - `dishId`
  - `dishName`
  - `slotCode`
  - `foodGroupCode`
  - `expectedScore = null`
  - `expectedServing`
  - `expectedActualGrams`
  - `unit`
  - `baseServingG`
  - `favorite`
- FE can xu ly list rong va khong render score khi `expectedScore == null`.

Verification:

- Da chay `.\mvnw.cmd -pl nutrition-service -DskipTests compile` sau khi them endpoint.
- Da chay lai `.\mvnw.cmd -pl nutrition-service -DskipTests compile` sau khi chuyen validation tu controller xuong service.
- Ket qua: BUILD SUCCESS.

Review can user xac nhan:

- Contract endpoint search dung nhu FE can.
- Gioi han 20 ket qua va expectedServing theo config duoc chap nhan.

### Step 8 - Test va kiem tra tong the

Status: DONE

Noi dung da lam:

- Chay test module `nutrition-service`.
- Lenh:
  - `.\mvnw.cmd -pl nutrition-service test`
- Ket qua:
  - BUILD SUCCESS.
  - Tests run: 6.
  - Failures: 0.
  - Errors: 0.
  - Skipped: 0.

Ghi chu khi test:

- Spring context test co log warning do khong ket noi duoc Eureka tai `localhost:8761`.
- Warning nay khong lam fail test; ket qua Maven van BUILD SUCCESS.
- Chua smoke test API runtime bang Postman/cURL vi chua khoi dong day du discovery/gateway/nutrition-service trong Step 8.

Files changed:

- `doc/UserTuDeXuatContext.md`

Tong ket BE da hoan thanh:

- Seed config `warn.carb_ratio_threshold`.
- DTO contract cho override serving, warning va serving metadata.
- Repository search mon theo slot/name.
- Engine fixed serving.
- Swap service dung `overrideGrams`, tra warnings carb-bomb.
- Response mapper populate `unit/baseServingG` cho full-day/swap/alternatives.
- Endpoint search mon `GET /api/nutrition/dishes/search`.

Ghi chu cho BE tiep theo:

- Neu DB da ton tai tu truoc, can dam bao row `warn.carb_ratio_threshold` da duoc insert vao DB runtime.
- Can smoke test manual khi infra day du dang chay:
  - search endpoint.
  - swap-dish voi `overrideGrams`.
  - case warning `CARB_BOMB`.
  - case override qua cao/thap.

Ghi chu cho FE tiep theo:

- Co the dung `doc/UserTuDeXuatContext.md` lam contract BE/FE.
- Search endpoint tra `expectedScore = null`; FE khong render score khi null.
- `SwapResultResponse.warnings` la list, render banner neu co `type = CARB_BOMB`.
- Meal-log history van co the thieu `unit/baseServingG`; FE fallback grams.

Review can user xac nhan:

- Ket qua test chap nhan duoc.
- Co can them smoke test runtime/manual hay them unit test rieng cho `DishSearchService`/override swap truoc khi ket thuc BE khong.
