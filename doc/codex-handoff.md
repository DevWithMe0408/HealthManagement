# Codex Handoff - Feature De Xuat Thuc Don

## Muc tieu task dang lam

- Thuc hien feature de xuat thuc don theo huong dan trong `doc/HuongDanXayDungFeatureDeXuatThucDon.md`.
- Step 1, Step 2 va Step 3 da duoc hoan thanh, commit va push len branch `feature-DeXuatThucDon`.
- Step 4 da duoc implement va verify xong, hien dang dung tai checkpoint review cuoi Part 1 truoc khi commit/push hoac sang Part 2.

## Cac file da doc

### Tai lieu va cau hinh tong quan

- `doc/HuongDanXayDungFeatureDeXuatThucDon.md`
- `doc/nghiep_vu_de_xuat_thuc_don_v3.md`
- `CLAUDE.md`
- `pom.xml`
- `nutrition-service/pom.xml`
- `nutrition-service/src/main/resources/application.yml`
- `api-gateway/src/main/resources/application.yml`

### Security va response/exception dung chung

- `common/src/main/java/org/example/security/HeaderAuthenticationFilter.java`
- `common/src/main/java/org/example/dto/DataResponse.java`
- `common/src/main/java/org/example/exception/BusinessException.java`
- `common/src/main/java/org/example/exception/GlobalExceptionHandler.java`
- `common/src/main/java/org/example/exception/ErrorCode.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/config/SecurityConfig.java`
- Gateway JWT filter va cac route lien quan trong `api-gateway`.

### Nutrition service

- Entity, repository, controller, service va test hien co cho cac nhom:
  - meal log
  - favorite dish
  - dish/catalog
  - recommendation config
  - recommendation core logic

## Cac file da sua

### Da commit truoc do

- Step 1: cac file domain/repository cho `MealLog`, `MealLogDish`, `MealStatus`, `MealType`.
- Step 2: cac file domain/repository cho `FavoriteDish`.
- Step 3: cac file core recommendation va test lien quan, bao gom repository mon an dung cho recommendation.

### Dang sua o Step 4, chua commit

- `api-gateway/src/main/resources/application.yml`
- `common/src/main/java/org/example/security/HeaderAuthenticationFilter.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/controller/FavoriteDishController.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/controller/RecommendationController.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/request/ConfirmMealRequest.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/request/RecommendFullDayRequest.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/request/SwapDishRequest.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/DailyPlanResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/DishSuggestionResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/MealCombinationResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/MealLogHistoryResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/MealSuggestionResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/dto/response/SwapResultResponse.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/exception/RecommendationExceptionHandler.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/exception/RecommendationTooComplexException.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/repository/meallog/MealLogDishRepository.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/service/favorite/FavoriteDishService.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/service/meallog/MealLogService.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/BruteForceEngine.java`
- `nutrition-service/src/main/java/org/example/nutritionservice/service/recommendation/RecommendationApiService.java`
- `nutrition-service/src/test/java/org/example/nutritionservice/service/meallog/MealLogServiceTest.java`
- `nutrition-service/src/test/java/org/example/nutritionservice/service/recommendation/RecommendationApiServiceTest.java`

### Tai lieu dang co thay doi rieng trong worktree

- `doc/HuongDanXayDungFeatureDeXuatThucDon.md` dang o trang thai `AM`.
- `doc/nghiep_vu_de_xuat_thuc_don_v3.md` dang o trang thai `A`.
- Can bao toan cac thay doi nay, khong revert va khong dua vao commit Step 4 neu nguoi dung khong yeu cau ro.

## Logic hien tai da hieu

### Tien do da chot

- Step 1 da them lich su bua an va chi tiet mon an da xac nhan.
- Step 2 da them mon yeu thich theo user.
- Step 3 da tach core recommendation de tinh ung vien bua an, diem so, penalty va qua trinh chon ket qua.
- Step 4 dua core logic ra API, them confirm meal log, history, swap dish va favorite dish API.

### API Step 4 da co

- `POST /api/recommendation/full-day`
- `POST /api/recommendation/swap-dish`
- `POST /api/meal-log/confirm`
- `GET /api/meal-log/history`
- `POST /api/favorite-dishes/{dishId}`
- `DELETE /api/favorite-dishes/{dishId}`
- `GET /api/favorite-dishes`

### Hanh vi quan trong

