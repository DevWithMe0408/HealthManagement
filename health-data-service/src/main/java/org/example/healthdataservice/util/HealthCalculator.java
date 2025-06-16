package org.example.healthdataservice.util;

import org.springframework.stereotype.Component;

@Component
public class HealthCalculator {

    public Double calculateBMI(Double heightCm, Double weightKg) {
        if (heightCm == null || weightKg == null ||heightCm <= 0 || weightKg <= 0) {
            return null;
        }
        double heightM = heightCm / 100.0;
        return weightKg / (heightM * heightM);
    }
    // Mifflin-St Jeor Equation
    public Double calculateBMR(String gender, Double weightKg, Double heightCm, Double ageYears) {
        if (gender == null || weightKg == null || heightCm == null || ageYears == null ||
        weightKg <= 0 || heightCm <= 0 || ageYears <= 0) {
            return null;
        }
        if (gender.equalsIgnoreCase("male")) {
            return (10 * weightKg) + (6.25 * heightCm) - (5 * ageYears) + 5;
        } else if (gender.equalsIgnoreCase("female")) {
            return (10 * weightKg) + (6.25 * heightCm) - (5 * ageYears) - 161;
        } else {
            return null; // Invalid
        }
    }

    public Double calculateTDEE(Double activityFactor, Double bmr) {
        if (bmr == null || activityFactor == null || activityFactor <= 0) {
            return null;
        }
        return bmr * activityFactor;
    }

    // U.S. Navy Body Fat Formula (cần tuổi)
    public Double calculatePBF(String gender, Double waistCm, Double hipCm, Double neckCm,
                               Double heightCm,Double ageYears) {
        if (gender == null || waistCm == null || heightCm == null || neckCm == null || ageYears == null ||
                waistCm <=0 || heightCm <=0 || neckCm <=0 || ageYears <=0 ) {
            return null;
        }
        if (gender.equalsIgnoreCase("male")) {
            // Công thức cho nam (ví dụ)
            // log10(waist - neck) và log10(height)
            if (waistCm - neckCm <= 0) return null; // Tránh log của số không dương
            return 495 / (1.0324 - 0.19077 * Math.log10(waistCm - neckCm) + 0.15456 * Math.log10(heightCm)) - 450;
        } else if (gender.equalsIgnoreCase("female")) {
            if (hipCm == null || hipCm <=0) return null;
            // Công thức cho nữ (ví dụ)
            // log10(waist + hip - neck) và log10(height)
            if (waistCm + hipCm - neckCm <= 0) return null; // Tránh log của số không dương
            return 495 / (1.29579 - 0.35004 * Math.log10(waistCm + hipCm - neckCm) + 0.22100 * Math.log10(heightCm)) - 450;
        }
        return null;
    }
    public Double calculateWHR(Double waistCm, Double hipCm) {
        if (waistCm == null || hipCm == null || hipCm <= 0 || waistCm <= 0) {
            return null;
        }
        return waistCm / hipCm;
    }

}
