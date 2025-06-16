package org.example.healthdataservice.entity.enums;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
public enum IndicatorType {
    HEIGHT(IndicatorCategory.BASE,"Chiều cao","cm", MeasurementFrequency.YEARLY), // chiều cao
    WEIGHT(IndicatorCategory.BASE, "Cân nặng", "kg", MeasurementFrequency.WEEKLY), // cân nặng
    WAIST(IndicatorCategory.BASE, "Vòng eo", "cm",MeasurementFrequency.MONTHLY),
    HIP(IndicatorCategory.BASE, "Vòng hông", "cm",MeasurementFrequency.MONTHLY),
    NECK(IndicatorCategory.BASE, "Vòng cổ", "cm", MeasurementFrequency.MONTHLY),
    BUST(IndicatorCategory.BASE, "Vòng ngực", "cm",MeasurementFrequency.MONTHLY), // Giả sử bạn có chỉ số này
    ACTIVITY_FACTOR(IndicatorCategory.BASE, "Hệ số vận động", null,MeasurementFrequency.ON_CHANGE), // Không có unit cụ thể

    // Có thể thêm các chỉ số cơ bản khác sau này:
    // BLOOD_GLUCOSE(IndicatorCategory.BASE, "Đường huyết", "mg/dL"),
    // SYSTOLIC_BP(IndicatorCategory.BASE, "Huyết áp tâm thu", "mmHg"),
    // DIASTOLIC_BP(IndicatorCategory.BASE, "Huyết áp tâm trương", "mmHg"),

    // --- Chỉ số Tính toán (CALCULATED hoặc USER_PROVIDED_CALCULATED) ---
    // Mặc định là CALCULATED, nhưng người dùng có thể nhập nên khi lưu sẽ xác định category
    BMI(IndicatorCategory.CALCULATED, "Chỉ số khối cơ thể", "kg/m²",MeasurementFrequency.MONTHLY),
    BMR(IndicatorCategory.CALCULATED, "Tỷ lệ trao đổi chất cơ bản", "kcal/day",MeasurementFrequency.MONTHLY),
    TDEE(IndicatorCategory.CALCULATED, "Tổng năng lượng tiêu thụ hàng ngày", "kcal/day",MeasurementFrequency.MONTHLY),
    PBF(IndicatorCategory.CALCULATED, "Tỷ lệ mỡ cơ thể", "%",MeasurementFrequency.MONTHLY),
    WHR(IndicatorCategory.CALCULATED, "Tỷ lệ eo trên hông", null,MeasurementFrequency.MONTHLY);


    private final IndicatorCategory category;
    private final String defaultDisplayName;
    private final String defaultUnitCode; // Mã của đơn vị mặc định (từ bảng units)
    private final MeasurementFrequency defaultFrequency; // Tần suất đo mặc định

    IndicatorType(IndicatorCategory category, String defaultDisplayName, String defaultUnitCode, MeasurementFrequency defaultFrequency) {
        this.category = category;
        this.defaultDisplayName = defaultDisplayName;
        this.defaultUnitCode = defaultUnitCode;
        this.defaultFrequency = defaultFrequency;
    }
    public boolean isBaseMetric() {
        return this.category == IndicatorCategory.BASE;
    }
    public boolean isCalculatedMetric() {
        return this.category == IndicatorCategory.CALCULATED || this.category ==
                IndicatorCategory.USER_PROVIDED_CALCULATED;
    }
}
