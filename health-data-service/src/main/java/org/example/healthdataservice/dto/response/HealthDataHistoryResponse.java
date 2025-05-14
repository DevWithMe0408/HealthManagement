package org.example.healthdataservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthDataHistoryResponse {
    private Long userId;
    private Double height;
    private Double weight;
    private Double bmi;
    private Double bmr;
    private Double tdee;
    private Double pbf;
    private Double whr;
    private Double waist;
    private Double hip;
    private Double neck;
    private Double bust;
    private Double activityFactor;
    private LocalDateTime measurementTime;
}
