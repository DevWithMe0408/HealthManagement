package org.example.userservice.service;

import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User save(User user);
    User createDefaultUser();
    Optional<User> findById(String id);
    List<User> findAll();
    void deleteById(String id);
    User updateUserProfile(String id, User newUserData);
    void markProfileCompleted(String userId);
    void deleteUserAndAuthById(String userId);
    User createAndAssociateUser(Auth auth);
}
