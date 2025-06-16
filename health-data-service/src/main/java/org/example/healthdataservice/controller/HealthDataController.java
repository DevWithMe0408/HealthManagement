package org.example.healthdataservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.dto.HistoricalDataPointDTO;
import org.example.healthdataservice.dto.request.SubmitHealthDataRequest;
import org.example.healthdataservice.dto.response.DashboardMetricsResponse;
import org.example.healthdataservice.dto.response.LatestHealthDataResponse;
import org.example.healthdataservice.dto.response.MetricData;
import org.example.healthdataservice.entity.BaseMetricValue;
import org.example.healthdataservice.entity.CalculatedMetricSnapshot;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.service.BaseMetricService;
import org.example.healthdataservice.service.CalculatedMetricService;
import org.example.healthdataservice.service.HealthDataSubmitService;
import org.example.healthdataservice.service.HistoricalDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/health-data")
@RequiredArgsConstructor
public class HealthDataController {

    private static final Logger log = LoggerFactory.getLogger(HealthDataController.class);
    private final HealthDataSubmitService healthDataSubmitService;
    private final BaseMetricService baseMetricService;
    private final CalculatedMetricService calculatedMetricService;
    private final HistoricalDataService historicalDataService;

    @PostMapping("/submit")
    public ResponseEntity<String> submitHealthData(@Valid @RequestBody SubmitHealthDataRequest request) {
        try {
           log.info("Received health data submisstion for userId: {}", request.getUserId());
           healthDataSubmitService.processSubmittedHealthData(request);
           return ResponseEntity.ok("Dữ liệu sức khỏe đã được xử lý thành công.");
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument in health data submisstion for userId: {}: {}", request.getUserId(), e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi dữ liệu đầu vào: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing health data submisstion for userId: {}: {}", request.getUserId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Đã xảy ra lỗi máy chủ khi xử lý dữ liệu của bạn.");
        }
    }

    @GetMapping("/latest-metrics")
    public ResponseEntity<LatestHealthDataResponse> getLatestUserMetrics(
            @RequestHeader("userId") Long userId //// Lấy userId từ header do API Gateway thêm vào
    ) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Fetching latest metrics for userId: {}", userId);

        // Xác định các IndicatorType cơ bản để lấy
        Set<IndicatorType> baseTypesToFetch = Arrays.stream(IndicatorType.values())
                .filter(IndicatorType::isBaseMetric)
                .collect(Collectors.toSet());

        Map<IndicatorType, BaseMetricValue> latestBaseValues = baseMetricService.getLatestBaseMetrics(userId, baseTypesToFetch);

        Map<String, Double> baseMetricsMap = new HashMap<>();
        // Map<String, String> baseMetricsRecordedAtMap = new HashMap<>(); //trả về cả thời gian

        latestBaseValues.forEach((type, metricValue) -> {
            baseMetricsMap.put(type.name(), metricValue.getValue());
            // if (metricValue.getRecordedAt() != null) {
            //     baseMetricsRecordedAtMap.put(type.name(), metricValue.getRecordedAt().toString());
            // }
        });
        LatestHealthDataResponse response = new LatestHealthDataResponse(baseMetricsMap);
        // response.setBaseMetricsRecordedAt(baseMetricsRecordedAtMap);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/dashboard-metrics")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics(
            @RequestHeader("userId") Long userId
    ) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Fetching dashboard metrics for userId: {}", userId);

        DashboardMetricsResponse response = new DashboardMetricsResponse();

        // Lấy các chỉ số cơ bản cần thiết
        Optional<BaseMetricValue> weightOpt = baseMetricService.getLatestBaseMetric(userId, IndicatorType.WEIGHT);
        weightOpt.ifPresent(bmv -> response.setWeight(new MetricData(bmv.getValue(),
                bmv.getUnit() != null ? bmv.getUnit().getCode() : null, bmv.getRecordedAt())));

        Optional<BaseMetricValue> heightOpt = baseMetricService.getLatestBaseMetric(userId, IndicatorType.HEIGHT);
        heightOpt.ifPresent(bmv -> response.setHeight(new MetricData(bmv.getValue(),
                bmv.getUnit() != null ? bmv.getUnit().getCode() : null, bmv.getRecordedAt())));

        // Lấy các chỉ số tính toán mới nhất
        Optional<CalculatedMetricSnapshot> bmiOpt = calculatedMetricService.getLatestSnapshot(userId, IndicatorType.BMI);
        bmiOpt.ifPresent(cms -> response.setBmi(new MetricData(cms.getValue(),
                cms.getUnit() != null ? cms.getUnit().getCode() : null, cms.getCalculatedAt())));

        Optional<CalculatedMetricSnapshot> bmrOpt = calculatedMetricService.getLatestSnapshot(userId, IndicatorType.BMR);
        bmrOpt.ifPresent(cms -> response.setBmr(new MetricData(cms.getValue(), cms.getUnit() != null ? cms.getUnit().getCode() : null, cms.getCalculatedAt())));

        Optional<CalculatedMetricSnapshot> tdeeOpt = calculatedMetricService.getLatestSnapshot(userId, IndicatorType.TDEE);
        tdeeOpt.ifPresent(cms -> response.setTdee(new MetricData(cms.getValue(), cms.getUnit() != null ? cms.getUnit().getCode() : null, cms.getCalculatedAt())));

        Optional<CalculatedMetricSnapshot> pbfOpt = calculatedMetricService.getLatestSnapshot(userId, IndicatorType.PBF);
        pbfOpt.ifPresent(cms -> response.setPbf(new MetricData(cms.getValue(), cms.getUnit() != null ? cms.getUnit().getCode() : null, cms.getCalculatedAt())));

        Optional<CalculatedMetricSnapshot> whrOpt = calculatedMetricService.getLatestSnapshot(userId, IndicatorType.WHR);
        whrOpt.ifPresent(cms -> response.setWhr(new MetricData(cms.getValue(), cms.getUnit() != null ? cms.getUnit().getCode() : null, cms.getCalculatedAt())));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/query/history/{indicatorTypeString}")
    public ResponseEntity<List<HistoricalDataPointDTO>> getIndicatorHistory(
            @RequestHeader("userId") Long userId,
            @PathVariable String indicatorTypeString,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAILY") String granularity // "DAILY", "WEEKLY", "MONTHLY", "NONE"
            ) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        IndicatorType indicatorType;

        try {
            indicatorType = IndicatorType.valueOf(indicatorTypeString.toUpperCase()); // Chuyen String sang enum
        } catch (IllegalArgumentException e) {
            log.warn("Invalid indicatorType string: {}", indicatorTypeString);
            return ResponseEntity.badRequest().body(Collections.singletonList(new HistoricalDataPointDTO(null, null,"Invalid Indicator Type")));
        }

        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(23, 59, 59, 999999999);  // Bao gồm cả ngày 'to'

        List<HistoricalDataPointDTO> history = historicalDataService.getHistoricalData(
                userId,
                indicatorType,
                fromDateTime,
                toDateTime,
                granularity.toUpperCase()
        );
        return ResponseEntity.ok(history);
    }
}
