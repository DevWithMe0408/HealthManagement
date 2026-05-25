package org.example.healthdataservice.service;

import java.util.Optional;

public interface UserPreferenceMirrorService {
    void saveOrUpdate(String userId, String prefKey, String prefValue);

    Optional<String> getValue(String userId, String prefKey);

    String getValueOrDefault(String userId, String prefKey, String defaultValue);
}
