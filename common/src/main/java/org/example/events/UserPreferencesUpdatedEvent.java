package org.example.events;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserPreferencesUpdatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String prefKey;
    private String prefValue;

    public UserPreferencesUpdatedEvent() {
    }

    public UserPreferencesUpdatedEvent(String userId, String prefKey, String prefValue) {
        this.userId = userId;
        this.prefKey = prefKey;
        this.prefValue = prefValue;
    }
}
