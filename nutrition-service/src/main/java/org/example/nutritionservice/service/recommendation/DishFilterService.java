package org.example.nutritionservice.service.recommendation;

import org.example.nutritionservice.domain.recommendation.DishCandidate;
import org.example.nutritionservice.domain.recommendation.LoadedConfigs;
import org.example.nutritionservice.entity.catalog.Dish;
import org.example.nutritionservice.entity.catalog.SlotCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class DishFilterService {
    /**
     * filter ứng viên cho 1 slot
     * kcal_tolerance - biên chấp nhận của kcal lấy ra
     * weight constraint - điều kiện cho khối lượng món ăn
     */

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int CALC_SCALE = 4;

    /**
     * Filter ứng viên cho từng slot
     * @param slot Loại của món ăn: CHINH/RAU/TINH_BOT/COMBO/BUA_PHU
     * @param slotKcalTarget Mức kcal mục tiêu cho slot đó
     * @param configs cấu hình
     * @param allActiveDishesInSlot Danh sách món ăn lấy từ DB
     * @return
     */

    public List<DishCandidate> filterCandidatesForSlot(
            SlotCode slot,
            BigDecimal slotKcalTarget,
            LoadedConfigs configs,
            List<Dish> allActiveDishesInSlot)
    {
        BigDecimal tolerance = configs.getDecimal("filter.kcal_tolerance"); // Biên độ dao động kcal cho phép
        BigDecimal minServing = configs.getDecimal("filter.serving_min"); // Hệ số serving nhỏ nhất
        BigDecimal maxServing = configs.getDecimal("filter.serving_max"); // Hệ sô serving lớn nhất
        // Tính khoảng kcal chấp nhận được
        BigDecimal maxAcceptedMin = slotKcalTarget.multiply(BigDecimal.ONE.add(tolerance));
        BigDecimal minAcceptedMax = slotKcalTarget.multiply(BigDecimal.ONE.subtract(tolerance));

        return allActiveDishesInSlot.stream()
                .filter(dish -> dish.getSlotCode() == slot && Boolean.TRUE.equals(dish.getIsActive())) // Món thuộc slot cần lọc && Món active
                .map(this::toCandidate) // Chuyển Dish thành DishCandidate
                .filter(candidate -> candidate.getBaseKcal().multiply(minServing).compareTo(maxAcceptedMin) <= 0)// baseKcal * minServing <= target * (1 + tolerance)
                .filter(candidate -> candidate.getBaseKcal().multiply(maxServing).compareTo(minAcceptedMax) >= 0)// baseKcal * maxServing >= target * (1 - tolerance)
                .toList();
    }
    // Logic filter cho mỗi dish D:
    // 1. base_kcal_at_serving = D.kcal_per_100g × D.base_serving_g / 100
    // 2. kcal_at_min = base_kcal_at_serving × serving_min_multiplier
    // 3. kcal_at_max = base_kcal_at_serving × serving_max_multiplier
    // 4. pass = (kcal_at_min ≤ target × 1.15) AND (kcal_at_max ≥ target × 0.85) -> Khi dùng khẩu phần nhỏ nhất, kcal của món không được vượt quá ngưỡng trên, Khi dùng khẩu phần lớn nhất, kcal của món phải đạt ít nhất ngưỡng dưới
    // 5. weight constraint (§3.6 tuyệt đối): check ở scoring phase, không ở đây
    //    (vì filter ở đây chỉ loại dish KHÔNG THỂ vừa, sau brute force mới kiểm serving cụ thể)

    public DishCandidate toCandidate(Dish dish) {
        BigDecimal baseServingRatio = BigDecimal.valueOf(dish.getBaseServingG())
                .divide(ONE_HUNDRED, CALC_SCALE, RoundingMode.HALF_UP);
        return DishCandidate.builder()
                .dish(dish)
                .baseKcal(dish.getKcalPer100g().multiply(baseServingRatio).setScale(CALC_SCALE, RoundingMode.HALF_UP))
                .baseProteinG(dish.getProteinPer100g().multiply(baseServingRatio).setScale(CALC_SCALE, RoundingMode.HALF_UP))
                .baseFatG(dish.getFatPer100g().multiply(baseServingRatio).setScale(CALC_SCALE, RoundingMode.HALF_UP))
                .baseCarbG(dish.getCarbPer100g().multiply(baseServingRatio).setScale(CALC_SCALE, RoundingMode.HALF_UP))
                .build();
    }
}
