package org.example.healthdataservice.service;

import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.dto.response.ConstitutionResponse;
import org.example.healthdataservice.entity.CalculatedMetricSnapshot;
import org.example.healthdataservice.entity.UserForHealthData;
import org.example.healthdataservice.entity.enums.Gender;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.util.BodyClassifier;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BodyClassificationServiceImpl implements BodyClassificationService {

    private static final String PBF_METHOD_KEY = "pbf_method";
    private static final String PBF_METHOD_FORMULA = "FORMULA";
    private static final String PBF_METHOD_MODEL_1 = "MODEL_1";

    private final CalculatedMetricService calculatedMetricService;
    private final UserProfileMirrorService userProfileMirrorService;
    private final UserPreferenceMirrorService userPreferenceMirrorService;
    private final BodyClassifier bodyClassifier;

    @Override
    public ConstitutionResponse classifyCurrent(String userId) {
        Gender gender = getRequiredGender(userId);

        CalculatedMetricSnapshot bmiSnapshot = calculatedMetricService
                .getLatestSnapshot(userId, IndicatorType.BMI)
                .orElseThrow(() -> new BusinessException(ErrorCode.HEALTH_MISSING_BASIC_DATA));
        Double bmi = bmiSnapshot.getValue();

        String requestedPbfMethod = normalizePbfMethod(userPreferenceMirrorService.getValueOrDefault(
                userId,
                PBF_METHOD_KEY,
                PBF_METHOD_FORMULA
        ));

        Optional<CalculatedMetricSnapshot> pbfFormulaSnapshot = calculatedMetricService
                .getLatestSnapshotByMethod(userId, IndicatorType.PBF, PBF_METHOD_FORMULA);
        Optional<CalculatedMetricSnapshot> pbfModelSnapshot = calculatedMetricService
                .getLatestSnapshotByMethod(userId, IndicatorType.PBF, PBF_METHOD_MODEL_1);

        Optional<CalculatedMetricSnapshot> activePbfSnapshot = PBF_METHOD_MODEL_1.equals(requestedPbfMethod)
                ? pbfModelSnapshot
                : pbfFormulaSnapshot;
        String activePbfMethod = requestedPbfMethod;
        String warning = null;

        if (PBF_METHOD_MODEL_1.equals(requestedPbfMethod) && activePbfSnapshot.isEmpty() && pbfFormulaSnapshot.isPresent()) {
            activePbfSnapshot = pbfFormulaSnapshot;
            activePbfMethod = PBF_METHOD_FORMULA;
            warning = "Model AI chua san sang, dung cong thuc Navy";
        }

        Double pbf = activePbfSnapshot.map(CalculatedMetricSnapshot::getValue).orElse(null);
        Double pbfFormula = pbfFormulaSnapshot.map(CalculatedMetricSnapshot::getValue).orElse(null);
        Double pbfModel = pbfModelSnapshot.map(CalculatedMetricSnapshot::getValue).orElse(null);

        Integer bmiClass = bodyClassifier.classifyByBmi(bmi);
        Integer pbfClass = bodyClassifier.classifyByPbf(pbf, gender);
        Integer finalClass = bodyClassifier.classifyFinal(bmiClass, pbfClass);

        return ConstitutionResponse.builder()
                .constitution(bodyClassifier.classNameOf(finalClass))
                .method("RULE_BMI_PBF")
                .bmi(bmi)
                .pbf(pbf)
                .pbfFormula(pbfFormula)
                .pbfModel(pbfModel)
                .pbfSource(pbf == null ? null : activePbfMethod)
                .bmiClass(bmiClass)
                .pbfClass(pbfClass)
                .finalClass(finalClass)
                .suggestedGoal(suggestGoalForConstitution(finalClass))
                .warning(warning != null ? warning : (pbf == null ? buildMissingPbfWarning(gender) : null))
                .computedAt(LocalDateTime.now())
                .build();
    }

    private String normalizePbfMethod(String pbfMethod) {
        if (PBF_METHOD_MODEL_1.equals(pbfMethod)) {
            return PBF_METHOD_MODEL_1;
        }
        return PBF_METHOD_FORMULA;
    }

    private Gender getRequiredGender(String userId) {
        Optional<UserForHealthData> profileOpt = userProfileMirrorService.getUserProfile(userId);
        if (profileOpt.isEmpty() || profileOpt.get().getGender() == null) {
            throw new BusinessException(ErrorCode.HEALTH_MISSING_GENDER);
        }
        return profileOpt.get().getGender();
    }

    private String buildMissingPbfWarning(Gender gender) {
        if (gender == Gender.FEMALE) {
            return "Thieu vong bung, co, hoac hong - chi phan loai theo BMI";
        }
        return "Thieu vong bung hoac co - chi phan loai theo BMI";
    }

    private String suggestGoalForConstitution(Integer finalClass) {
        if (finalClass == null) {
            return null;
        }
        return switch (finalClass) {
            case 0 -> "TANG";
            case 1 -> "DUY_TRI";
            case 2, 3 -> "GIAM";
            default -> null;
        };
    }
}
