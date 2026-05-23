package org.example.nutritionservice.entity.meallog;

public enum MealStatus {
    SUGGESTED, // mặc định khi gen
    FOLLOWED, // user xác nhận ã ăn đúng đề xuất
    MODIFIED, // user ăn nhưng sửa serving
    CUSTOM, // user tự nhập món khác hoàn toàn
    SKIPPED // user bỏ bữa
}
