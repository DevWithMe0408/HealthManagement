package org.example.userservice.dto.response;

import lombok.Data;
import org.example.userservice.enums.Gender;

import java.time.LocalDate;

@Data
public class UserResponseDTO {
    private Long id;
    private String name;
    private String phone;
    private LocalDate birthDate;
    private Gender gender;
}
