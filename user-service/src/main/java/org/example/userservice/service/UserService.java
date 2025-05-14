package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;
import org.example.userservice.repository.AuthRepository;
import org.example.userservice.repository.RefreshTokenRepository;
import org.example.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
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

    public User updateUser(Long id, User newUserData) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setName(newUserData.getName());
                    user.setPhone(newUserData.getPhone());
                    user.setBirthDate(newUserData.getBirthDate());
                    user.setGender(newUserData.getGender());
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
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
}
