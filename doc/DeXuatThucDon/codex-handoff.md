# Codex Handoff - Refactor Feature De Xuat Thuc Don v3.3

## Muc tieu task dang lam

- Refactor feature de xuat thuc don theo nghiep vu moi `doc/nghiep_vu_de_xuat_thuc_don_v3.3.md` va huong dan `doc/HuongDanRefactor_v3_2.md`.
- Cach lam theo yeu cau user: dung sau moi step/phien review; user OK thi moi commit step vua lam va tiep tuc step tiep theo.
- Da hoan thanh va commit Phan A, Phan B. Chua lam Phan C.
- Branch hien tai: `feature-DeXuatThucDon`.

## Cac file da doc

- `doc/codex-handoff.md`
- `CLAUDE.md`
- `doc/nghiep_vu_de_xuat_thuc_don_v3.md`
- `doc/nghiep_vu_de_xuat_thuc_don_v3.3.md`
- `doc/HuongDanRefactor_v3_2.md`
- `nutrition-service/src/main/resources/application.yml`
- `nutrition-service/src/main/resources/db/data.sql`
- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/DishFilterService.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/BruteForceEngine.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/RecommendationOrchestrator.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/RecommendationApiService.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/domain/recommendation/*` lien quan: `DishCandidate`, `DishWithServing`, `MealCombination`, `MealTarget`, `PerMealConfig`, `RecommendedMeal`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/request/SystemConfigUpdateRequest.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/SystemConfigResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/DishSuggestionResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/MealSuggestionResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/SwapResultResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/request/SwapDishRequest.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/controller/RecommendationController.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/controller/admin/AdminConfigController.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/service/config/SystemConfigServiceImpl.java`
- `nutrition-service/src/test/java/org/example/nutritionservice/service/recommendation/RecommendationApiServiceTest.java`

## Cac file da sua

### Da commit trong dot refactor nay

- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/DishFilterService.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/RecommendationOrchestrator.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/BruteForceEngine.java`
- `nutrition-service/src/main/resources/db/data.sql`
- `nutrition-service/src/main/java/org/example/nutritionservice/domain/recommendation/SlotAlternative.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/DishOptionResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/DishSuggestionResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/MealSuggestionResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/domain/recommendation/RecommendedMeal.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/RecommendationApiService.java`
- `nutrition-service/src/test/java/org/example/nutritionservice/service/recommendation/RecommendationApiServiceTest.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/request/SystemConfigUpdateRequest.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/SystemConfigResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/service/config/SystemConfigServiceImpl.java`

### Dang sua/chua commit

- `doc/codex-handoff.md` vua duoc cap nhat bo sung context nay.

### File tai lieu dang untracked, can giu nguyen

- `doc/HuongDanRefactor_v3_2.md`
- `doc/nghiep_vu_de_xuat_thuc_don_v3.3.md`

Khong tu y xoa/revert hai file nay. Chung la tai lieu user dua vao de refactor.

## Logic hien tai da hieu

### Thay doi nghiep vu v3.3 can implement

- Alternatives khong con la list full combo `alternativeCombinations`; response moi dung `slotAlternatives`.
- Moi dish trong `topCombination.dishes` co `slotKey` dang `CHINH_0`, `RAU_0`, `TINH_BOT_0`, ...
- Diversity:
  - Top combos group theo main key, moi main key chi lay 1 dai dien.
  - Neu `nMain >= 2`, main key la danh sach dish id slot CHINH da sort va join.
  - Co config `filter.forbid_same_food_group_in_main`, default true, de cam cap 2 mon CHINH cung food group.
- Swap logic moi (chua lam) se pin tat ca slot hien tai, thay slot user chon, va toi uu lai serving only.
- Suggestion logic moi (chua lam) chi goi y doi mon neu sau khi toi uu serving score van thap.

### Phan A da lam

- A1: `DishFilterService.filterCandidatesForSlot` nhan them `dishesNeededInSlot`.
  - Filter chia target theo so mon trong slot.
  - Dung bien moi `[0.5x, 1.5x]`.
  - `RecommendationOrchestrator` truyen nguyen slot target + slot count.
- A2: `BruteForceEngine` them early prune trong recursion serving.
  - Neu kcal hien tai + max phan con lai van thieu, prune.
  - Neu kcal hien tai + min phan con lai chac chan vuot, prune.
- A3: giam `filter.serving_steps` default tu 7 muc xuong 5 muc `[0.5,0.75,1.0,1.5,2.0]`, combo steps giu `[0.75,1.0,1.25,1.5]`.

### Phan B da lam

- B1:
  - Them `SlotAlternative` internal domain.
  - Them `DishOptionResponse`.
  - Them `slotKey` vao `DishSuggestionResponse`.
  - Doi `MealSuggestionResponse` tu `alternativeCombinations` sang `slotAlternatives`.
- B2:
  - `BruteForceEngine.findTopK` dung buffer `topK * 5`, sort score desc, group diverse theo main key.
  - `buildDishCombinations` filter combo co 2 mon CHINH cung `foodGroupCode` neu config flag true.
- B3:
  - `BruteForceEngine.computeSlotAlternatives(...)` tinh alternatives cho tung slot.
  - Alternatives giu cac slot khac, thay 1 candidate, thu serving steps, check weight + kcal deviation 25%, score theo macro score - penalty cua top combo.
  - Slot CHINH alternatives da diversity food group, khong cung group voi CHINH trong top combo.
  - `RecommendedMeal` giu `candidatesPerSlot` de map response.
  - `RecommendationApiService.toMealResponse` tra `slotAlternatives`.
  - `toCombinationResponse` tao `slotKey` cho tung dish.
- B4:
  - Them config seed:
    - `display.slot_alternatives_count = 10`
    - `filter.forbid_same_food_group_in_main = true`
  - Admin `GET/PUT /api/admin/configs/system` expose/update duoc:
    - `filter.forbidSameFoodGroupInMain`
    - `display.slotAlternativesCount`

## Bug/van de con lai

- Phan A checkpoint theo huong dan chua dat voi DB local:
  - Request `nMain=2,nRau=1,nCarb=1`, `forceCompute=false` van HTTP 422.
  - Message: `Uoc tinh 305662500 to hop`.
  - Candidate sau filter cho bua toi: CHINH=66, RAU=19, TINH_BOT=12.
  - `forceCompute=true` khong xong trong 120s; prune co chay nhung runtime van qua lau.
  - User da chap nhan: "Khong sao, cu thuc hien het cac noi dung can chinh sua, con ket qua thi toi uu sau."
- Debug log prune hien tai co the rat nhieu neu bat DEBUG, anh huong performance khi force compute. Can can nhac giam log ve trace hoac sample khi toi uu sau.
- `nMain=2` performance/cap can toi uu rieng sau khi hoan thanh cac thay doi contract.
- `spring-boot:run` mac dinh chay testCompile; neu constructor service thay doi thi test compile co the fail truoc khi app start. Da cap nhat `RecommendationApiServiceTest` cho dependency moi.
- Test co log Eureka connection refused neu discovery-server khong chay; tests van pass.
- Hai file doc `doc/HuongDanRefactor_v3_2.md` va `doc/nghiep_vu_de_xuat_thuc_don_v3.3.md` dang untracked.
- Handoff nay dang modified sau commit B4; neu can luu tren git thi user co the yeu cau commit rieng, con hien tai dung de compact.

## Command da chay va ket qua chinh

### Commit refactor da tao

- `97353ec feat: hoan thanh step A1 filter ung vien`
- `d9cea97 feat: hoan thanh step A2 early prune`
- `24a726e feat: hoan thanh step A3 giam serving steps`
- `c877a6e feat: hoan thanh step B1 slot alternatives DTO`
- `d6b38bc feat: hoan thanh step B2 diversity main key`
- `27ab522 feat: hoan thanh step B3 slot alternatives`
- `e64a402 feat: hoan thanh step B4 cau hinh slot alternatives`

### Verify da chay

- `.\mvnw clean compile -pl nutrition-service`
  - Da chay sau cac step A/B, build success.
- `.\mvnw test -pl nutrition-service`
  - Sau B3: build success, 6 tests pass.
  - Sau B4: build success, 6 tests pass.
- DB update/verify qua MySQL container:
  - `filter.serving_steps = [0.5,0.75,1.0,1.5,2.0]`
  - `filter.combo_serving_steps = [0.75,1.0,1.25,1.5]`
  - `display.slot_alternatives_count = 10`
  - `filter.forbid_same_food_group_in_main = true`
- Smoke API sau B3:
  - `POST http://localhost:8888/api/recommendation/full-day`, `nMain=1`, HTTP 200.
  - Response co `slotAlternatives`.
  - Response khong con `alternativeCombinations`.
  - `topCombination.dishes` co `slotKey`: `CHINH_0`, `RAU_0`, `TINH_BOT_0`.
- Admin API sau B4:
  - `GET /api/admin/configs/system` HTTP 200, co `forbidSameFoodGroupInMain`, `slotAlternativesCount`.
  - `PUT /api/admin/configs/system` HTTP 200 voi cung gia tri, save OK.

### Local service/process

- Da start nutrition-service nhieu lan bang `.\mvnw spring-boot:run -pl nutrition-service` de smoke test.
- Da stop cac process listen port 8888 sau khi test.
- MySQL container dang chay: `mysql_container`.

## Viec can lam tiep theo

1. Sau compact, doc file nay truoc.
2. Kiem tra `git status --short`; du kien chi con:
   - modified `doc/codex-handoff.md`
   - untracked `doc/HuongDanRefactor_v3_2.md`
   - untracked `doc/nghiep_vu_de_xuat_thuc_don_v3.3.md`
3. Tiep tuc Phan C trong `doc/HuongDanRefactor_v3_2.md`.
4. Step tiep theo la **C1: Refactor `SwapDishRequest` schema va `SwapResultResponse` schema**.
5. Sau C1 phai compile `.\mvnw clean compile -pl nutrition-service`, bao user review, khong commit neu user chua OK.
6. Sau user OK moi commit C1 va lam C2.
7. Sau C2 can test swap:
   - swap 1 slot, score khong sut ve ~16 do serving cu.
   - swap 2 lan voi `pinnedDishes`, slot da pin duoc giu.
   - suggestion null khi score tot.
   - suggestion khac null khi score thap.
   - `nMain=2` moi slot co the swap rieng.

## Nhung dieu khong duoc lam

- Khong revert/reset/checkout mat thay doi hien co.
- Khong tu y xoa hoac commit hai file doc untracked neu user chua yeu cau.
- Khong tu y push neu user chua yeu cau.
- Khong ghi meal log khi goi full-day recommendation; full-day chi sinh goi y.
- Khong quay lai `alternativeCombinations`; contract moi la `slotAlternatives`.
- Khong auto-doi mon khac trong swap moi; swap Phan C phai pin mon hien tai va chi toi uu serving.
- Khong bo qua compile sau moi step.
- Khong nhay qua checkpoint review step neu user chua OK.
