package org.example.healthdataservice.util;

import org.springframework.stereotype.Component;

@Component
public class HealthCalculator {

    public Double calculateBMI(Double heightCm, Double weightKg) {
        if (heightCm == null || weightKg == null || heightCm <= 0 || weightKg <= 0) {
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
        }
        return null;
    }

    public Double calculateTDEE(Double activityFactor, Double bmr) {
        if (bmr == null || activityFactor == null || activityFactor <= 0) {
            return null;
        }
        return bmr * activityFactor;
    }

    // U.S. Navy Body Fat Formula. The formula does not use age; ageYears is kept for API compatibility.
    public Double calculatePBF(String gender, Double abdomenCm, Double hipCm, Double neckCm,
                               Double heightCm, Double ageYears) {
        if (gender == null || abdomenCm == null || heightCm == null || neckCm == null ||
                abdomenCm <= 0 || heightCm <= 0 || neckCm <= 0) {
            return null;
        }
        if (gender.equalsIgnoreCase("male")) {
            if (abdomenCm - neckCm <= 0) {
                return null;
            }
            return 495 / (1.0324 - 0.19077 * Math.log10(abdomenCm - neckCm) + 0.15456 * Math.log10(heightCm)) - 450;
        } else if (gender.equalsIgnoreCase("female")) {
            if (hipCm == null || hipCm <= 0 || abdomenCm + hipCm - neckCm <= 0) {
                return null;
            }
            return 495 / (1.29579 - 0.35004 * Math.log10(abdomenCm + hipCm - neckCm) + 0.22100 * Math.log10(heightCm)) - 450;
        }
        return null;
    }

    public Double calculateWHR(Double abdomenCm, Double hipCm) {
        if (abdomenCm == null || hipCm == null || hipCm <= 0 || abdomenCm <= 0) {
            return null;
        }
        return abdomenCm / hipCm;
    }
}
