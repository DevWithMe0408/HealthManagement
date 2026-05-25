package org.example.healthdataservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.healthdataservice.entity.UserPreferenceMirror;
import org.example.healthdataservice.repository.UserPreferenceMirrorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceMirrorServiceImpl implements UserPreferenceMirrorService {

    private final UserPreferenceMirrorRepository repo;

    @Override
    @Transactional
    public void saveOrUpdate(String userId, String prefKey, String prefValue) {
        if (prefValue == null) {
            repo.deleteByUserIdAndPrefKey(userId, prefKey);
            log.info("Deleted preference mirror userId={} key={}", userId, prefKey);
            return;
        }

        UserPreferenceMirror mirror = repo.findByUserIdAndPrefKey(userId, prefKey)
                .orElseGet(() -> {
                    UserPreferenceMirror created = new UserPreferenceMirror();
                    created.setUserId(userId);
                    created.setPrefKey(prefKey);
                    return created;
                });
        mirror.setPrefValue(prefValue);
        repo.save(mirror);
        log.info("Saved preference mirror userId={} key={}", userId, prefKey);
    }

    @Override
    public Optional<String> getValue(String userId, String prefKey) {
        return repo.findByUserIdAndPrefKey(userId, prefKey)
                .map(UserPreferenceMirror::getPrefValue);
    }

    @Override
    public String getValueOrDefault(String userId, String prefKey, String defaultValue) {
        return getValue(userId, prefKey).orElse(defaultValue);
    }
}
