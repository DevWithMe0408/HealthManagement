package org.example.healthdataservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// DTO con để chứa thông tin của một chỉ số
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricData {
    private Double value;
    private String unit;
    private LocalDateTime lastUpdatedAt;
}