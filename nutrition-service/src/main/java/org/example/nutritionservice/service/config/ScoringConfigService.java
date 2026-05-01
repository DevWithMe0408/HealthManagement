package org.example.nutritionservice.service.config;

import org.example.nutritionservice.dto.request.ScoringConfigUpdateRequest;
import org.example.nutritionservice.dto.response.ScoringConfigResponse;

public interface ScoringConfigService {
    ScoringConfigResponse get();
    ScoringConfigResponse update(ScoringConfigUpdateRequest request, String updatedBy);
}
