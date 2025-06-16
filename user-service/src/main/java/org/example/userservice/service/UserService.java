package org.example.userservice.service;

import org.example.userservice.dto.request.UserRequestDTO;
import org.example.userservice.dto.response.UserAccountDetailsResponse;
import org.example.userservice.entity.Auth;
import org.example.userservice.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
     User save(User user);
     User createDefaultUser();
     Optional<User> findById(Long id);
     List<User> findAll();
     void deleteById(Long id);
     User updateUserProfile(Long id, User newUserData);
     void deleteUserAndAuthById(Long userId);
     User createAndAssociateUser(Auth auth);
}
