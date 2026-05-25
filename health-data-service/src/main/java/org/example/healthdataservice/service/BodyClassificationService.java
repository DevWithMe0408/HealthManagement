package org.example.healthdataservice.service;

import org.example.healthdataservice.dto.response.ConstitutionResponse;

public interface BodyClassificationService {
    ConstitutionResponse classifyCurrent(String userId);
}
