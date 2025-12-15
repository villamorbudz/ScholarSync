package com.scholarsync.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email"),
    @UniqueConstraint(columnNames = "microsoftId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true, name = "microsoft_id")
    private String microsoftId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "job_title")
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.UNKNOWN;

    // AuthenticationMethod is tracked in session/JWT only, not stored in the database

    @Column(name = "account_created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime accountCreatedAt = LocalDateTime.now();

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_oauth_sync_at")
    private LocalDateTime lastOAuthSyncAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;
}
