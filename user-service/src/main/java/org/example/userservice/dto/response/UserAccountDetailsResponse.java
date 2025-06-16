package org.example.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.userservice.enums.Gender;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccountDetailsResponse {
    private Long userId;
    private String userName;
    private String email;
    private List<String> roles;

    private String name;
    private String phoneNumber;
    private LocalDate birthDate;
    private Gender gender;
}
