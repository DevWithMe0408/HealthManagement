package org.example.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.request.UserRequestDTO;
import org.example.userservice.dto.response.UserAccountDetailsResponse;
import org.example.userservice.dto.response.UserProfileResponse;
import org.example.userservice.dto.response.UserResponseDTO;
import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;
import org.example.userservice.mapper.UserMapper;
import org.example.userservice.repository.AuthRepository;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.security.CustomUserDetails;
import org.example.userservice.service.UserServiceImpl;
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
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userService;
    private final UserMapper userMapper;
    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    @GetMapping("/allUsers")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        List<UserResponseDTO> response = userService.findAll()
                .stream()
                .map(userMapper::toDTO)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/currentUser")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(
            @RequestHeader(name = "username", required = false) String usernameFromGateway,
            @RequestHeader(name = "userId", required = false) String userIdFromGateway,
            @RequestHeader(name = "userRoles", required = false) String rolesFromGateway)
    {
        // Cách 1: Ưu tiên lấy từ SecurityContextHolder nếu User Service cũng có Spring Security
        // và được cấu hình để tạo Principal từ thông tin Gateway gửi.
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        String username;
        List<String> roles;
        Long userId = null;

        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication.getPrincipal() instanceof String &&
                        authentication.getPrincipal().equals("anonymousUser"))) {
            if (authentication.getPrincipal() instanceof UserDetails) {
                username = ((UserDetails) authentication.getPrincipal()).getUsername();
                // Nếu CustomUserDetails của bạn có ID:
                 if (authentication.getPrincipal() instanceof CustomUserDetails) {
                     userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
                 }
            } else {
                username = authentication.getName();
            }
            roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // Cố gắng lấy userId nếu Principal là CustomUserDetails có id
            // Hoặc nếu bạn đã cấu hình để userId được đưa vào Principal.
        } else if (usernameFromGateway != null) {
            // Cách 2: Nếu không có Principal đầy đủ, dựa vào header từ Gateway
            // (Ít an toàn hơn nếu User Service có thể được gọi trực tiếp bỏ qua Gateway,
            // nhưng chấp nhận được nếu Gateway là entry point duy nhất)
            username = usernameFromGateway;
            roles = (rolesFromGateway != null && !rolesFromGateway.isEmpty()) ?
                    Arrays.asList(rolesFromGateway.split(",")) : // Giả sử roles là chuỗi cách nhau bởi dấu phẩy
                    List.of(); // Hoặc List.of("ROLE_USER") mặc định nếu không có
            if (userIdFromGateway != null) {
                try {
                    userId = Long.parseLong(userIdFromGateway);
                } catch (NumberFormatException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            // Không có thông tin xác thực
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Nếu userId vẫn null và bạn cần nó, bạn có thể phải query DB dựa trên username
        // Ví dụ: Auth auth = authRepository.findByUsername(username).orElse(null);
        // if (auth != null && auth.getUser() != null) { userId = auth.getUser().getId(); }
        UserProfileResponse response = new UserProfileResponse(userId, username, roles);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(userMapper.toDTO(user)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
    }


    @GetMapping("/account-details")
    public ResponseEntity<UserAccountDetailsResponse> getAccountDetails(
            @RequestHeader("userId") String userIdFromGateway) {
        Long userId;
        try {
            userId = Long.parseLong(userIdFromGateway);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Auth> authOpt = authRepository.findByUserId(userId);

        if (userOpt.isEmpty() || authOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User user = userOpt.get();
        Auth auth = authOpt.get();

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
        return ResponseEntity.ok(response);

    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody UserRequestDTO userDTO) {
        User user = userMapper.toEntity(userDTO);
        User saved = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDTO(saved));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserRequestDTO userDTO) {
        try {
            User updated = userService.updateUserProfile(id, userMapper.toEntity(userDTO));
            return ResponseEntity.ok(userMapper.toDTO(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUserAndAuthById(id);
            return ResponseEntity.ok("User and associated auth deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
