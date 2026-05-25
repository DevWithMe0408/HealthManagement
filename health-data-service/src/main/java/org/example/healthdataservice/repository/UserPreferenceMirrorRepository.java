package org.example.healthdataservice.repository;

import org.example.healthdataservice.entity.UserPreferenceMirror;
import org.example.healthdataservice.entity.UserPreferenceMirrorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceMirrorRepository extends JpaRepository<UserPreferenceMirror, UserPreferenceMirrorId> {

    Optional<UserPreferenceMirror> findByUserIdAndPrefKey(String userId, String prefKey);

    void deleteByUserIdAndPrefKey(String userId, String prefKey);
}
