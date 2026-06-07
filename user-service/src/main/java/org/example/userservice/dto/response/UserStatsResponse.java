package org.example.userservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatsResponse {

    private long totalUsers;
    private long usersWithProfile;
}
