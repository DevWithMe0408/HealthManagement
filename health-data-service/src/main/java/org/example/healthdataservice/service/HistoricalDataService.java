package org.example.healthdataservice.service;

import org.example.healthdataservice.dto.HistoricalDataPointDTO;
import org.example.healthdataservice.entity.enums.IndicatorType;

import java.time.LocalDateTime;
import java.util.List;

public interface HistoricalDataService {
    List<HistoricalDataPointDTO> getHistoricalData(
            Long userId,
            IndicatorType indicatorType,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String granularity //"DAILY", "WEEKLY", "MONTHLY", "NONE"
    );
}
