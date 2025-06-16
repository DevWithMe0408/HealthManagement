package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import org.example.events.UserCreatedEvent;
import org.example.userservice.dto.request.LoginRequest;
import org.example.userservice.dto.request.RegisterRequest;
import org.example.userservice.dto.response.TokenRefreshResponse;
import org.example.userservice.entity.Auth;
import org.example.userservice.entity.RefreshToken;
import org.example.userservice.entity.User;
import org.example.userservice.enums.Role;
import org.example.userservice.exception.TokenRefreshException;
import org.example.userservice.exception.UsernameAlreadyExistsException;
import org.example.userservice.repository.AuthRepository;
import org.example.userservice.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    @Autowired
    private AuthRepository authRepository;


    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.user-events}")
    private String userEventsExchangeName;

    @Value("${app.rabbitmq.routing-key.user-created}")
    private String userCreateRoutingKey;

    /**
     * Check if a user exists by username
     * @param username the username to check
     * @return true if user exists, false otherwise
     */
    @Override
    public boolean existsByUsername(String username) {
        return authRepository.existsByUsername(username);
    }

    /**
     * Check if a user exists by email
     * @param email the email to check
     * @return true if user exists, false otherwise
     */
    @Override
    public boolean existsByEmail(String email) {
        return authRepository.existsByEmail(email);
    }

    /**
     * Save a new user
     * @param user the user to save
     * @return the saved user
     */
    @Override
    public Auth save(Auth user) {
        return authRepository.save(user);
    }
    /**
     * Create a new account
     * @param request the request containing user information
     */
    @Override
    @Transactional
    public void registerUser(RegisterRequest request) {
        if (existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username is already taken");
        }
        if (existsByEmail(request.getEmail())) {
            throw new UsernameAlreadyExistsException("Email is already in use");
        }
        // 1. Tạo và lưu Auth
        Auth auth = new Auth(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getEmail()
        );
        auth.setRole(Role.ROLE_USER);
        Auth saveAuth = authRepository.save(auth); // Lưu auth trước để có ID
        // 2. Tạo User và liên kết với Auth
        User saveUser = userService.createAndAssociateUser(saveAuth);

        // 3. Cập nhật lại Auth để có tham chiếu ngược tới User (nếu cần và có mapping)
        // Điều này phụ thuộc vào thiết kế entity của bạn.
        // Nếu Auth có trường user_id hoặc @OneToOne(mappedBy="auth") User user;
        // và User là owning side (có auth_id) thì bước này có thể không cần thiết sau khi user đã lưu.
        saveAuth.setUser(saveUser);
        authRepository.save(saveAuth);

        // --- BẮT ĐẦU PHẦN GỬI MESSAGE RABBITMQ ---
        // 4. Tạo Event DTO (sử dụng DTO từ module common-events bạn đã tạo)
        UserCreatedEvent event = new UserCreatedEvent(
                saveUser.getId().toString(),
                saveAuth.getUsername(),
                saveAuth.getEmail()
        );
        // 5. Gửi event tới RabbitMQ
        try {
            log.info("Sending UserCreatedEvent for userId: {}, username: {}",event.getUserId(),event.getUsername());
            rabbitTemplate.convertAndSend(userEventsExchangeName,userCreateRoutingKey,event);
            log.info("UserCreatedEvent sent successfully to exchange '{}' with routing key '{}'", userEventsExchangeName, userCreateRoutingKey);
        } catch (Exception e) {
            // Xử lý lỗi gửi message:
            // - Log lỗi nghiêm trọng.
            // - Cân nhắc:
            //    - Không làm gì cả (eventual consistency, có thể có job retry sau này hoặc giám sát).
            //    - Ném một exception tùy chỉnh nếu việc gửi event là cực kỳ quan trọng và bạn muốn transaction rollback.
            //      Tuy nhiên, điều này sẽ làm User Service phụ thuộc vào sự sẵn sàng của RabbitMQ.
            //      Thường thì không nên để việc tạo user thất bại chỉ vì không gửi được event.
            //    - Ghi lại event vào một "outbox table" trong DB để một tiến trình khác xử lý gửi (Transactional Outbox Pattern).
            log.error("Failed to send UserCreatedEvent for userId: {}. Error: {}", event.getUserId(), e.getMessage(), e);
            // Ví dụ: throw new EventPublishingException("Failed to publish UserCreatedEvent", e);
            // Nếu bạn ném exception ở đây, @Transactional sẽ rollback toàn bộ.
        }
        log.info("User {} registered successfully. Auth ID: {}, User ID: {}", saveAuth.getUsername(), saveAuth.getId(), saveUser.getId());

    }
    @Override
    public TokenRefreshResponse loginUser(LoginRequest request) {
        log.info("Authenticating user: {}", request.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        log.info("Authentication successful for user: {}", request.getUsername());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtil.generateToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(request.getUsername());


        return new TokenRefreshResponse(jwt, refreshToken.getToken());
    }

    @Override
    public Optional<TokenRefreshResponse> refreshAccessToken(String refreshTokenString) {
        return refreshTokenService.findByToken(refreshTokenString)
                .map(refreshToken -> {
                    if (refreshTokenService.isTokenExpired(refreshToken)) {
                        refreshTokenService.delete(refreshToken); // Xóa token hết hạn
                        throw new TokenRefreshException(refreshTokenString, "Refresh token was expired. Please make a new signin request");
                    }
                    // Lấy thông tin Auth từ RefreshToken
                    Auth auth = refreshToken.getAuth();
                    // Tạo đối tượng Authentication mới từ thông tin user trong Auth
                    // Đây là cách tạo Authentication object mà không cần password,
                    // vì refresh token đã chứng minh user đã từng xác thực.
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            auth.getUsername(),
                            null, // Password không cần thiết ở đây
                            List.of(new SimpleGrantedAuthority(auth.getRole().name()))
                    );
                    String newJwt = jwtUtil.generateToken(authentication);
                    return new TokenRefreshResponse(newJwt, refreshToken.getToken());
                });
    }
}
