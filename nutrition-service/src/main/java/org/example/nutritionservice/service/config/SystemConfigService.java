package org.example.nutritionservice.service.config;

import org.example.nutritionservice.dto.request.SystemConfigUpdateRequest;
import org.example.nutritionservice.dto.response.SystemConfigResponse;

public interface SystemConfigService {
    SystemConfigResponse get();
    SystemConfigResponse update(SystemConfigUpdateRequest request, String updatedBy);
}
