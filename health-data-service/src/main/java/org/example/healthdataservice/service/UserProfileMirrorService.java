package org.example.healthdataservice.service;

import org.example.healthdataservice.entity.UserForHealthData;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public interface UserProfileMirrorService {

    public void saveOrUpdateUserProfile(String userId, LocalDate birthDate, String genderString);
    public Optional<UserForHealthData> getUserProfile(String userId);
    void createDefaultUserForHealthData(String userId);
}
