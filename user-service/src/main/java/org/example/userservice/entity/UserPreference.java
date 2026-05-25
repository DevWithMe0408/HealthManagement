package org.example.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@IdClass(UserPreferenceId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Id
    @Column(name = "pref_key", length = 100, nullable = false)
    private String prefKey;

    @Column(name = "pref_value", nullable = false, length = 500)
    private String prefValue;

    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType;

    @Column(name = "description", length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
