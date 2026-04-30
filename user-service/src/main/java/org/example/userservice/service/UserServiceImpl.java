package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import org.example.events.UserProfileUpdatedEvent;
import org.example.userservice.common.util.UuidV7Generator;
import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;
import org.example.userservice.exception.BusinessException;
import org.example.userservice.exception.ErrorCode;
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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Value("${app.rabbitmq.exchange.user-events}")
    private String userEventsExchangeName;

    @Value("${app.rabbitmq.routing-key.user-profile-updated}")
    private String userProfileUpdatedRoutingKey;

    @Autowired
    private final RabbitTemplate rabbitTemplate;
    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public User createDefaultUser() {
        User user = new User();
        user.setId(UuidV7Generator.generate());
        user.setName("Unknown");
        user.setGender(null);
        user.setPhone(null);
        user.setBirthDate(null);
        user.setAge(null);
        return user;
    }

    @Override
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public void deleteById(String id) {
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public User updateUserProfile(String id, User newUserDataRequest) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        boolean birthDateActuallyChanged = false;
        boolean genderActuallyChanged = false;

        if (newUserDataRequest.getName() != null &&
                !newUserDataRequest.getName().equals(existingUser.getName())) {
            existingUser.setName(newUserDataRequest.getName());
        }
        if (newUserDataRequest.getPhone() != null &&
                !newUserDataRequest.getPhone().equals(existingUser.getPhone())) {
            existingUser.setPhone(newUserDataRequest.getPhone());
        }

        if (newUserDataRequest.getBirthDate() != null && !newUserDataRequest.getBirthDate().equals(existingUser.getBirthDate())) {
            existingUser.setBirthDate(newUserDataRequest.getBirthDate());
            birthDateActuallyChanged = true;
        }

        if (newUserDataRequest.getGender() != null && newUserDataRequest.getGender() != existingUser.getGender()) {
            existingUser.setGender(newUserDataRequest.getGender());
            genderActuallyChanged = true;
        }

        if (existingUser.getBirthDate() != null) {
            int calculatedAge = Period.between(existingUser.getBirthDate(), LocalDate.now()).getYears();
            if (existingUser.getAge() == null || existingUser.getAge() != calculatedAge) {
                existingUser.setAge(calculatedAge);
            }
        } else {
            existingUser.setAge(null);
        }
        User updatedUser = userRepository.save(existingUser);

        if (birthDateActuallyChanged || genderActuallyChanged) {
            LocalDate birthDateForEvent = updatedUser.getBirthDate();
            String genderForEvent = (updatedUser.getGender() != null) ? updatedUser.getGender().name() : null;

            UserProfileUpdatedEvent profileUpdatedEvent = new UserProfileUpdatedEvent(
                    updatedUser.getId(),
                    birthDateForEvent,
                    genderForEvent
            );
            try {
                rabbitTemplate.convertAndSend(userEventsExchangeName, userProfileUpdatedRoutingKey, profileUpdatedEvent);
                log.info("Sent UserProfileUpdateEvent for userId: {} due to profile update. BirthDate: {}, Gender: {}",
                        updatedUser.getId(), birthDateForEvent, genderForEvent);
            } catch (Exception e) {
                log.error("Failed to send UserProfileUpdateEvent for userId: {}. Error: {}", updatedUser.getId(), e.getMessage(), e);
            }
        }
        return updatedUser;
    }

    @Override
    @Transactional
    public void deleteUserAndAuthById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Auth auth = user.getAuth();

        // User owns the FK to Auth, so delete User first to release the FK,
        // then delete Auth (refresh tokens reference Auth, clear those too).
        if (auth != null) {
            refreshTokenRepository.deleteByAuth(auth);
        }
        userRepository.delete(user);
        if (auth != null) {
            authRepository.delete(auth);
        }
    }

    @Override
    @Transactional
    public User createAndAssociateUser(Auth auth) {
        User user = createDefaultUser();
        user.setAuth(auth);
        return userRepository.save(user);
    }
}
