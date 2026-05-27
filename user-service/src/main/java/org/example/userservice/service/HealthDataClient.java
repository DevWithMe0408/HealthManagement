package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthDataClient {

    private final RestTemplate healthDataRestTemplate;

    @Value("${app.services.health-data.url:http://localhost:8085}")
    private String healthDataBaseUrl;

    @SuppressWarnings("unchecked")
    public BigDecimal fetchCurrentWeightKg(String userId) {
        String url = healthDataBaseUrl + "/api/health-data/dashboard-metrics";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("userId", userId);

            ResponseEntity<Map> response = healthDataRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                return null;
            }

            Object dataObj = body.get("data");
            if (!(dataObj instanceof Map)) {
                return null;
            }

            Object weightObj = ((Map<String, Object>) dataObj).get("weight");
            if (!(weightObj instanceof Map)) {
                return null;
            }

            Object valueObj = ((Map<String, Object>) weightObj).get("value");
            return valueObj != null ? new BigDecimal(valueObj.toString()) : null;
        } catch (Exception e) {
            log.warn("Failed to fetch current weight for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }
}
