package org.example.userservice.service;

import org.example.userservice.dto.request.PreferenceUpdateRequest;
import org.example.userservice.dto.response.PreferenceResponse;

import java.util.List;
import java.util.Optional;

public interface UserPreferenceService {
    List<PreferenceResponse> getAll(String userId);

    Optional<PreferenceResponse> getOne(String userId, String prefKey);

    PreferenceResponse upsert(String userId, String prefKey, PreferenceUpdateRequest req);

    void delete(String userId, String prefKey);

    void seedDefaults(String userId);
}
