# CRUD Mon An Admin - Context

## Pham vi

Tinh nang CRUD admin cho `Dish` tai endpoint `/api/admin/dishes`.

Pha hien tai chi xu ly cac field tu than cua mon an:

- Tao, sua, liet ke, xem chi tiet.
- Bat/tat hien thi bang `isActive`.
- Macro nhap tay: `kcalPer100g`, `proteinPer100g`, `fatPer100g`, `carbPer100g`.
- Khong xu ly anh.
- Khong sua `dish_ingredients`.
- Khong tinh macro tu nguyen lieu.
- Khong xoa cung dish.

## API Contract Du Kien

Base path: `/api/admin/dishes`

| Method | Path | Mo ta |
| --- | --- | --- |
| GET | `/api/admin/dishes` | Liet ke co phan trang, loc va sort |
| GET | `/api/admin/dishes/{id}` | Xem chi tiet mon an |
| POST | `/api/admin/dishes` | Tao mon an |
| PUT | `/api/admin/dishes/{id}` | Sua mon an |
| PATCH | `/api/admin/dishes/{id}/active?active=true|false` | Bat/tat hien thi |

Query list:

- `page`: mac dinh `0`
- `size`: mac dinh `10`, toi da `100`
- `sort`: mac dinh `updatedAt,desc`
- `search`: tim theo ten
- `slotCode`: `CHINH`, `RAU`, `TINH_BOT`, `COMBO`, `BUA_PHU`
- `isActive`: `true` hoac `false`

Sort field hop le:

- `id`
- `name`
- `slotCode`
- `foodGroupCode`
- `kcalPer100g`
- `proteinPer100g`
- `fatPer100g`
- `carbPer100g`
- `baseServingG`
- `unit`
- `isActive`
- `createdAt`
- `updatedAt`
- `updatedBy`

Sort direction hop le: `asc`, `desc`.

## Request Tao/Sua

`DishUpsertRequest`:

- `name`
- `slotCode`
- `foodGroupCode`
- `kcalPer100g`
- `proteinPer100g`
- `fatPer100g`
- `carbPer100g`
- `baseServingG`
- `unit`
- `description`
- `isActive`

Luu y:

- `foodGroupCode = GIA_VI` khong hop le cho mon an.
- Khi tao moi, neu `isActive = null` thi mac dinh la `true`.

## Response Admin

`DishAdminResponse`:

- `id`
- `name`
- `slotCode`
- `foodGroupCode`
- `kcalPer100g`
- `proteinPer100g`
- `fatPer100g`
- `carbPer100g`
- `baseServingG`
- `unit`
- `description`
- `isActive`
- `createdAt`
- `updatedAt`
- `updatedBy`

## Error Codes

Du kien them vao module `common`:

- `CATALOG-001`: mon an khong ton tai.
- `CATALOG-002`: ten mon da ton tai.

Validation dung `COMMON-001`.

## Tien Do Thuc Hien

| Buoc | Trang thai | Ghi chu |
| --- | --- | --- |
| 1. Them error code CATALOG cho Dish | Da xong | Them `DISH_NOT_FOUND`, `DISH_NAME_TAKEN` |
| 2. Bo sung repository admin query | Da xong | Them check trung ten va `findForAdmin` |
| 3. Tao DTO request/response | Da xong | `DishUpsertRequest`, `DishAdminResponse` |
| 4. Tao service admin | Da xong | `DishAdminService`, `DishAdminServiceImpl` |
| 5. Tao controller admin | Da xong | `AdminDishController` |
| 6. Them gateway route | Da xong | `/api/admin/dishes/**` -> `nutrition-service` |
| 7. Build verify | Da xong | Maven compile PASS |
| 8. Commit va push | Da xong | Da push branch `AdminTongQuanPage` |

## Nhat Ky

- Khoi tao context file cho BE/FE theo doi tien do CRUD mon an admin.
- Buoc 1 da them error code `DISH_NOT_FOUND` (`CATALOG-001`) va `DISH_NAME_TAKEN` (`CATALOG-002`) vao `common`.
- Buoc 2 da cap nhat `DishRepository`: them `existsByNameIgnoreCase`, `existsByNameIgnoreCaseAndIdNot`, va `findForAdmin(search, slotCode, isActive, pageable)`.
- Buoc 3 da tao DTO `DishUpsertRequest` va `DishAdminResponse` trong `nutrition-service`.
- Buoc 4 da tao service `DishAdminService` va `DishAdminServiceImpl`.
- Service hien tai xu ly: list, get detail, create, update, set active; check trung ten; tra `DISH_NOT_FOUND` khi khong tim thay; tra `DISH_NAME_TAKEN` khi trung ten; chan `foodGroupCode = GIA_VI` bang `VALIDATION_FAILED`.
- Service co ham rieng `applyMacros` de giu macro nhap tay trong pha nay va de sau nay de doi sang tinh macro tu nguyen lieu neu can.
- Buoc 5 da tao `AdminDishController` tai `/api/admin/dishes` voi `@PreAuthorize("hasRole('ADMIN')")`.
- Controller da co validation cho `page >= 0`, `size` trong khoang `1..100`, `slotCode` hop le, `sort` field/direction hop le. Request sai tra `VALIDATION_FAILED`.
- Buoc 6 da them route gateway `admin-dishes`: `Path=/api/admin/dishes/**`, `uri=lb://nutrition-service`, dung `JwtFilter`.
- Buoc 7 da chay `.\mvnw.cmd -q -pl common,nutrition-service,api-gateway -am compile`.
- Lan build dau bi chan do sandbox khong co quyen tai dependency Maven Central. Sau khi chay lai voi quyen network, compile PASS.
- Buoc 8 bat dau publish: may hien tai khong co GitHub CLI (`gh`), nen khong tao PR tu local duoc. Se commit va push bang `git`.
- Working tree co thay doi ngoai pham vi tai `doc/HuongDanCRUDMonAn_BE.md` va `doc/HuongDanPageTongQuanAdmin_BE.md`; cac file nay khong nam trong commit CRUD admin mon an.
- Da tao commit `d90fa48` voi message `feat(nutrition): add admin dish CRUD` tren branch `AdminTongQuanPage`.
- Da push branch `AdminTongQuanPage` len `origin` va set upstream `origin/AdminTongQuanPage`.
