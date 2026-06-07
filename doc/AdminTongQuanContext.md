# Admin Tong Quan - BE Context

## Muc tieu

Xay BE cho trang Admin Tong quan tang 1. Trang FE se goi 2 endpoint doc-thuan va tu ghep du lieu:

- `GET /api/admin/dashboard/catalog-stats` tu `nutrition-service`.
- `GET /api/admin/users/stats` tu `user-service`.

Khong tao aggregator service goi cheo giua cac service.

## Quyet dinh da chot

- `usersWithProfile` se duoc tinh bang dieu kien:
  `birthDate IS NOT NULL AND gender IS NOT NULL`.
- Khong dung field `profileCompleted` cho dashboard nay vi hien tai field do la workflow flag, chi duoc set true qua endpoint rieng `/api/user/profile-completed`, khong tu dong sync khi cap nhat profile.
- Cach tinh nay giu nhat quan voi man Admin Danh sach nguoi dung hien tai.

## API contract du kien

### Catalog stats

Endpoint:

```http
GET /api/admin/dashboard/catalog-stats
```

Response:

```json
{
  "code": null,
  "message": "Success",
  "data": {
    "dishTotal": 142,
    "dishActive": 138,
    "ingredientTotal": 203,
    "ingredientWithMacro": 151,
    "mealLogTotal": 1284,
    "dishCountBySlot": {
      "CHINH": 45,
      "RAU": 33,
      "TINH_BOT": 20,
      "COMBO": 32,
      "BUA_PHU": 12
    }
  }
}
```

Ghi chu:

- `dishCountBySlot` chi dem mon active.
- `dishCountBySlot` luon co du 5 key theo enum `SlotCode`.
- Tong gia tri trong `dishCountBySlot` phai bang `dishActive`.

### User stats

Endpoint:

```http
GET /api/admin/users/stats
```

Response:

```json
{
  "code": null,
  "message": "Success",
  "data": {
    "totalUsers": 48,
    "usersWithProfile": 31
  }
}
```

Ghi chu:

- `usersWithProfile` = so user co `birthDate` va `gender`.
- Can dam bao `totalUsers >= usersWithProfile >= 0`.

## Bao mat va gateway

- Cac endpoint admin dung `@PreAuthorize("hasRole('ADMIN')")`.
- Gateway phai gan `JwtFilter` cho route moi `/api/admin/dashboard/**`.
- Route `/api/admin/users/**` da co san, nen `/api/admin/users/stats` khong can route rieng.

## Checklist thuc hien

1. Nutrition service:
   - Tao `IngredientRepository`.
   - Bo sung `DishRepository.countByIsActiveTrue()`.
   - Bo sung `DishRepository.countActiveBySlot()`.
   - Tao `CatalogStatsResponse`.
   - Tao `DashboardStatsService`.
   - Tao `AdminDashboardController`.

2. User service:
   - Bo sung `UserRepository.countWithProfile()`.
   - Tao `UserStatsResponse`.
   - Them `GET /stats` vao `AdminUserController`.

3. Gateway:
   - Them route `admin-dashboard` cho `/api/admin/dashboard/**` ve `lb://nutrition-service`.

4. Verify:
   - Chay compile cac module: `common,nutrition-service,user-service,api-gateway`.
   - Runtime verify qua gateway voi ADMIN token.
   - Verify request khong co ADMIN token tra `401/403`.

## Trang thai thuc hien

- 2026-06-07: Da danh gia kha thi. Compile baseline cac module lien quan thanh cong sau khi cho phep Maven tai dependency.
- 2026-06-07: Da chot khong dung `profileCompleted`; dung `birthDate IS NOT NULL AND gender IS NOT NULL`.
- 2026-06-07: Tao file context nay.
- 2026-06-07: Da tao `IngredientRepository` va bo sung cac method thong ke trong `DishRepository`.
- 2026-06-07: Compile `common,nutrition-service` thanh cong sau buoc repository.
- 2026-06-07: Da tao `CatalogStatsResponse`, `DashboardStatsService`, va `AdminDashboardController` cho endpoint catalog-stats.
- 2026-06-07: Compile `common,nutrition-service` thanh cong sau buoc catalog-stats.
- 2026-06-07: Da tao `UserStatsResponse`, bo sung `UserRepository.countWithProfile()`, va them endpoint `/api/admin/users/stats`.
- 2026-06-07: Compile `common,user-service` thanh cong sau buoc user-stats.

## Luu y cho FE

- FE can goi 2 endpoint rieng va ghep du lieu o client.
- FE khong nen ky vong co endpoint tong hop dashboard gom ca user va nutrition trong cung response.
- FE nen xu ly truong hop slot co count bang 0, nhung BE se van tra du 5 key.
- FE nen hien thi user profile completion theo cung logic BE: `usersWithProfile / totalUsers`.
