package org.example.userservice.mapper;

import org.example.userservice.dto.request.UserRequestDTO;
import org.example.userservice.dto.response.UserResponseDTO;
import org.example.userservice.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRequestDTO dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setPhoneProvided(dto.isPhoneProvided());
        user.setBirthDate(dto.getBirthDate());
        user.setGender(dto.getGender());
        return user;
    }

    public void updateEntity(User user, UserRequestDTO dto) {
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setBirthDate(dto.getBirthDate());
        user.setGender(dto.getGender());
    }

    public UserResponseDTO toDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getAuth() != null ? user.getAuth().getEmail() : null);
        dto.setPhone(user.getPhone());
        dto.setBirthDate(user.getBirthDate());
        dto.setGender(user.getGender());
        dto.setProfileCompleted(Boolean.TRUE.equals(user.getProfileCompleted()));
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
