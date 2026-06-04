package org.example.healthdataservice.service;

import org.example.healthdataservice.dto.request.SubmitHealthDataRequest;
import org.example.healthdataservice.entity.BaseMetricValue;
import org.example.healthdataservice.entity.Unit;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthDataSubmitServiceImplTest {

    private static final String USER_ID = "user-1";

    @Mock
    private BaseMetricService baseMetricService;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private CalculatedMetricService calculatedMetricService;

    private HealthDataSubmitServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HealthDataSubmitServiceImpl(baseMetricService, unitRepository, calculatedMetricService);
    }

    @Test
    void processSubmittedHealthDataDoesNotFailWhenModel1PredictionFails() {
        SubmitHealthDataRequest request = requestWithWeight();
        when(unitRepository.findByCode("kg")).thenReturn(Optional.empty());
        when(baseMetricService.saveBaseMetricIfChanged(
                eq(USER_ID),
                eq(IndicatorType.WEIGHT),
                eq(70.0),
                nullable(Unit.class),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(baseMetric(IndicatorType.WEIGHT, 70.0)));
        org.mockito.Mockito.doThrow(new RuntimeException("model timeout"))
                .when(calculatedMetricService)
                .predictAndSaveModel1Pbf(eq(USER_ID), any(LocalDateTime.class));

        assertThatCode(() -> service.processSubmittedHealthData(request)).doesNotThrowAnyException();

        InOrder inOrder = inOrder(calculatedMetricService);
        inOrder.verify(calculatedMetricService).recalculateAndSaveDerivedMetrics(eq(USER_ID), any(Set.class));
        inOrder.verify(calculatedMetricService).predictAndSaveModel1Pbf(eq(USER_ID), any(LocalDateTime.class));
    }

    @Test
    void processSubmittedHealthDataSkipsRecalculationAndModelWhenBaseMetricDoesNotChange() {
        SubmitHealthDataRequest request = requestWithWeight();
        when(unitRepository.findByCode("kg")).thenReturn(Optional.empty());
        when(baseMetricService.saveBaseMetricIfChanged(
                eq(USER_ID),
                eq(IndicatorType.WEIGHT),
                eq(70.0),
                nullable(Unit.class),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        service.processSubmittedHealthData(request);

        verify(calculatedMetricService, never()).recalculateAndSaveDerivedMetrics(eq(USER_ID), any(Set.class));
        verify(calculatedMetricService, never()).predictAndSaveModel1Pbf(eq(USER_ID), any(LocalDateTime.class));
    }

    private SubmitHealthDataRequest requestWithWeight() {
        SubmitHealthDataRequest request = new SubmitHealthDataRequest();
        request.setUserId(USER_ID);
        request.setBaseMetrics(List.of(new SubmitHealthDataRequest.BaseMetricInput(IndicatorType.WEIGHT, 70.0)));
        return request;
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
