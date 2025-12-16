package com.scholarsync.backend.service;

import com.scholarsync.backend.model.Role;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleDeterminationService roleDeterminationService;

    /**
     * Unified sign up/login via Microsoft OAuth: Creates a new user if not exists, or logs in existing user
     * 
     * IMPORTANT: Registration is ONLY possible via Microsoft OAuth. There is no local registration.
     * This method is called when user authenticates via OAuth (either new registration or existing login).
     * 
     * @param microsoftId Microsoft user ID
     * @param email User email
     * @param displayName User display name
     * @param institutionalId User institutional ID (from Microsoft jobTitle field)
     * @return Created or updated User entity
     */
    @Transactional
    public User createOrUpdateUserFromOAuth(
            String microsoftId,
            String email,
            String displayName,
            String institutionalId) {
        
        Optional<User> existingUser = userRepository.findByMicrosoftId(microsoftId);
        boolean isNewUser = existingUser.isEmpty();
        
        if (isNewUser) {
            // NEW USER: Auto-register via OAuth (registration only possible via OAuth)
            Role role = roleDeterminationService.determineRole(institutionalId);
            
            User newUser = User.builder()
                    .microsoftId(microsoftId)
                    .email(email)
                    .displayName(displayName)
                    .institutionalId(institutionalId)
                    .role(role)
                    .accountCreatedAt(LocalDateTime.now())
                    .lastLoginAt(LocalDateTime.now())
                    .isActive(true)
                    .build();
            
            return userRepository.save(newUser);
        } else {
            // EXISTING USER: Update and sign in via OAuth
            User user = existingUser.get();
            user.setDisplayName(displayName);
            user.setInstitutionalId(institutionalId);
            user.setLastLoginAt(LocalDateTime.now());
            
            // Re-evaluate role if institutionalId changed
            Role newRole = roleDeterminationService.determineRole(institutionalId);
            if (!user.getRole().equals(newRole)) {
                user.setRole(newRole);
            }
            
            // Update email if changed
            if (!user.getEmail().equals(email)) {
                user.setEmail(email);
            }
            
            return userRepository.save(user);
        }
    }

    public Optional<User> findByMicrosoftId(String microsoftId) {
        return userRepository.findByMicrosoftId(microsoftId);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

}

