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
@Table(name = "slot_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotConfig {

    @Id
    @Column(name = "slot_code", length = 20)
    private String slotCode;         // CHINH, RAU, TINH_BOT, COMBO

    @Column(name = "slot_factor", precision = 2, scale = 1, nullable = false)
    private BigDecimal slotFactor;

    @Column(name = "min_g", nullable = false)
    private Integer minG;

    @Column(name = "max_g", nullable = false)
    private Integer maxG;

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
