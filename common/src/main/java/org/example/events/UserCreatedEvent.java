package org.example.events;

import lombok.Data;

import java.io.Serializable;
@Data
public class UserCreatedEvent  implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String username;
    private String email;

    // Constructor không tham số (cần cho một số thư viện deserialization như Jackson)
    public UserCreatedEvent() {}

    public UserCreatedEvent(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }
    @Override
    public String toString() {
        return "UserCreatedEvent{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
