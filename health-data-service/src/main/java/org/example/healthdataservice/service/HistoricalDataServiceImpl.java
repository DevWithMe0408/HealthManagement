package org.example.healthdataservice.service;

import org.example.healthdataservice.dto.HistoricalDataPointDTO;
import org.example.healthdataservice.entity.enums.IndicatorCategory;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.repository.BaseMetricValueRepository;
import org.example.healthdataservice.repository.CalculatedMetricSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoricalDataServiceImpl implements HistoricalDataService {

    private static final Logger log = LoggerFactory.getLogger(HistoricalDataServiceImpl.class);

    @Autowired
    private BaseMetricValueRepository baseMetricRepo;

    @Autowired
    private CalculatedMetricSnapshotRepository calculatedMetricRepo;

    @Override
    public List<HistoricalDataPointDTO> getHistoricalData(Long userId, IndicatorType indicatorType, LocalDateTime fromDate, LocalDateTime toDate, String granularity ) {

        LocalDateTime toDatePlusOneDay = toDate.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        if (fromDate.isAfter(toDate)) {
            log.warn("fromDate is after toDate for userId {}, indicator {}, from {}, to {}", userId, indicatorType, fromDate, toDate);
            return Collections.emptyList();
        }

        List<Object[]> results;
        boolean isBaseMetric = indicatorType.getCategory() == IndicatorCategory.BASE;

        log.debug("Fetching historical data for userId: {}, type: {}, from: {}, to: {}, granularity: {}, isBase: {}",
                userId, indicatorType, fromDate, toDatePlusOneDay, granularity, isBaseMetric);

        switch (granularity.toUpperCase()) {
            case "DAILY":
                results = isBaseMetric ? baseMetricRepo.findLastDailyBaseMetrics(userId, indicatorType.name(), fromDate, toDatePlusOneDay) :
                calculatedMetricRepo.findLastDailyCalculatedMetrics(userId, indicatorType.name(), fromDate, toDatePlusOneDay);
                break;
            case "WEEKLY":
                results = isBaseMetric ?
                        baseMetricRepo.findLastWeeklyBaseMetrics(userId, indicatorType.name(), fromDate, toDatePlusOneDay) :
                        calculatedMetricRepo.findLastWeeklyCalculatedMetrics(userId, indicatorType.name(), fromDate, toDatePlusOneDay);
                break;
            case "MONTHLY":
                results = isBaseMetric ?
                        baseMetricRepo.findLastMonthlyBaseMetrics(userId, indicatorType.name(), fromDate, toDatePlusOneDay) :
                        calculatedMetricRepo.findLastMonthlyCalculatedMetrics(userId, indicatorType.name(), fromDate, toDatePlusOneDay);
                break;
            case "NONE": // Lấy tất cả điểm dữ liệu
            default: // Mặc định cũng lấy tất cả nếu granularity không hợp lệ
                return isBaseMetric ?
                        baseMetricRepo.findAllBaseMetricsInDateRange(userId, indicatorType, fromDate, toDatePlusOneDay) :
                        calculatedMetricRepo.findAllCalculatedMetricsInDateRange(userId, indicatorType, fromDate, toDatePlusOneDay);
        }
        if (results == null) return Collections.emptyList();

        return results.stream()
                .map(row -> {
                    LocalDateTime ts = ((Timestamp) row[0]).toLocalDateTime();
                    Double val = (Double) row[1];
                    String unit = (String) row[2];
                    return new HistoricalDataPointDTO(ts, val, unit);
                })
                .collect(Collectors.toList());

    }

}