- Full-day recommendation validate cau hinh plan va canh bao constitution truoc khi goi orchestrator.
- Truong hop `BEO_PHI` ket hop goal `TANG` ma chua confirm se tra warning va khong tra danh sach bua an.
- Goi full-day chi sinh goi y, khong ghi `meal_log`.
- Confirm meal upsert theo `(userId, mealDate, mealType)`, xoa chi tiet mon cu cua meal log do roi luu snapshot moi.
- Swap thay mon trong cung slot, tinh lai score/penalty/history va tim lai serving grid cho cac mon giu nguyen neu can.
- Ket qua daily plan dang mang them `goalCode` va `planType` de swap co du context tinh lai.
- Khi brute-force vuot nguong 50 trieu combination, exception duoc map thanh HTTP 422.
- Nutrition service nhan duoc ca header `X-User-Id`, `X-Username`, `X-Roles` va cac header legacy `userId`, `username`, `userRoles`.
- API gateway da mo route cho recommendation, meal log va favorite dish.

## Bug/van de con lai

- Step 4 chua commit va chua push vi dang dung tai checkpoint review Part 1.
- Chay full test cua `nutrition-service` co log Eureka registration/connection failure khi discovery server khong chay. Test van pass, nhung log nhieu.
- Can tiep tuc canh giu cac thay doi tai lieu dang staged/modified rieng trong `doc`.
- Da tung quan sat physical PK order cua bang favorite dish do Hibernate tao la `(dish_id, user_id)` trong khi huong dan mo ta `(user_id, dish_id)`. Can xem lai neu cac buoc sau yeu cau dung thu tu PK vat ly.

## Command da chay va ket qua chinh

### Commit/push cac step truoc

- Step 1:
  - Commit `da26d6a`
  - Message `feat: hoan thanh step 1 lich su bua an`
  - Da push len `origin/feature-DeXuatThucDon`
- Step 2:
  - Commit `a47462f`
  - Message `feat: hoan thanh step 2 mon yeu thich`
  - Da push len `origin/feature-DeXuatThucDon`
- Step 3:
  - Commit `d60e3d5`
  - Message `feat: hoan thanh step 3 logic de xuat thuc don`
  - Da push len `origin/feature-DeXuatThucDon`

### Verify Step 4

- `.\mvnw clean compile -pl common,api-gateway,nutrition-service`
  - Ket qua: build success.
- `.\mvnw test -pl nutrition-service`
  - Ket qua: build success, 6 tests pass.
  - Ghi chu: co log Eureka connection refusal neu discovery server khong chay.
- `.\mvnw clean compile -pl nutrition-service`
  - Ket qua: build success theo yeu cau verify sau moi step.

### Manual check Step 4

- Da start local discovery server va nutrition service.
- Da kiem tra Eureka thay `NUTRITION-SERVICE` tra ve HTTP 200.
- Da goi full-day warning case va nhan warning voi `meals=[]`.
- Da goi full-day confirmed case va nhan danh sach bua con lai trong ngay cung score.
- Da xac nhan full-day khong tao meal log truoc khi confirm.
- Da kiem tra favorite flow `POST -> GET -> DELETE`.
- Da confirm cung bua an hai lan, ket qua con mot `meal_log` va mot dong dish snapshot tuong ung.
- Da kiem tra history tra ve ban ghi vua confirm.
- Da kiem tra swap tra bua an da doi mon va score duoc tinh lai.
- Da xoa row test local cua user `user-step4` va stop cac process local da start.

## Viec can lam tiep theo

1. Review diff Step 4 va doi chieu lai voi checkpoint trong huong dan.
2. Neu review dat, commit rieng Step 4, tranh tron cac file tai lieu neu nguoi dung khong yeu cau.
3. Push commit Step 4 len branch `feature-DeXuatThucDon` khi duoc yeu cau tiep tuc.
4. Chi bat dau Part 2/step tiep theo sau checkpoint review theo huong dan hoac khi nguoi dung chi dao ro.
5. Tiep tuc chay `.\mvnw clean compile -pl nutrition-service` sau moi step feature tiep theo.

## Nhung dieu khong duoc lam

- Khong revert, reset, checkout de mat thay doi hien co trong worktree.
- Khong commit chung cac tai lieu dang co trang thai rieng trong `doc` neu chua co yeu cau ro.
- Khong bo qua checkpoint review giua cac step neu huong dan yeu cau dung lai.
- Khong ghi meal log trong luong de xuat full-day; chi luu sau confirm.
- Khong thay doi contract header/gateway/security Step 4 ma khong kiem tra lai API flow.
- Khong bo qua lenh compile bat buoc sau moi step.
