package org.example.nutritionservice.entity.config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "surplus_penalty_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurplusPenaltyConfig {

    @Id
    @Column(name = "macro_code", length = 10)
    private String macroCode;        // PROTEIN, FAT, CARB, KCAL

    @Column(precision = 2, scale = 1, nullable = false)
    private BigDecimal factor;

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
