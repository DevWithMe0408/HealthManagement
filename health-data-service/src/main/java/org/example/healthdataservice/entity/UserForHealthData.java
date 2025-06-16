package org.example.healthdataservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.healthdataservice.entity.enums.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_for_health_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserForHealthData {

    @Id
    @Column(name = "user_id", nullable = false,unique = true)
    private Long userId;

    @Column(name = "birth_data")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt; // Thời điểm bản ghi này được cập nhật lần cuối

    // Bạn không cần lưu 'age' trực tiếp ở đây.
    // Tuổi có thể được tính toán động khi cần từ birthDate.
    // Nếu bạn thực sự muốn cache tuổi (ví dụ để query), thì cần logic cập nhật nó.
    // @Transient // Để không map vào cột DB nếu chỉ tính toán
    // public Integer getAge() {
    //     if (birthDate == null) {
    //         return null;
    //     }
    //     return Period.between(birthDate, LocalDate.now()).getYears();
    // }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
}
