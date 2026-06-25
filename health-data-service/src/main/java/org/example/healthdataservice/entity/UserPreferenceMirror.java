package org.example.healthdataservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preference_mirror")
@IdClass(UserPreferenceMirrorId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceMirror {

    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Id
    @Column(name = "pref_key", length = 100, nullable = false)
    private String prefKey; // ten thuoc tinh

    @Column(name = "pref_value", nullable = false, length = 500)
    private String prefValue; // gia tri thuoc tinh

    @UpdateTimestamp
    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;
}
