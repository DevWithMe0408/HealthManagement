package org.example.healthdataservice.service;

import org.example.healthdataservice.dto.response.ConstitutionResponse;
import org.example.healthdataservice.entity.CalculatedMetricSnapshot;
import org.example.healthdataservice.entity.UserForHealthData;
import org.example.healthdataservice.entity.enums.Gender;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.util.BodyClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BodyClassificationServiceImplTest {

    private static final String USER_ID = "user-1";

    @Mock
    private CalculatedMetricService calculatedMetricService;

    @Mock
    private UserProfileMirrorService userProfileMirrorService;

    @Mock
    private UserPreferenceMirrorService userPreferenceMirrorService;

    private BodyClassificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BodyClassificationServiceImpl(
                calculatedMetricService,
                userProfileMirrorService,
                userPreferenceMirrorService,
                new BodyClassifier()
        );
    }

    @Test
    void classifyCurrentUsesFormulaWhenPreferenceIsFormula() {
        givenGender(Gender.MALE);
        givenBmi(22.0);
        when(userPreferenceMirrorService.getValueOrDefault(USER_ID, "pbf_method", "FORMULA"))
                .thenReturn("FORMULA");
        when(calculatedMetricService.getLatestSnapshotByMethod(USER_ID, IndicatorType.PBF, "FORMULA"))
                .thenReturn(Optional.of(snapshot(IndicatorType.PBF, 24.0)));
        when(calculatedMetricService.getLatestSnapshotByMethod(USER_ID, IndicatorType.PBF, "MODEL_1"))
                .thenReturn(Optional.of(snapshot(IndicatorType.PBF, 31.0)));

        ConstitutionResponse response = service.classifyCurrent(USER_ID);

        assertThat(response.getPbf()).isEqualTo(24.0);
        assertThat(response.getPbfFormula()).isEqualTo(24.0);
        assertThat(response.getPbfModel()).isEqualTo(31.0);
        assertThat(response.getPbfSource()).isEqualTo("FORMULA");
        assertThat(response.getWarning()).isNull();
    }

    @Test
    void classifyCurrentUsesModelWhenPreferenceIsModelAndModelSnapshotExists() {
        givenGender(Gender.MALE);
        givenBmi(22.0);
        when(userPreferenceMirrorService.getValueOrDefault(USER_ID, "pbf_method", "FORMULA"))
                .thenReturn("MODEL_1");
        when(calculatedMetricService.getLatestSnapshotByMethod(USER_ID, IndicatorType.PBF, "FORMULA"))
                .thenReturn(Optional.of(snapshot(IndicatorType.PBF, 24.0)));
        when(calculatedMetricService.getLatestSnapshotByMethod(USER_ID, IndicatorType.PBF, "MODEL_1"))
                .thenReturn(Optional.of(snapshot(IndicatorType.PBF, 31.0)));

        ConstitutionResponse response = service.classifyCurrent(USER_ID);

        assertThat(response.getPbf()).isEqualTo(31.0);
        assertThat(response.getPbfFormula()).isEqualTo(24.0);
        assertThat(response.getPbfModel()).isEqualTo(31.0);
        assertThat(response.getPbfSource()).isEqualTo("MODEL_1");
        assertThat(response.getWarning()).isNull();
    }

    @Test
    void classifyCurrentFallsBackToFormulaWhenModelIsPreferredButUnavailable() {
        givenGender(Gender.MALE);
        givenBmi(22.0);
        when(userPreferenceMirrorService.getValueOrDefault(USER_ID, "pbf_method", "FORMULA"))
                .thenReturn("MODEL_1");
        when(calculatedMetricService.getLatestSnapshotByMethod(USER_ID, IndicatorType.PBF, "FORMULA"))
                .thenReturn(Optional.of(snapshot(IndicatorType.PBF, 24.0)));
        when(calculatedMetricService.getLatestSnapshotByMethod(USER_ID, IndicatorType.PBF, "MODEL_1"))
                .thenReturn(Optional.empty());

        ConstitutionResponse response = service.classifyCurrent(USER_ID);

        assertThat(response.getPbf()).isEqualTo(24.0);
        assertThat(response.getPbfFormula()).isEqualTo(24.0);
        assertThat(response.getPbfModel()).isNull();
        assertThat(response.getPbfSource()).isEqualTo("FORMULA");
        assertThat(response.getWarning()).isEqualTo("Model AI chua san sang, dung cong thuc Navy");
    }

    private void givenGender(Gender gender) {
        UserForHealthData profile = new UserForHealthData();
        profile.setUserId(USER_ID);
        profile.setGender(gender);
        profile.setLastUpdatedAt(LocalDateTime.now());
        when(userProfileMirrorService.getUserProfile(USER_ID)).thenReturn(Optional.of(profile));
    }

    private void givenBmi(Double value) {
        when(calculatedMetricService.getLatestSnapshot(USER_ID, IndicatorType.BMI))
                .thenReturn(Optional.of(snapshot(IndicatorType.BMI, value)));
    }

    private CalculatedMetricSnapshot snapshot(IndicatorType type, Double value) {
        CalculatedMetricSnapshot snapshot = new CalculatedMetricSnapshot();
        snapshot.setUserId(USER_ID);
        snapshot.setIndicatorType(type);
        snapshot.setValue(value);
        snapshot.setCalculatedAt(LocalDateTime.now());
        return snapshot;
    }
}
