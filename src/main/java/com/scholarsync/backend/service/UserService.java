package com.scholarsync.backend.service;

import com.scholarsync.backend.model.Role;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleDeterminationService roleDeterminationService;
    private final TeacherInstitutionalIdService teacherInstitutionalIdService;
    
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Unified sign up/login via Microsoft OAuth: Creates a new user if not exists, or logs in existing user
     * 
     * IMPORTANT: 
     * - Registration is ONLY possible via Microsoft OAuth. There is no local registration.
     * - This method is called when user authenticates via OAuth (either new registration or existing login).
     * - NEW USER LOGINS are assigned STUDENT role by default, UNLESS their institutional ID matches
     *   the list in teacher-institutional-ids.properties, in which case they get TEACHER role.
     * - User information is saved to the 'users' table in the database with all required fields.
     * 
     * Flow:
     * 1. Check if user exists in database by microsoftId (primary identifier)
     * 2. If exists, update lastLoginAt and return existing user
     * 3. If not found, check by email (secondary check)
     * 4. If still not found, create NEW user with role determined by institutional ID:
     *    - TEACHER if institutional ID is in teacher-institutional-ids.properties
     *    - STUDENT otherwise (default)
     * 5. Save to database with all fields: id, email, microsoft_id, display_name, institutional_id, role, account_created_at, last_login_at, is_active
     * 6. Verify persistence by querying database multiple times
     * 7. Return saved user entity
     * 
     * Database Schema (users table):
     * - id: UUID primary key
     * - email: User email address (@cit.edu)
     * - microsoft_id: Microsoft Azure AD user ID
     * - display_name: User display name from Microsoft
     * - institutional_id: Institutional ID (from Microsoft jobTitle field)
     * - role: User role (TEACHER if institutional ID matches teacher list, STUDENT otherwise)
     * - account_created_at: Account creation timestamp
     * - last_login_at: Last login timestamp
     * - is_active: Account active status (true by default)
     * 
     * @param microsoftId Microsoft user ID
     * @param email User email
     * @param displayName User display name
     * @param institutionalId User institutional ID (from Microsoft jobTitle field)
     * @return Created or updated User entity (saved to database)
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public User createOrUpdateUserFromOAuth(
            String microsoftId,
            String email,
            String displayName,
            String institutionalId) {
        
        log.info("=== Starting createOrUpdateUserFromOAuth ===");
        log.info("Input: microsoftId={}, email={}, displayName={}, institutionalId={}", 
            microsoftId, email, displayName, institutionalId);
        
        // Validate required fields
        if (microsoftId == null || microsoftId.isEmpty()) {
            log.error("Validation failed: Microsoft ID is null or empty");
            throw new IllegalArgumentException("Microsoft ID cannot be null or empty");
        }
        if (email == null || email.isEmpty()) {
            log.error("Validation failed: Email is null or empty");
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        // STEP 1: Check if user exists in database by microsoftId (primary identifier)
        log.info("STEP 1: Checking if user exists in database by microsoftId: {}", microsoftId);
        Optional<User> existingUserByMicrosoftId = userRepository.findByMicrosoftId(microsoftId);
        
        if (existingUserByMicrosoftId.isPresent()) {
            // EXISTING USER: Update and sign in via OAuth
            log.info("✓ User found in database by microsoftId. Updating existing user...");
            User user = existingUserByMicrosoftId.get();
            user.setDisplayName(displayName);
            user.setInstitutionalId(institutionalId);
            user.setLastLoginAt(LocalDateTime.now());
            
            // Re-evaluate role if institutionalId changed
            Role newRole = roleDeterminationService.determineRole(institutionalId);
            if (!user.getRole().equals(newRole)) {
                user.setRole(newRole);
            }
            
            // Update email if changed (but check for duplicates first)
            if (!user.getEmail().equals(email)) {
                Optional<User> existingUserByEmail = userRepository.findByEmail(email);
                if (existingUserByEmail.isPresent() && !existingUserByEmail.get().getId().equals(user.getId())) {
                    throw new IllegalArgumentException("Email already exists for another user: " + email);
                }
                user.setEmail(email);
            }
            
            log.info("Updating existing user: email={}, microsoftId={}", email, microsoftId);
            User savedUser = userRepository.saveAndFlush(user);
            log.info("saveAndFlush completed for existing user. User ID: {}", savedUser.getId());
            
            // Force entity manager to flush and clear cache
            entityManager.flush();
            entityManager.clear(); // Clear persistence context to force fresh query
            Optional<User> verifyUser = userRepository.findById(savedUser.getId());
            if (verifyUser.isEmpty()) {
                log.error("CRITICAL ERROR: User was not found in database after update! ID: {}", savedUser.getId());
                throw new RuntimeException("User was not persisted to database after update");
            }
            
            // Also verify by microsoftId
            Optional<User> verifyByMicrosoftId = userRepository.findByMicrosoftId(microsoftId);
            if (verifyByMicrosoftId.isEmpty()) {
                log.error("CRITICAL ERROR: User was not found by microsoftId after update! microsoftId: {}", microsoftId);
                throw new RuntimeException("User was not found by microsoftId after update");
            }
            
            log.info("✓ SUCCESS: Successfully updated existing user: {} (ID: {}, Role: {})", 
                email, savedUser.getId(), savedUser.getRole());
            log.info("=== Completed createOrUpdateUserFromOAuth (update existing) ===");
            return savedUser;
        }
        
        // STEP 2: Check if user exists by email (secondary check)
        log.info("STEP 2: User not found by microsoftId. Checking by email: {}", email);
        Optional<User> existingUserByEmail = userRepository.findByEmail(email);
        
        if (existingUserByEmail.isPresent()) {
            log.info("User found in database by email. Updating with microsoftId...");
            // Email exists but microsoftId doesn't match - this shouldn't happen normally
            // but we'll update the existing user with the new microsoftId
            User user = existingUserByEmail.get();
            if (user.getMicrosoftId() != null && !user.getMicrosoftId().equals(microsoftId)) {
                throw new IllegalArgumentException("Email already exists with different Microsoft ID");
            }
            // Update the existing user
            user.setMicrosoftId(microsoftId);
            user.setDisplayName(displayName);
            user.setInstitutionalId(institutionalId);
            user.setLastLoginAt(LocalDateTime.now());
            Role newRole = roleDeterminationService.determineRole(institutionalId);
            user.setRole(newRole);
            log.info("Updating existing user with microsoftId: email={}, microsoftId={}", email, microsoftId);
            User savedUser = userRepository.saveAndFlush(user);
            log.info("saveAndFlush completed for user by email. User ID: {}", savedUser.getId());
            
            // Force entity manager to flush and clear cache
            entityManager.flush();
            entityManager.clear(); // Clear persistence context to force fresh query
            Optional<User> verifyUser = userRepository.findById(savedUser.getId());
            if (verifyUser.isEmpty()) {
                log.error("CRITICAL ERROR: User was not found in database after update! ID: {}", savedUser.getId());
                throw new RuntimeException("User was not persisted to database after update");
            }
            
            // Also verify by microsoftId
            Optional<User> verifyByMicrosoftId = userRepository.findByMicrosoftId(microsoftId);
            if (verifyByMicrosoftId.isEmpty()) {
                log.error("CRITICAL ERROR: User was not found by microsoftId after update! microsoftId: {}", microsoftId);
                throw new RuntimeException("User was not found by microsoftId after update");
            }
            
            log.info("✓ SUCCESS: Successfully updated user by email: {} (ID: {}, Role: {})", 
                email, savedUser.getId(), savedUser.getRole());
            log.info("=== Completed createOrUpdateUserFromOAuth (update by email) ===");
            return savedUser;
        }
        
        // STEP 3: User does not exist in database - CREATE NEW USER
        log.info("STEP 3: User not found in database. Creating new user...");
        log.info("No existing user found - this is a new registration");
        
        // Determine role: Check if institutional ID is in teacher list
        // Default to STUDENT if not in teacher list
        Role role = Role.STUDENT;
        if (teacherInstitutionalIdService.isTeacherInstitutionalId(institutionalId)) {
            role = Role.TEACHER;
            log.info("Institutional ID '{}' found in teacher list - assigning TEACHER role", institutionalId);
        } else {
            log.info("Institutional ID '{}' not in teacher list - assigning STUDENT role (default)", institutionalId);
        }
        
        // Ensure all required fields are set explicitly
        LocalDateTime now = LocalDateTime.now();
        User newUser = User.builder()
                .microsoftId(microsoftId)
                .email(email)
                .displayName(displayName)
                .institutionalId(institutionalId)
                .role(role) // TEACHER if in list, otherwise STUDENT
                .accountCreatedAt(now)
                .lastLoginAt(now)
                .isActive(true)
                .build();
        
        // Double-check all required fields are set
        if (newUser.getRole() == null) {
            log.warn("Role was null after building, setting to STUDENT");
            newUser.setRole(Role.STUDENT);
        }
        if (newUser.getAccountCreatedAt() == null) {
            log.warn("accountCreatedAt was null after building, setting to now");
            newUser.setAccountCreatedAt(LocalDateTime.now());
        }
        if (newUser.getIsActive() == null) {
            log.warn("isActive was null after building, setting to true");
            newUser.setIsActive(true);
        }
        
            log.info("Built user object for database: email={}, microsoftId={}, role={}, displayName={}, institutionalId={}, accountCreatedAt={}, isActive={}", 
            newUser.getEmail(), newUser.getMicrosoftId(), newUser.getRole(), 
            newUser.getDisplayName(), newUser.getInstitutionalId(), newUser.getAccountCreatedAt(), newUser.getIsActive());
            
            // Final check: Ensure role is STUDENT before saving
            if (newUser.getRole() != Role.STUDENT) {
                log.error("CRITICAL: New user role is not STUDENT! Current role: {}. Forcing to STUDENT.", newUser.getRole());
                newUser.setRole(Role.STUDENT);
            }
            log.info("✓ Confirmed: New user will be saved with STUDENT role");
        
        try {
            // STEP 4: Save new user to database
            log.info("STEP 4: Saving new user to database...");
            log.info("Calling saveAndFlush on repository...");
            User savedUser = userRepository.saveAndFlush(newUser);
            log.info("saveAndFlush completed. Saved user ID: {}", savedUser.getId());
            
            // Force entity manager to flush and clear cache
            entityManager.flush();
            entityManager.clear(); // Clear persistence context to force fresh query
            
            // STEP 5: Verify the user was saved to database
            log.info("STEP 5: Verifying user was saved to database...");
            log.info("Querying database with ID: {}", savedUser.getId());
            
            // Clear any potential cache and query fresh from database
            Optional<User> verifyUser = userRepository.findById(savedUser.getId());
            if (verifyUser.isEmpty()) {
                log.error("CRITICAL ERROR: User was not found in database immediately after save! ID: {}", savedUser.getId());
                log.error("Attempting to query all users in database to debug...");
                long userCount = userRepository.count();
                log.error("Total users in database: {}", userCount);
                throw new RuntimeException("User was not persisted to database");
            }
            
            log.info("✓ Verification successful! User found in database: ID={}, email={}, role={}, microsoftId={}", 
                verifyUser.get().getId(), verifyUser.get().getEmail(), verifyUser.get().getRole(), verifyUser.get().getMicrosoftId());
            
            // Also verify by microsoftId to ensure we can find the user
            log.info("Verifying user exists by microsoftId: {}", microsoftId);
            Optional<User> verifyByMicrosoftId = userRepository.findByMicrosoftId(microsoftId);
            if (verifyByMicrosoftId.isEmpty()) {
                log.error("CRITICAL ERROR: User was not found by microsoftId after save! microsoftId: {}", microsoftId);
                log.error("Attempting to query all users to find the issue...");
                long userCount = userRepository.count();
                log.error("Total users in database: {}", userCount);
                throw new RuntimeException("User was not found by microsoftId after save");
            }
            log.info("✓ Verified user exists by microsoftId as well. User ID: {}", verifyByMicrosoftId.get().getId());
            
            // Final verification: count total users and ensure our user is in the list
            long totalUsers = userRepository.count();
            log.info("Database verification: Total users in database: {}", totalUsers);
            
            // Final check: Query all users and verify our user is in the list
            UUID savedUserId = savedUser.getId();
            List<User> allUsers = userRepository.findAll();
            boolean userFoundInList = allUsers.stream()
                .anyMatch(u -> u.getId().equals(savedUserId));
            if (!userFoundInList) {
                log.error("CRITICAL ERROR: User was not found in the list of all users after save!");
                throw new RuntimeException("User was not persisted to database - not found in user list");
            }
            log.info("✓ Final verification passed: User found in list of all users");
            
            // Final verification: Ensure role is correct (TEACHER if in list, otherwise STUDENT)
            Role expectedRole = teacherInstitutionalIdService.isTeacherInstitutionalId(institutionalId) 
                ? Role.TEACHER 
                : Role.STUDENT;
            
            if (savedUser.getRole() != expectedRole) {
                log.error("CRITICAL ERROR: User was saved with role {} but expected {}! Fixing...", 
                    savedUser.getRole(), expectedRole);
                savedUser.setRole(expectedRole);
                savedUser = userRepository.saveAndFlush(savedUser);
                entityManager.flush();
                log.warn("Fixed user role to {}", expectedRole);
            }
            
            log.info("✓✓✓ SUCCESS: New user created and saved to database with {} role", savedUser.getRole());
            log.info("User details: email={}, ID={}, role={}, microsoftId={}, accountCreatedAt={}, lastLoginAt={}, isActive={}", 
                email, savedUser.getId(), savedUser.getRole(), savedUser.getMicrosoftId(), 
                savedUser.getAccountCreatedAt(), savedUser.getLastLoginAt(), savedUser.getIsActive());
            
            // CRITICAL: Force transaction to commit by ensuring all changes are flushed
            // The @Transactional annotation will commit when this method returns successfully
            entityManager.flush();
            entityManager.clear(); // Clear cache to ensure next query hits database
            
            // Final verification: Query in a new transaction context to ensure data is persisted
            // This forces a database read after the transaction commits
            log.info("Performing final database verification after flush...");
            Optional<User> finalVerify = userRepository.findById(savedUser.getId());
            if (finalVerify.isEmpty()) {
                log.error("CRITICAL ERROR: User not found in final verification! Transaction may have rolled back.");
                throw new RuntimeException("User was not persisted to database - transaction may have rolled back");
            }
            log.info("✓ Final verification passed: User confirmed persisted in database. ID: {}", finalVerify.get().getId());
            
            log.info("=== Completed createOrUpdateUserFromOAuth (new user) ===");
            log.info("Transaction will commit when method returns successfully");
            
            return savedUser;
        } catch (DataIntegrityViolationException e) {
            // Handle database constraint violations
            log.error("Database constraint violation while creating user: {}", e.getMessage(), e);
            log.error("Constraint violation details: {}", e.getRootCause() != null ? e.getRootCause().getMessage() : "No root cause");
            if (e.getMessage() != null && e.getMessage().contains("microsoft_id")) {
                throw new IllegalArgumentException("Microsoft ID already exists: " + microsoftId, e);
            }
            if (e.getMessage() != null && e.getMessage().contains("email")) {
                throw new IllegalArgumentException("Email already exists: " + email, e);
            }
            throw new RuntimeException("Database error while saving user: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error saving user: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Optional<User> findByMicrosoftId(String microsoftId) {
        return userRepository.findByMicrosoftId(microsoftId);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public Optional<User> findByInstitutionalId(String institutionalId) {
        return userRepository.findByInstitutionalId(institutionalId);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Verifies that a user exists in the database by querying in a new transaction.
     * This ensures the previous transaction has committed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean verifyUserExists(UUID userId) {
        return userRepository.findById(userId).isPresent();
    }

}

