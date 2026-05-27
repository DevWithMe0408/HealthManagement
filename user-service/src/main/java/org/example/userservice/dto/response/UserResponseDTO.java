package org.example.userservice.dto.response;

import lombok.Data;
import org.example.userservice.enums.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserResponseDTO {
    private String id;
    private String name;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private Gender gender;
    private Boolean profileCompleted;
    private LocalDateTime createdAt;
}
