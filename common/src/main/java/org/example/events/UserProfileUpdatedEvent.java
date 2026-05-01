package org.example.events;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data

public class UserProfileUpdatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private LocalDate birthDate;
    private String gender;

    public UserProfileUpdatedEvent() {}

    public UserProfileUpdatedEvent(String userId, LocalDate birthDate, String gender) {
        this.userId = userId;
        this.birthDate = birthDate;
        this.gender = gender;
    }
}
