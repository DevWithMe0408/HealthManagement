package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import org.example.events.UserProfileUpdatedEvent;
import org.example.userservice.dto.request.UserRequestDTO;
import org.example.userservice.dto.response.UserAccountDetailsResponse;
import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;
import org.example.userservice.enums.Gender;
import org.example.userservice.repository.AuthRepository;
import org.example.userservice.repository.RefreshTokenRepository;
import org.example.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Value("${app.rabbitmq.exchange.user-events}")
    private String userEventsExchangeName;

    @Value("${app.rabbitmq.routing-key.user-profile-updated}") // Định nghĩa trong application.properties
    private String userProfileUpdatedRoutingKey;

    @Autowired
    private final RabbitTemplate rabbitTemplate;
    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public User save(User user) {
        return userRepository.save(user);
    }
    public User createDefaultUser() {
        User user = new User();
        user.setName("Unknown");
        user.setGender(null);
        user.setPhone(null);
        user.setBirthDate(null);
        user.setAge(null);
        return user;
    }
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public User updateUserProfile(Long id, User newUserDataRequest) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
//        boolean profileChanged = false;
        boolean birthDateActuallyChanged = false;
        boolean genderActuallyChanged = false;

        // Cập nhật name (ví dụ, không ảnh hưởng đến event profile chính)
        if (newUserDataRequest.getName() != null &&
                !newUserDataRequest.getName().equals(existingUser.getName())) {
            existingUser.setName(newUserDataRequest.getName());
        }
        // Phone
        if (newUserDataRequest.getPhone() != null && // Giả sử User entity có getPhoneNumber()
                !newUserDataRequest.getPhone().equals(existingUser.getPhone())) {
            existingUser.setPhone(newUserDataRequest.getPhone());
        }

        // Cập nhật birthDate và kiểm tra thay đổi
        if (newUserDataRequest.getBirthDate() != null && !newUserDataRequest.getBirthDate().equals(existingUser.getBirthDate())) {
            existingUser.setBirthDate(newUserDataRequest.getBirthDate());
            birthDateActuallyChanged = true;
        }

        // Cập nhật gender và kiểm tra thay đổi
        // Giả sử Gender là ENUM
        if (newUserDataRequest.getGender() != null && newUserDataRequest.getGender() != existingUser.getGender()) {
            existingUser.setGender(newUserDataRequest.getGender());
            genderActuallyChanged = true;
        }
        // Tính toán lại age nếu birthDate thay đổi hoặc nếu user không có age (lần cập nhật đầu)
        // Hoặc nếu bạn muốn client tự truyền age đã tính, thì chỉ set nếu nó được cung cấp.
        // Khuyến nghị: Chỉ lưu birthDate, tính age khi cần. Nếu vẫn muốn lưu age:
        if (existingUser.getBirthDate() != null) {
            int calculatedAge = Period.between(existingUser.getBirthDate(), LocalDate.now()).getYears();
            if (existingUser.getAge() == null || existingUser.getAge() != calculatedAge) {
                existingUser.setAge(calculatedAge); // Cập nhật age
                // Việc thay đổi age tự nó (nếu birth_date không đổi) có thể không cần trigger event
                // vì event chính nên dựa trên birth_date và gender.
            }
        } else {
                 existingUser.setAge(null);

        }
        User updatedUser = userRepository.save(existingUser);

        // Chỉ gửi sự kiện nếu birthDate hoặc gender thực sự thay đổi
        if (birthDateActuallyChanged || genderActuallyChanged) {
            // Đảm bảo birthDate và gender không null trước khi gửi, hoặc DTO chấp nhận null
            LocalDate birthDateForEvent = updatedUser.getBirthDate();
            String genderForEvent = (updatedUser.getGender() != null) ? updatedUser.getGender().name() : null; // Lấy tên ENUM

            UserProfileUpdatedEvent profileUpdatedEvent = new UserProfileUpdatedEvent(
                    updatedUser.getId().toString(),
                    birthDateForEvent,
                    genderForEvent
            );
            try {
                rabbitTemplate.convertAndSend(userEventsExchangeName, userProfileUpdatedRoutingKey, profileUpdatedEvent);
                log.info("Sent UserProfileUpdateEvent for userId: {} due to profile update. BirthDate: {}, Gender:: {}",
                        updatedUser.getId(),birthDateForEvent,genderForEvent);
            } catch (Exception e) {
                // Xử lý lỗi gửi message (log, hoặc chiến lược retry nếu cần)
                log.error("Failed to send UserProfileUpdateEvent for userId: {}. Error: {}", updatedUser.getId(), e.getMessage(), e);
                // Không nên ném exception ở đây để làm rollback DB, trừ khi event này cực kỳ critical.

            }
        }
        return updatedUser;

    }


    @Transactional
    public void deleteUserAndAuthById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Auth auth = user.getAuth(); // liên kết bidirectional

        if (auth != null) {
            refreshTokenRepository.deleteByAuth(auth); // Xoá refresh token trước
            authRepository.delete(auth);               // Xoá auth
        }

        userRepository.delete(user); // Cuối cùng xoá user
    }
    @Transactional
    @Override
    public User createAndAssociateUser(Auth auth) {
        User user = createDefaultUser();
        user.setAuth(auth);
        return userRepository.save(user);
    }
}
