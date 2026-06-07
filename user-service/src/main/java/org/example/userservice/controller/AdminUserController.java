package org.example.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.response.AdminUserDetailResponse;
import org.example.userservice.dto.response.AdminUserListItemResponse;
import org.example.userservice.dto.response.UserStatsResponse;
import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;
import org.example.userservice.enums.Role;
import org.example.userservice.repository.UserRepository;
import org.example.web.dto.response.DataResponse;
import org.example.web.exception.BusinessException;
import org.example.web.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    public DataResponse<Page<AdminUserListItemResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean hasProfile) {

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Role roleEnum = null;
        if (role != null && !role.isBlank()) {
            try {
                roleEnum = Role.valueOf(role);
            } catch (IllegalArgumentException ignored) {
            }
        }

        Page<AdminUserListItemResponse> result = userRepository
                .findAllWithFilters(search, roleEnum, hasProfile, pageable)
                .map(this::toListItem);

        return DataResponse.success(result);
    }

    @GetMapping("/stats")
    public DataResponse<UserStatsResponse> getUserStats() {
        return DataResponse.success(UserStatsResponse.builder()
                .totalUsers(userRepository.count())
                .usersWithProfile(userRepository.countWithProfile())
                .build());
    }

    @GetMapping("/{userId}")
    public DataResponse<AdminUserDetailResponse> getUserDetail(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Auth auth = user.getAuth();

        AdminUserDetailResponse.ProfileInfo profile = null;
        if (user.getName() != null || user.getBirthDate() != null
                || user.getGender() != null || user.getPhone() != null) {
            profile = AdminUserDetailResponse.ProfileInfo.builder()
                    .name(user.getName())
                    .birthDate(user.getBirthDate())
                    .age(user.getAge())
                    .gender(user.getGender() != null ? user.getGender().name() : null)
                    .phone(user.getPhone())
                    .build();
        }

        AdminUserDetailResponse response = AdminUserDetailResponse.builder()
                .account(AdminUserDetailResponse.AccountInfo.builder()
                        .userId(user.getId())
                        .username(auth != null ? auth.getUsername() : null)
                        .email(auth != null ? auth.getEmail() : null)
                        .role(auth != null ? auth.getRole().name() : null)
                        .build())
                .profile(profile)
                .build();

        return DataResponse.success(response);
    }

    private AdminUserListItemResponse toListItem(User user) {
        Auth auth = user.getAuth();
        boolean hasProfile = user.getBirthDate() != null && user.getGender() != null;
        return AdminUserListItemResponse.builder()
                .userId(user.getId())
                .username(auth != null ? auth.getUsername() : null)
                .email(auth != null ? auth.getEmail() : null)
                .name(user.getName())
                .role(auth != null ? auth.getRole().name() : null)
                .hasProfile(hasProfile)
                .build();
    }
}
