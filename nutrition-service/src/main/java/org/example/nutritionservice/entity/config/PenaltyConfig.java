package org.example.nutritionservice.entity.config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "penalty_configs")
@IdClass(PenaltyConfigId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyConfig {

    @Id
    @Column(nullable = false)
    private Integer layer;           // 1 or 2

    @Id
    @Column(name = "distance_days", nullable = false)
    private Integer distanceDays;    // 0, 1, 2

    @Column(name = "penalty_value", nullable = false)
    private Integer penaltyValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;
}
