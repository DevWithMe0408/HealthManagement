package org.example.healthdataservice.service;

import org.example.healthdataservice.client.Model1PbfClient;
import org.example.healthdataservice.dto.ml.PbfPredictRequest;
import org.example.healthdataservice.entity.BaseMetricValue;
import org.example.healthdataservice.entity.CalculatedMetricSnapshot;
import org.example.healthdataservice.entity.UserForHealthData;
import org.example.healthdataservice.entity.enums.Gender;
import org.example.healthdataservice.entity.enums.IndicatorCategory;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.repository.CalculatedMetricSnapshotRepository;
import org.example.healthdataservice.repository.UnitRepository;
import org.example.healthdataservice.util.HealthCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculatedMetricServiceImplTest {

    private static final String USER_ID = "user-1";

    @Mock
    private CalculatedMetricSnapshotRepository snapshotRepository;

    @Mock
    private BaseMetricService baseMetricService;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private UserProfileMirrorService userProfileMirrorService;

    @Mock
    private Model1PbfClient model1PbfClient;

    private CalculatedMetricServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CalculatedMetricServiceImpl(
                snapshotRepository,
                baseMetricService,
                unitRepository,
                new HealthCalculator(),
                userProfileMirrorService,
                model1PbfClient
        );
    }

    @Test
    void predictAndSaveModel1PbfSavesModelSnapshotWhenRequiredInputsExist() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 4, 9, 30);
        when(userProfileMirrorService.getUserProfile(USER_ID)).thenReturn(Optional.of(profile(Gender.MALE)));
        when(baseMetricService.getLatestBaseMetrics(eq(USER_ID), any(Set.class))).thenReturn(completeBaseMetrics());
        when(model1PbfClient.predictPbf(any(PbfPredictRequest.class))).thenReturn(21.5);
        when(unitRepository.findByCode("%")).thenReturn(Optional.empty());

        service.predictAndSaveModel1Pbf(USER_ID, now);

        ArgumentCaptor<PbfPredictRequest> requestCaptor = ArgumentCaptor.forClass(PbfPredictRequest.class);
        verify(model1PbfClient).predictPbf(requestCaptor.capture());
        PbfPredictRequest request = requestCaptor.getValue();
        assertThat(request.getSexM()).isEqualTo(1);
        assertThat(request.getAge()).isEqualTo(30.0);
        assertThat(request.getWeight()).isEqualTo(70.0);
        assertThat(request.getHeight()).isEqualTo(175.0);
        assertThat(request.getNeck()).isEqualTo(38.0);
        assertThat(request.getChest()).isEqualTo(95.0);
        assertThat(request.getAbdomen()).isEqualTo(84.0);
        assertThat(request.getHip()).isEqualTo(90.0);
        assertThat(request.getThigh()).isEqualTo(55.0);

        ArgumentCaptor<CalculatedMetricSnapshot> snapshotCaptor = ArgumentCaptor.forClass(CalculatedMetricSnapshot.class);
        verify(snapshotRepository).save(snapshotCaptor.capture());
        CalculatedMetricSnapshot snapshot = snapshotCaptor.getValue();
        assertThat(snapshot.getUserId()).isEqualTo(USER_ID);
        assertThat(snapshot.getIndicatorType()).isEqualTo(IndicatorType.PBF);
        assertThat(snapshot.getValue()).isEqualTo(21.5);
        assertThat(snapshot.getCalculatedAt()).isEqualTo(now);
        assertThat(snapshot.getSourceCategory()).isEqualTo(IndicatorCategory.CALCULATED);
        assertThat(snapshot.getMethod()).isEqualTo("MODEL_1");
    }

    @Test
    void predictAndSaveModel1PbfSkipsWhenThighIsMissing() {
        Map<IndicatorType, BaseMetricValue> metrics = completeBaseMetrics();
        metrics.remove(IndicatorType.THIGH);
        when(userProfileMirrorService.getUserProfile(USER_ID)).thenReturn(Optional.of(profile(Gender.FEMALE)));
        when(baseMetricService.getLatestBaseMetrics(eq(USER_ID), any(Set.class))).thenReturn(metrics);

        service.predictAndSaveModel1Pbf(USER_ID, LocalDateTime.now());

        verifyNoInteractions(model1PbfClient);
        verify(snapshotRepository, never()).save(any(CalculatedMetricSnapshot.class));
        verifyNoInteractions(unitRepository);
    }

    @Test
    void predictAndSaveModel1PbfSkipsWhenProfileIsMissing() {
        when(userProfileMirrorService.getUserProfile(USER_ID)).thenReturn(Optional.empty());

        service.predictAndSaveModel1Pbf(USER_ID, LocalDateTime.now());

        verifyNoInteractions(baseMetricService, model1PbfClient, unitRepository);
        verify(snapshotRepository, never()).save(any(CalculatedMetricSnapshot.class));
    }

    private UserForHealthData profile(Gender gender) {
        UserForHealthData profile = new UserForHealthData();
        profile.setUserId(USER_ID);
        profile.setGender(gender);
        profile.setBirthDate(LocalDate.now().minusYears(30));
        profile.setLastUpdatedAt(LocalDateTime.now());
        return profile;
    }

    private Map<IndicatorType, BaseMetricValue> completeBaseMetrics() {
        Map<IndicatorType, BaseMetricValue> metrics = new EnumMap<>(IndicatorType.class);
        metrics.put(IndicatorType.WEIGHT, baseMetric(IndicatorType.WEIGHT, 70.0));
        metrics.put(IndicatorType.HEIGHT, baseMetric(IndicatorType.HEIGHT, 175.0));
        metrics.put(IndicatorType.NECK, baseMetric(IndicatorType.NECK, 38.0));
        metrics.put(IndicatorType.BUST, baseMetric(IndicatorType.BUST, 95.0));
        metrics.put(IndicatorType.ABDOMEN, baseMetric(IndicatorType.ABDOMEN, 84.0));
        metrics.put(IndicatorType.HIP, baseMetric(IndicatorType.HIP, 90.0));
        metrics.put(IndicatorType.THIGH, baseMetric(IndicatorType.THIGH, 55.0));
        return metrics;
    }

    private BaseMetricValue baseMetric(IndicatorType type, Double value) {
        BaseMetricValue metric = new BaseMetricValue();
        metric.setUserId(USER_ID);
        metric.setIndicatorType(type);
        metric.setValue(value);
        metric.setRecordedAt(LocalDateTime.now());
        return metric;
    }
}
