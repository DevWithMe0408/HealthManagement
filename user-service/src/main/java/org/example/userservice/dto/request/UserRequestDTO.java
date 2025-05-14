package org.example.userservice.dto.request;

import lombok.Data;
import org.example.userservice.enums.Gender;

import java.time.LocalDate;

@Data
public class UserRequestDTO {
    private String name;
    private String phone;
    private LocalDate birthDate;
    private Gender gender;
}
