package org.example.healthdataservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LatestHealthDataResponse {
    // Key là IndicatorType (dưới dạng String để dễ serialize/deserialize qua JSON),
    // Value là giá trị của chỉ số đó.
    private Map<String, Double> baseMetrics;
}
