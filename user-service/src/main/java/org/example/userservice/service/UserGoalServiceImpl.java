package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.common.util.UuidV7Generator;
import org.example.userservice.dto.request.UpdateGoalRequest;
import org.example.userservice.dto.response.UserGoalResponse;
import org.example.userservice.entity.UserGoal;
import org.example.userservice.repository.UserGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserGoalServiceImpl implements UserGoalService {

    private final UserGoalRepository repo;
    private final HealthDataClient healthDataClient;

    @Override
    public Optional<UserGoalResponse> getCurrent(String userId) {
        return repo.findByUserIdAndIsActiveTrue(userId).map(this::toResponse);
    }

    @Override
    public List<UserGoalResponse> getHistory(String userId) {
        return repo.findByUserIdOrderByStartDateDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserGoalResponse updateCurrent(String userId, UpdateGoalRequest req) {
        LocalDate today = LocalDate.now();
        repo.deactivateCurrentGoal(userId, today);

        BigDecimal startWeight = healthDataClient.fetchCurrentWeightKg(userId);
        if (startWeight == null) {
            log.info("No current weight available for userId={}, startWeightKg will be null", userId);
        }

        UserGoal newGoal = UserGoal.builder()
                .id(UuidV7Generator.generate())
                .userId(userId)
                .goalCode(req.getGoalCode())
                .startDate(today)
                .endDate(null)
                .isActive(true)
                .targetWeightKg(req.getTargetWeightKg())
                .startWeightKg(startWeight)
                .targetDurationMonths(req.getTargetDurationMonths() != null ? req.getTargetDurationMonths() : 6)
                .note(req.getNote())
                .build();

        UserGoal saved = repo.save(newGoal);
        log.info("Updated goal for userId {}: {}, startWeight={}", userId, req.getGoalCode(), startWeight);
        return toResponse(saved);
    }

    private UserGoalResponse toResponse(UserGoal goal) {
        return UserGoalResponse.builder()
                .id(goal.getId())
                .goalCode(goal.getGoalCode().name())
                .startDate(goal.getStartDate())
                .endDate(goal.getEndDate())
                .isActive(Boolean.TRUE.equals(goal.getIsActive()))
                .targetWeightKg(goal.getTargetWeightKg())
                .startWeightKg(goal.getStartWeightKg())
                .targetDurationMonths(goal.getTargetDurationMonths())
                .note(goal.getNote())
                .build();
    }
}
