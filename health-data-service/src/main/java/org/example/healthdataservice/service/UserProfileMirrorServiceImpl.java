package org.example.healthdataservice.service;

import org.example.healthdataservice.entity.UserForHealthData;
import org.example.healthdataservice.entity.enums.Gender;
import org.example.healthdataservice.repository.UserForHealthDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserProfileMirrorServiceImpl implements UserProfileMirrorService{

    private static final Logger log = LoggerFactory.getLogger(UserProfileMirrorServiceImpl.class);

    private final UserForHealthDataRepository repository;

    @Autowired
    public UserProfileMirrorServiceImpl(UserForHealthDataRepository repository) {
        this.repository = repository;
    }
    @Override
    @Transactional
    public void saveOrUpdateUserProfile(Long userId, LocalDate birthDate, String genderString) {
        Gender gender = null;
        if (genderString != null && !genderString.isBlank()) {
            try {
                gender = Gender.valueOf(genderString.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid gender string '{}' received for userId {}. Storing as null.", genderString, userId);
            }
        }
        Optional<UserForHealthData> existingProfileOpt = repository.findById(userId);
        UserForHealthData profile;
        if (existingProfileOpt.isPresent()) {
            profile = existingProfileOpt.get();
            log.info("Updating existing profile for userId: {}", userId);
        } else {
            profile = new UserForHealthData();
            profile.setUserId(userId);
            log.info("Creating new profile mirror for userId: {}", userId);
        }

        profile.setBirthDate(birthDate);
        profile.setGender(gender);
        // lastUpdatedAt sẽ tự cập nhật qua @PrePersist/@PreUpdate

        repository.save(profile);
        log.info("Successfully saved/updated profile mirror for userId: {}. BirthDate: {}, Gender: {}",
                userId, birthDate, gender);
    }

    public void createDefaultUserForHealthData(Long userId) {
        // Tạo một bản ghi mặc định cho người dùng mới
        UserForHealthData defaultProfile = new UserForHealthData();
        defaultProfile.setUserId(userId);
        defaultProfile.setBirthDate(LocalDate.now()); // Ngày sinh mặc định là hôm nay
        defaultProfile.setGender(null);
        defaultProfile.setLastUpdatedAt(LocalDateTime.now());
        repository.save(defaultProfile);

    }

    @Override
    public Optional<UserForHealthData> getUserProfile(Long userId) {
        return repository.findById(userId);
    }
}
