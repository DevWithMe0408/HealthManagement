package org.example.userservice.repository;

import org.example.userservice.entity.UserGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserGoalRepository extends JpaRepository<UserGoal, String> {

    Optional<UserGoal> findByUserIdAndIsActiveTrue(String userId);

    List<UserGoal> findByUserIdOrderByStartDateDesc(String userId);

    @Modifying
    @Query("UPDATE UserGoal g SET g.isActive = false, g.endDate = :endDate "
            + "WHERE g.userId = :userId AND g.isActive = true")
    int deactivateCurrentGoal(@Param("userId") String userId,
                              @Param("endDate") LocalDate endDate);
}
