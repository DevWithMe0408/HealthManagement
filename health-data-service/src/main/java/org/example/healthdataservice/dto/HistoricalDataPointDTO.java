package org.example.healthdataservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalDataPointDTO {
    private LocalDateTime timestamp;
    private Double value;
    private String unitCode; // Mã đơn vị, ví dụ: kg, cm, %, bmi
}
