package org.example.userservice.service;

import org.example.userservice.dto.request.UpdateGoalRequest;
import org.example.userservice.dto.response.UserGoalResponse;

import java.util.List;
import java.util.Optional;

public interface UserGoalService {
    Optional<UserGoalResponse> getCurrent(String userId);

    List<UserGoalResponse> getHistory(String userId);

    UserGoalResponse updateCurrent(String userId, UpdateGoalRequest req);
}
