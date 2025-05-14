package org.example.healthdataservice.service;

import org.example.healthdataservice.dto.request.SubmitHealthDataRequest;

public interface HealthDataSubmitService {
    void submitBasicMetrics(SubmitHealthDataRequest request);
}
