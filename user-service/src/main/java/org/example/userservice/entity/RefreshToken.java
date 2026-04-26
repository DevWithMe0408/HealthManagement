package org.example.userservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
public class RefreshToken {

    @Id
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @OneToOne
    @JoinColumn(name = "auth_id", referencedColumnName = "id", nullable = false, unique = true)
    private Auth auth;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;
}
