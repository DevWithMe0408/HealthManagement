package org.example.nutritionservice.service.config;

import org.example.nutritionservice.dto.request.PenaltyConfigUpdateRequest;
import org.example.nutritionservice.dto.response.PenaltyConfigResponse;

public interface PenaltyConfigService {
    PenaltyConfigResponse get();
    PenaltyConfigResponse update(PenaltyConfigUpdateRequest request, String updatedBy);
}
