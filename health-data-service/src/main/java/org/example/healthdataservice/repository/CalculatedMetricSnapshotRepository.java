package org.example.healthdataservice.repository;

import org.example.healthdataservice.dto.HistoricalDataPointDTO;
import org.example.healthdataservice.entity.CalculatedMetricSnapshot;
import org.example.healthdataservice.entity.enums.IndicatorCategory;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalculatedMetricSnapshotRepository extends JpaRepository<CalculatedMetricSnapshot, Long> {

    // Tìm snapshot mới nhất của một loại chỉ số tính toán cho một user
    Optional<CalculatedMetricSnapshot> findTopByUserIdAndIndicatorTypeOrderByCalculatedAtDesc(Long userId, IndicatorType indicatorType);

    // Tìm snapshot mới nhất của một loại chỉ số tính toán cho một user VÀ có nguồn cụ thể
    Optional<CalculatedMetricSnapshot>
    findTopByUserIdAndIndicatorTypeAndSourceCategoryOrderByCalculatedAtDesc(Long userId, IndicatorType indicatorType, IndicatorCategory sourceCategory);

    List<CalculatedMetricSnapshot> findByUserIdAndIndicatorTypeOrderByCalculatedAtDesc(Long userId, IndicatorType indicatorType);


    // Native query để lấy giá trị cuối cùng theo ngày
    @Query(value = "WITH RankedMetrics AS (" +
            "  SELECT cms.*, u.code as unit_code, " +
            "         ROW_NUMBER() OVER (PARTITION BY DATE(cms.calculated_at) ORDER BY cms.calculated_at DESC, cms.id DESC) as rn " + // Thêm id để đảm bảo thứ tự nếu timestamp trùng
            "  FROM calculated_metric_snapshots cms " +
            "  LEFT JOIN units u ON cms.unit_id = u.id " +
            "  WHERE cms.user_id = :userId AND cms.indicator_type = :indicatorType " +
            "    AND cms.calculated_at >= :fromDate AND cms.calculated_at < :toDatePlusOneDay" +
            ") " +
            "SELECT rm.calculated_at as timestamp, rm.value, rm.unit_code as unitCode " +
            "FROM RankedMetrics rm WHERE rm.rn = 1 ORDER BY rm.calculated_at ASC", nativeQuery = true)
    List<Object[]> findLastDailyCalculatedMetrics(@Param("userId") Long userId,
                                            @Param("indicatorType") String indicatorType, // Truyền String vì native query
                                            @Param("fromDate") LocalDateTime fromDate,
                                            @Param("toDatePlusOneDay") LocalDateTime toDatePlusOneDay);

    // Native query để lấy giá trị cuối cùng theo tuần (Ví dụ cho MySQL, cần điều chỉnh cho DB khác)
    // WEEK(date, mode) mode 1: Tuần bắt đầu từ Thứ Hai
    @Query(value = "WITH RankedMetrics AS (" +
            "  SELECT cms.*, u.code as unit_code, " +
            "         ROW_NUMBER() OVER (PARTITION BY YEAR(cms.calculated_at), WEEK(cms.calculated_at, 1) ORDER BY cms.calculated_at DESC, cms.id DESC) as rn " +
            "  FROM calculated_metric_snapshots cms " +
            "  LEFT JOIN units u ON cms.unit_id = u.id " +
            "  WHERE cms.user_id = :userId AND cms.indicator_type = :indicatorType " +
            "    AND cms.calculated_at >= :fromDate AND cms.calculated_at < :toDatePlusOneDay" +
            ") " +
            "SELECT rm.calculated_at as timestamp, rm.value, rm.unit_code as unitCode " +
            "FROM RankedMetrics rm WHERE rm.rn = 1 ORDER BY rm.calculated_at ASC", nativeQuery = true)
    List<Object[]> findLastWeeklyCalculatedMetrics(@Param("userId") Long userId,
                                             @Param("indicatorType") String indicatorType,
                                             @Param("fromDate") LocalDateTime fromDate,
                                             @Param("toDatePlusOneDay") LocalDateTime toDatePlusOneDay);

    // Native query để lấy giá trị cuối cùng theo tháng
    @Query(value = "WITH RankedMetrics AS (" +
            "  SELECT cms.*, u.code as unit_code, " +
            "         ROW_NUMBER() OVER (PARTITION BY YEAR(cms.calculated_at), MONTH(cms.calculated_at) ORDER BY cms.calculated_at DESC, cms.id DESC) as rn " +
            "  FROM calculated_metric_snapshots cms " +
            "  LEFT JOIN units u ON cms.unit_id = u.id " +
            "  WHERE cms.user_id = :userId AND cms.indicator_type = :indicatorType " +
            "    AND cms.calculated_at >= :fromDate AND cms.calculated_at < :toDatePlusOneDay" +
            ") " +
            "SELECT rm.calculated_at as timestamp, rm.value, rm.unit_code as unitCode " +
            "FROM RankedMetrics rm WHERE rm.rn = 1 ORDER BY rm.calculated_at ASC", nativeQuery = true)
    List<Object[]> findLastMonthlyCalculatedMetrics(@Param("userId") Long userId,
                                              @Param("indicatorType") String indicatorType,
                                              @Param("fromDate") LocalDateTime fromDate,
                                              @Param("toDatePlusOneDay") LocalDateTime toDatePlusOneDay);

    // Query để lấy tất cả bản ghi (raw data) nếu granularity là NONE
    @Query("SELECT NEW org.example.healthdataservice.dto.HistoricalDataPointDTO(cms.recordedAt, cms.value, u.code) " +
            "FROM BaseMetricValue cms LEFT JOIN cms.unit u " +
            "WHERE cms.userId = :userId AND cms.indicatorType = :indicatorType " +
            "  AND cms.recordedAt >= :fromDate AND cms.recordedAt < :toDatePlusOneDay " +
            "ORDER BY cms.recordedAt ASC")
    List<HistoricalDataPointDTO> findAllCalculatedMetricsInDateRange(@Param("userId") Long userId,
                                                               @Param("indicatorType") IndicatorType indicatorType, // JPQL có thể dùng ENUM
                                                               @Param("fromDate") LocalDateTime fromDate,
                                                               @Param("toDatePlusOneDay") LocalDateTime toDatePlusOneDay);
}
