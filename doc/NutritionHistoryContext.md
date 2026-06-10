# Nutrition History FE Context

## Muc tieu

Backend da bo sung contract cho man hinh lich su nutrition. FE co the doc lich su theo khoang ngay, hien thi ten mon trong tung bua, va cap nhat trang thai bua an da ton tai.

Pham vi BE nam trong `nutrition-service`, qua gateway route `/api/meal-log/**`.

## Auth va user id

Khi goi qua API Gateway, gateway se gan header `X-User-Id` tu JWT.

Khi test truc tiep `nutrition-service`, co the truyen header:

```http
userId: <USER_ID>
```

Controller hien van chap nhan ca `X-User-Id` va `userId` de giu tuong thich voi cach test cu.

## Endpoints

### GET `/api/meal-log/history`

Lay lich su gan day theo so ngay.

Query:

```text
days: number, optional, default 3, range 1..30
```

Response:

```json
{
  "success": true,
  "data": [
    {
      "id": "meal-log-id",
      "mealDate": "2026-06-10",
      "mealType": "SANG",
      "planType": "3_BUA",
      "goalCode": "GIAM",
      "mealKcalTarget": 500.00,
      "totalKcalActual": 486.50,
      "totalProtein": 28.00,
      "totalFat": 12.00,
      "totalCarb": 61.00,
      "finalScore": 92.50,
      "status": "SUGGESTED",
      "customNote": null,
      "dishes": [
        {
          "dishId": "dish-id",
          "dishName": "Com ga",
          "slotCode": "MAIN",
          "foodGroupCode": "PROTEIN",
          "servingMultiplier": 1.0,
          "actualGrams": 250.00,
          "dishKcal": 486.50,
          "favorite": false
        }
      ]
    }
  ],
  "errorCode": null,
  "message": null
}
```

### GET `/api/meal-log/history-range`

Lay lich su trong khoang ngay de FE dieu huong tuan/thang.

Query bat buoc:

```text
from: YYYY-MM-DD
to: YYYY-MM-DD
```

Validation:

```text
from <= to
Khoang ngay toi da 366 ngay
```

Response contract giong `/api/meal-log/history`.

Vi du:

```http
GET /api/meal-log/history-range?from=2026-06-01&to=2026-06-30
```

### PATCH `/api/meal-log/{id}/status`

Cap nhat trang thai cho mot bua da ton tai theo `mealLog.id`.

Body:

```json
{
  "status": "FOLLOWED",
  "customNote": null
}
```

Status BE nhan theo enum:

```text
SUGGESTED
FOLLOWED
CUSTOM
SKIPPED
MODIFIED
```

FE nen dung:

```text
FOLLOWED: user da an dung de xuat
CUSTOM: user an khac de xuat
SKIPPED: user bo bua
SUGGESTED: trang thai mac dinh hoac reset ve de xuat
```

`MODIFIED` co the ton tai trong DB/enum cu, nhung FE moi nen map ve `CUSTOM` khi hien thi neu gap.

`customNote` chi duoc luu khi `status = CUSTOM`. Khi cap nhat sang trang thai khac, BE se xoa `customNote`.

Response tra ve mot `MealLogHistoryResponse` da cap nhat, co day du `dishes` va `dishName`.

Loi chinh:

```text
404 MEALLOG-001: meal log id khong ton tai
403 AUTH-008: meal log thuoc user khac
400 COMMON-001: body validation fail, vi du thieu status hoac customNote qua 500 ky tu
```

## Hanh vi confirm meal

`POST /api/meal-log/confirm` van tao/cap nhat bua an theo ngay va loai bua nhu truoc.

Thay doi quan trong: neu bua da ton tai va user da danh dau `FOLLOWED`, `CUSTOM` hoac `SKIPPED`, confirm lai khong reset status ve `SUGGESTED`. BE chi cap nhat thong tin mon/macros cua bua.

## Field moi cho FE

`MealLogHistoryResponse` co them:

```text
customNote: string | null
```

Moi item trong `dishes` co them:

```text
dishName: string | null
```

`dishName` co the null neu `dishId` trong log khong con ton tai trong catalog, nhung case binh thuong se co ten mon.

## Luu y schema

BE them cot nullable:

```sql
meal_log.custom_note VARCHAR(500) NULL
```

Moi truong dev hien dung `spring.jpa.hibernate.ddl-auto=update`, nen cot nay se duoc them khi `nutrition-service` khoi dong. Neu deploy moi truong khong cho Hibernate tu update schema, can migration SQL tuong ung.
