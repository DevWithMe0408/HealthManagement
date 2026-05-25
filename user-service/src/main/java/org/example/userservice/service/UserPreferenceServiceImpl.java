package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.events.UserPreferencesUpdatedEvent;
import org.example.userservice.dto.request.PreferenceUpdateRequest;
import org.example.userservice.dto.response.PreferenceResponse;
import org.example.userservice.entity.UserPreference;
import org.example.userservice.repository.UserPreferenceRepository;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private static final String PBF_METHOD = "pbf_method";
    private static final String DEFAULT_PBF_METHOD = "FORMULA";
    private static final String VALUE_TYPE_STRING = "STRING";
    private static final String PBF_METHOD_DESCRIPTION = "Method tinh PBF: FORMULA (Navy) hoac MODEL_1 (ML)";
    private static final Set<String> ALLOWED_KEYS = Set.of(PBF_METHOD);
    private static final Set<String> ALLOWED_PBF_METHODS = Set.of("FORMULA", "MODEL_1");

    private final UserPreferenceRepository repo;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.user-events}")
    private String userEventsExchangeName;

    @Value("${app.rabbitmq.routing-key.user-preferences-updated}")
    private String prefsUpdatedRoutingKey;

    @Override
    public List<PreferenceResponse> getAll(String userId) {
        return repo.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Optional<PreferenceResponse> getOne(String userId, String prefKey) {
        return repo.findByUserIdAndPrefKey(userId, prefKey).map(this::toResponse);
    }

    @Override
    @Transactional
    public PreferenceResponse upsert(String userId, String prefKey, PreferenceUpdateRequest req) {
        validateKey(prefKey);
        validateValueForKey(prefKey, req.getPrefValue());

        UserPreference pref = repo.findByUserIdAndPrefKey(userId, prefKey)
                .orElse(UserPreference.builder()
                        .userId(userId)
                        .prefKey(prefKey)
                        .description(descriptionForKey(prefKey))
                        .build());

        pref.setPrefValue(req.getPrefValue());
        pref.setValueType(req.getValueType() != null ? req.getValueType() : VALUE_TYPE_STRING);
        pref.setDescription(descriptionForKey(prefKey));

        UserPreference saved = repo.save(pref);
        publishEvent(userId, prefKey, saved.getPrefValue());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(String userId, String prefKey) {
        validateKey(prefKey);
        repo.deleteByUserIdAndPrefKey(userId, prefKey);
        publishEvent(userId, prefKey, null);
    }

    @Override
    @Transactional
    public void seedDefaults(String userId) {
        repo.findByUserIdAndPrefKey(userId, PBF_METHOD)
                .orElseGet(() -> repo.save(UserPreference.builder()
                        .userId(userId)
                        .prefKey(PBF_METHOD)
                        .prefValue(DEFAULT_PBF_METHOD)
                        .valueType(VALUE_TYPE_STRING)
                        .description(PBF_METHOD_DESCRIPTION)
                        .build()));
    }

    private void validateKey(String prefKey) {
        if (!ALLOWED_KEYS.contains(prefKey)) {
            throw new BusinessException(ErrorCode.PREFERENCE_INVALID_KEY);
        }
    }

    private void validateValueForKey(String prefKey, String prefValue) {
        if (PBF_METHOD.equals(prefKey) && !ALLOWED_PBF_METHODS.contains(prefValue)) {
            throw new BusinessException(ErrorCode.PREFERENCE_INVALID_VALUE);
        }
    }

    private String descriptionForKey(String prefKey) {
        if (PBF_METHOD.equals(prefKey)) {
            return PBF_METHOD_DESCRIPTION;
        }
        return null;
    }

    private void publishEvent(String userId, String prefKey, String prefValue) {
        try {
            UserPreferencesUpdatedEvent event = new UserPreferencesUpdatedEvent(userId, prefKey, prefValue);
            rabbitTemplate.convertAndSend(userEventsExchangeName, prefsUpdatedRoutingKey, event);
            log.info("Published UserPreferencesUpdatedEvent userId={} key={}", userId, prefKey);
        } catch (Exception e) {
            log.error("Failed to publish UserPreferencesUpdatedEvent for userId {}: {}", userId, e.getMessage(), e);
        }
    }

    private PreferenceResponse toResponse(UserPreference pref) {
        return PreferenceResponse.builder()
                .prefKey(pref.getPrefKey())
                .prefValue(pref.getPrefValue())
                .valueType(pref.getValueType())
                .description(pref.getDescription())
                .build();
    }
}
