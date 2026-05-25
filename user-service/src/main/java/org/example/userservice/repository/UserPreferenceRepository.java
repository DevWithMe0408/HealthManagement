package org.example.userservice.repository;

import org.example.userservice.entity.UserPreference;
import org.example.userservice.entity.UserPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UserPreferenceId> {

    List<UserPreference> findByUserId(String userId);

    Optional<UserPreference> findByUserIdAndPrefKey(String userId, String prefKey);

    void deleteByUserIdAndPrefKey(String userId, String prefKey);
}
