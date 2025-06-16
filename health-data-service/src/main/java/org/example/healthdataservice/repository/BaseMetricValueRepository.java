package org.example.healthdataservice.repository;

import org.example.healthdataservice.dto.HistoricalDataPointDTO;
import org.example.healthdataservice.entity.BaseMetricValue;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BaseMetricValueRepository extends JpaRepository<BaseMetricValue, Long> {
    // Tìm bản ghi mới nhất của một loại chỉ số cơ bản cho một user
    Optional<BaseMetricValue> findTopByUserIdAndIndicatorTypeOrderByRecordedAtDesc(Long userId, IndicatorType indicatorType);

    // Lấy tất cả các giá trị cơ bản cho một user tại một thời điểm hoặc mới nhất
    List<BaseMetricValue> findByUserIdOrderByRecordedAtDesc(Long userId);

    // Native query để lấy giá trị cuối cùng theo ngày
    @Query(value = "WITH RankedMetrics AS (" +
            "  SELECT bmv.*, u.code as unit_code, " +
            "         ROW_NUMBER() OVER (PARTITION BY DATE(bmv.recorded_at) ORDER BY bmv.recorded_at DESC, bmv.id DESC) as rn " + // Thêm id để đảm bảo thứ tự nếu timestamp trùng
            "  FROM base_metric_values bmv " +
            "  LEFT JOIN units u ON bmv.unit_id = u.id " +
            "  WHERE bmv.user_id = :userId AND bmv.indicator_type = :indicatorType " +
            "    AND bmv.recorded_at >= :fromDate AND bmv.recorded_at < :toDatePlusOneDay" +
            ") " +
            "SELECT rm.recorded_at as timestamp, rm.value, rm.unit_code as unitCode " +
            "FROM RankedMetrics rm WHERE rm.rn = 1 ORDER BY rm.recorded_at ASC", nativeQuery = true)
    List<Object[]> findLastDailyBaseMetrics(@Param("userId") Long userId,
                                            @Param("indicatorType") String indicatorType, // Truyền String vì native query
                                            @Param("fromDate") LocalDateTime fromDate,
                                            @Param("toDatePlusOneDay") LocalDateTime toDatePlusOneDay);

    // Native query để lấy giá trị cuối cùng theo tuần
    // WEEK(date, mode) mode 1: Tuần bắt đầu từ Thứ Hai
    @Query(value = "WITH RankedMetrics AS (" +
            "  SELECT bmv.*, u.code as unit_code, " +
            "         ROW_NUMBER() OVER (PARTITION BY YEAR(bmv.recorded_at), WEEK(bmv.recorded_at, 1) ORDER BY bmv.recorded_at DESC, bmv.id DESC) as rn " +
            "  FROM base_metric_values bmv " +
            "  LEFT JOIN units u ON bmv.unit_id = u.id " +
            "  WHERE bmv.user_id = :userId AND bmv.indicator_type = :indicatorType " +
            "    AND bmv.recorded_at >= :fromDate AND bmv.recorded_at < :toDatePlusOneDay" +
            ") " +
            "SELECT rm.recorded_at as timestamp, rm.value, rm.unit_code as unitCode " +
            "FROM RankedMetrics rm WHERE rm.rn = 1 ORDER BY rm.recorded_at ASC", nativeQuery = true)
    List<Object[]> findLastWeeklyBaseMetrics(@Param("userId") Long userId,
                                             @Param("indicatorType") String indicatorType,
                                             @Param("fromDate") LocalDateTime fromDate,
                                             @Param("toDatePlusOneDay") LocalDateTime toDatePlusOneDay);

    // Native query để lấy giá trị cuối cùng theo tháng
    @Query(value = "WITH RankedMetrics AS (" +
            "  SELECT bmv.*, u.code as unit_code, " +
            "         ROW_NUMBER() OVER (PARTITION BY YEAR(bmv.recorded_at), MONTH(bmv.recorded_at) ORDER BY bmv.recorded_at DESC, bmv.id DESC) as rn " +
            "  FROM base_metric_values bmv " +
            "  LEFT JOIN units u ON bmv.unit_id = u.id " +
            "  WHERE bmv.user_id = :userId AND bmv.indicator_type = :indicatorType " +
            "    AND bmv.recorded_at >= :fromDate AND bmv.recorded_at < :toDatePlusOneDay" +
            ") " +
            "SELECT rm.recorded_at as timestamp, rm.value, rm.unit_code as unitCode " +
            "FROM RankedMetrics rm WHERE rm.rn = 1 ORDER BY rm.recorded_at ASC", nativeQuery = true)
    List<Object[]> findLastMonthlyBaseMetrics(@Param("userId") Long userId,
                                              @Param("indicatorType") String indicatorType,
                                              @Param("fromDate") LocalDateTime fromDate,
                                              @Param("toDatePlusOneDay") LocalDateTime toDatePlusOneDay);

    // Query để lấy tất cả bản ghi (raw data) nếu granularity là NONE
    @Query("SELECT NEW org.example.healthdataservice.dto.HistoricalDataPointDTO(bmv.recordedAt, bmv.value, u.code) " +
            "FROM BaseMetricValue bmv LEFT JOIN bmv.unit u " +
            "WHERE bmv.userId = :userId AND bmv.indicatorType = :indicatorType " +
            "  AND bmv.recordedAt >= :fromDate AND bmv.recordedAt < :toDatePlusOneDay " +
            "ORDER BY bmv.recordedAt ASC")
    List<HistoricalDataPointDTO> findAllBaseMetricsInDateRange(@Param("userId") Long userId,
                                                               @Param("indicatorType") IndicatorType indicatorType, // JPQL có thể dùng ENUM
                                                               @Param("fromDate") LocalDateTime fromDate,
                                                               @Param("toDatePlusOneDay") LocalDateTime toDatePlusOneDay);
}
