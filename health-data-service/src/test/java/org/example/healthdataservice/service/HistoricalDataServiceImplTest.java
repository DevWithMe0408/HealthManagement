package org.example.healthdataservice.service;

import org.example.healthdataservice.repository.BaseMetricValueRepository;
import org.example.healthdataservice.repository.CalculatedMetricSnapshotRepository;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalDataServiceImplTest {

    private static final String USER_ID = "user-1";
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 6, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 6, 10, 23, 59, 59);
    private static final LocalDateTime TO_EXCLUSIVE = LocalDateTime.of(2026, 6, 11, 0, 0);

    @Mock
    private BaseMetricValueRepository baseMetricRepo;

    @Mock
    private CalculatedMetricSnapshotRepository calculatedMetricRepo;

    private HistoricalDataServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HistoricalDataServiceImpl();
        ReflectionTestUtils.setField(service, "baseMetricRepo", baseMetricRepo);
        ReflectionTestUtils.setField(service, "calculatedMetricRepo", calculatedMetricRepo);
    }

    @Test
    void getHistoricalDataWithNoneUsesBaseRepositoryForBaseMetrics() {
        when(baseMetricRepo.findAllBaseMetricsInDateRange(USER_ID, IndicatorType.WEIGHT, FROM, TO_EXCLUSIVE))
                .thenReturn(List.of());

        service.getHistoricalData(USER_ID, IndicatorType.WEIGHT, FROM, TO, "NONE");

        verify(baseMetricRepo).findAllBaseMetricsInDateRange(USER_ID, IndicatorType.WEIGHT, FROM, TO_EXCLUSIVE);
        verify(calculatedMetricRepo, never()).findAllCalculatedMetricsInDateRange(
                USER_ID,
                IndicatorType.WEIGHT,
                FROM,
                TO_EXCLUSIVE
        );
    }

    @Test
    void getHistoricalDataWithNoneUsesCalculatedRepositoryForCalculatedMetrics() {
        when(calculatedMetricRepo.findAllCalculatedMetricsInDateRange(USER_ID, IndicatorType.BMI, FROM, TO_EXCLUSIVE))
                .thenReturn(List.of());

        service.getHistoricalData(USER_ID, IndicatorType.BMI, FROM, TO, "NONE");

        verify(calculatedMetricRepo).findAllCalculatedMetricsInDateRange(USER_ID, IndicatorType.BMI, FROM, TO_EXCLUSIVE);
        verify(baseMetricRepo, never()).findAllBaseMetricsInDateRange(
                USER_ID,
                IndicatorType.BMI,
                FROM,
                TO_EXCLUSIVE
        );
    }
}
