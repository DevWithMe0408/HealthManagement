package org.example.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.request.UserRequestDTO;
import org.example.userservice.dto.response.DataResponse;
import org.example.userservice.dto.response.UserAccountDetailsResponse;
import org.example.userservice.dto.response.UserProfileResponse;
import org.example.userservice.dto.response.UserResponseDTO;
import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;
import org.example.userservice.exception.BusinessException;
import org.example.userservice.exception.ErrorCode;
import org.example.userservice.mapper.UserMapper;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.security.CustomUserDetails;
import org.example.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    @GetMapping("/allUsers")
    public ResponseEntity<DataResponse<List<UserResponseDTO>>> getAllUsers() {
        List<UserResponseDTO> response = userService.findAll()
                .stream()
                .map(userMapper::toDTO)
                .toList();
        return ResponseEntity.ok(DataResponse.success(response));
    }

    @GetMapping("/currentUser")
    public ResponseEntity<DataResponse<UserProfileResponse>> getCurrentUserProfile(
            @RequestHeader(name = "username", required = false) String usernameFromGateway,
            @RequestHeader(name = "userId", required = false) String userIdFromGateway,
            @RequestHeader(name = "userRoles", required = false) String rolesFromGateway) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username;
        List<String> roles;
        String userId = null;

        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication.getPrincipal() instanceof String &&
                        authentication.getPrincipal().equals("anonymousUser"))) {
            if (authentication.getPrincipal() instanceof UserDetails ud) {
                username = ud.getUsername();
                if (ud instanceof CustomUserDetails cud) {
                    userId = cud.getId();
                }
            } else {
                username = authentication.getName();
            }
            roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
        } else if (usernameFromGateway != null) {
            username = usernameFromGateway;
            roles = (rolesFromGateway != null && !rolesFromGateway.isEmpty()) ?
                    Arrays.asList(rolesFromGateway.split(",")) :
                    List.of();
            if (userIdFromGateway != null) {
                userId = userIdFromGateway;
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(DataResponse.success(new UserProfileResponse(userId, username, roles)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<UserResponseDTO>> getUserById(@PathVariable String id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(DataResponse.success(userMapper.toDTO(user)));
    }

    @GetMapping("/account-details")
    public ResponseEntity<DataResponse<UserAccountDetailsResponse>> getAccountDetails(
            @RequestHeader("userId") String userIdFromGateway) {
        User user = userRepository.findById(userIdFromGateway)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Auth auth = user.getAuth();
        if (auth == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        List<String> roles = Collections.singletonList(auth.getRole().name());
        UserAccountDetailsResponse response = UserAccountDetailsResponse.builder()
                .userId(user.getId())
                .userName(auth.getUsername())
                .email(auth.getEmail())
                .roles(roles)
                .name(user.getName())
                .phoneNumber(user.getPhone())
                .birthDate(user.getBirthDate())
                .gender(user.getGender() != null ? user.getGender() : null)
                .build();
        return ResponseEntity.ok(DataResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<DataResponse<UserResponseDTO>> createUser(@RequestBody UserRequestDTO userDTO) {
        User user = userMapper.toEntity(userDTO);
        User saved = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(DataResponse.success(userMapper.toDTO(saved)));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<DataResponse<UserResponseDTO>> updateUser(
            @PathVariable String id, @RequestBody UserRequestDTO userDTO) {
        User updated = userService.updateUserProfile(id, userMapper.toEntity(userDTO));
        return ResponseEntity.ok(DataResponse.success(userMapper.toDTO(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<Void>> deleteUser(@PathVariable String id) {
        userService.deleteUserAndAuthById(id);
        return ResponseEntity.ok(DataResponse.success());
    }
}
