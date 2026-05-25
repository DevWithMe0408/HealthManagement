package org.example.healthdataservice.util;

import org.example.healthdataservice.entity.enums.Gender;
import org.springframework.stereotype.Component;

@Component
public class BodyClassifier {

    private static final double BMI_UNDERWEIGHT = 18.5;
    private static final double BMI_NORMAL_HIGH = 23.0;
    private static final double BMI_OVERWEIGHT_HIGH = 25.0;

    private static final double PBF_MALE_LOW = 8.0;
    private static final double PBF_MALE_NORMAL_HIGH = 25.0;
    private static final double PBF_MALE_OVERWEIGHT_HIGH = 30.0;

    private static final double PBF_FEMALE_LOW = 21.0;
    private static final double PBF_FEMALE_NORMAL_HIGH = 36.0;
    private static final double PBF_FEMALE_OVERWEIGHT_HIGH = 42.0;

    private static final String[] CONSTITUTION_NAMES = {
            "GAY", "CAN_DOI", "THUA_CAN", "BEO_PHI"
    };

    public Integer classifyByBmi(Double bmi) {
        if (bmi == null) {
            return null;
        }
        if (bmi < BMI_UNDERWEIGHT) {
            return 0;
        }
        if (bmi < BMI_NORMAL_HIGH) {
            return 1;
        }
        if (bmi < BMI_OVERWEIGHT_HIGH) {
            return 2;
        }
        return 3;
    }

    public Integer classifyByPbf(Double pbf, Gender gender) {
        if (pbf == null || gender == null) {
            return null;
        }

        if (gender == Gender.FEMALE) {
            if (pbf < PBF_FEMALE_LOW) {
                return 0;
            }
            if (pbf < PBF_FEMALE_NORMAL_HIGH) {
                return 1;
            }
            if (pbf < PBF_FEMALE_OVERWEIGHT_HIGH) {
                return 2;
            }
            return 3;
        }

        if (gender == Gender.MALE) {
            if (pbf < PBF_MALE_LOW) {
                return 0;
            }
            if (pbf < PBF_MALE_NORMAL_HIGH) {
                return 1;
            }
            if (pbf < PBF_MALE_OVERWEIGHT_HIGH) {
                return 2;
            }
            return 3;
        }

        return null;
    }

    public Integer classifyFinal(Integer bmiClass, Integer pbfClass) {
        if (bmiClass == null) {
            return null;
        }
        if (pbfClass == null) {
            return bmiClass;
        }
        return Math.max(bmiClass, pbfClass);
    }

    public String classNameOf(Integer classIndex) {
        if (classIndex == null || classIndex < 0 || classIndex >= CONSTITUTION_NAMES.length) {
            return null;
        }
        return CONSTITUTION_NAMES[classIndex];
    }
}
