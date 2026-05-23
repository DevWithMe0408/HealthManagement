package org.example.nutritionservice.entity.config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    @Id
    @Column(name = "config_key", length = 100)
    private String configKey; // mã cấu hình

    @Column(name = "config_value", length = 500, nullable = false)
    private String configValue; // Giá trị cấu hình

    @Column(name = "value_type", length = 20, nullable = false)
    private String valueType;        // DECIMAL, INT, JSON_ARRAY, BOOLEAN

    @Column(length = 255)
    private String description; // Mô tả cấu hình

    @Column(nullable = false)
    private Boolean editable = true;

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
