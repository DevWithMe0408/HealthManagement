package org.example.healthdataservice.repository;

import org.example.healthdataservice.entity.UserForHealthData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserForHealthDataRepository extends JpaRepository<UserForHealthData, Long> {
    // findByUserId sẽ tự động được cung cấp bởi JpaRepository vì userId là @Id
    // Optional<UserForHealthData> findByUserId(Long userId); // Không cần nếu userId là @Id
}
