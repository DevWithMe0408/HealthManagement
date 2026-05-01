package org.example.userservice.repository;

import org.example.userservice.entity.User;
import org.example.userservice.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByAuth_Id(String authId);

    @Query("""
        SELECT u FROM User u
        WHERE (:search IS NULL OR :search = ''
               OR LOWER(u.auth.username) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.auth.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:role IS NULL OR u.auth.role = :role)
          AND (:hasProfile IS NULL
               OR (:hasProfile = TRUE AND u.birthDate IS NOT NULL AND u.gender IS NOT NULL)
               OR (:hasProfile = FALSE AND (u.birthDate IS NULL OR u.gender IS NULL)))
        """)
    Page<User> findAllWithFilters(
            @Param("search") String search,
            @Param("role") Role role,
            @Param("hasProfile") Boolean hasProfile,
            Pageable pageable);
}
