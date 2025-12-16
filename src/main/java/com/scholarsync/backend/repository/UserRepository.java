package com.scholarsync.backend.repository;

import com.scholarsync.backend.model.Role;
import com.scholarsync.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByMicrosoftId(String microsoftId);
    Optional<User> findByEmail(String email);
    Optional<User> findByInstitutionalId(String institutionalId);
    boolean existsByMicrosoftId(String microsoftId);
    boolean existsByEmail(String email);
    boolean existsByInstitutionalId(String institutionalId);
    
    // Find users by role (useful for filtering students, teachers, etc.)
    List<User> findByRole(Role role);
}


