package org.example.healthdataservice.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsResponse {
    private MetricData weight;
    private MetricData height;
    private MetricData bmi;
    private MetricData bmr;
    private MetricData tdee;
    private MetricData pbf;
    private MetricData whr;
    // Thêm các chỉ số khác nếu muốn hiển thị trên dashboard
}
