package com.scholarsync.backend.controller;

import com.scholarsync.backend.model.Role;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.service.RoleDeterminationService;
import com.scholarsync.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Test Controller for development/testing purposes
 * These endpoints are public and should be disabled in production
 */
@RestController
@RequestMapping("/api/public/test")
@RequiredArgsConstructor
public class TestController {

    private final RoleDeterminationService roleDeterminationService;
    private final UserService userService;

    /**
     * Test role determination with a given institutional ID
     * 
     * POST /api/public/test/role
     * Body: { "institutionalId": "1-1234" }
     */
    @PostMapping("/role")
    public ResponseEntity<Map<String, Object>> testRoleDetermination(
            @RequestBody Map<String, String> request) {
        
        String institutionalId = request.get("institutionalId");
        Map<String, Object> response = new HashMap<>();
        
        if (institutionalId == null || institutionalId.isEmpty()) {
            response.put("success", false);
            response.put("message", "institutionalId is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        Role determinedRole = roleDeterminationService.determineRole(institutionalId);
        
        response.put("success", true);
        response.put("institutionalId", institutionalId);
        response.put("determinedRole", determinedRole.toString());
        response.put("patternMatched", determinePattern(institutionalId));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Create a test teacher user with mock data
     * 
     * POST /api/public/test/create-teacher
     * Body: { "email": "teacher@test.com", "institutionalId": "1-1234", "displayName": "Test Teacher" }
     */
    @PostMapping("/create-teacher")
    public ResponseEntity<Map<String, Object>> createTestTeacher(
            @RequestBody Map<String, String> request) {
        
        String email = request.getOrDefault("email", "teacher.test@cit.edu");
        String institutionalId = request.getOrDefault("institutionalId", "1-1234");
        String displayName = request.getOrDefault("displayName", "Test Teacher");
        
        // Check if user already exists
        Optional<User> existingUser = userService.findByEmail(email);
        if (existingUser.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "User with this email already exists");
            response.put("user", mapUserToResponse(existingUser.get()));
            return ResponseEntity.badRequest().body(response);
        }
        
        // Determine role
        Role role = roleDeterminationService.determineRole(institutionalId);
        
        // Create test teacher user
        User testTeacher = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .microsoftId("test-teacher-" + UUID.randomUUID().toString())
                .displayName(displayName)
                .jobTitle(institutionalId)
                .role(role)
                .accountCreatedAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .lastOAuthSyncAt(LocalDateTime.now())
                .isActive(true)
                .emailVerified(true)
                .build();
        
        // Note: This bypasses the normal OAuth flow, so we'll save directly
        // In a real scenario, you'd use UserService.createOrUpdateUserFromOAuth()
        // For testing, we'll need to inject the repository or use UserService
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Test teacher user created (mock - not saved to DB)");
        response.put("user", mapUserToResponse(testTeacher));
        response.put("note", "This is a mock user. To actually save, use UserService or database directly. Local fallback passwords are no longer used.");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Test all role patterns
     * 
     * GET /api/public/test/role-patterns
     */
    @GetMapping("/role-patterns")
    public ResponseEntity<Map<String, Object>> testAllRolePatterns() {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> testCases = new HashMap<>();
        
        // Student patterns
        testCases.put("22-1234-567", roleDeterminationService.determineRole("22-1234-567").toString());
        testCases.put("23-5678-901", roleDeterminationService.determineRole("23-5678-901").toString());
        testCases.put("2010-12345", roleDeterminationService.determineRole("2010-12345").toString());
        testCases.put("2020-98765", roleDeterminationService.determineRole("2020-98765").toString());
        
        // Teacher patterns
        testCases.put("1-0001", roleDeterminationService.determineRole("1-0001").toString());
        testCases.put("1-1234", roleDeterminationService.determineRole("1-1234").toString());
        testCases.put("1-9999", roleDeterminationService.determineRole("1-9999").toString());
        
        // Admin patterns
        testCases.put("ADMIN", roleDeterminationService.determineRole("ADMIN").toString());
        testCases.put("ADMINISTRATOR", roleDeterminationService.determineRole("ADMINISTRATOR").toString());
        
        // Unknown patterns
        testCases.put("invalid", roleDeterminationService.determineRole("invalid").toString());
        testCases.put("", roleDeterminationService.determineRole("").toString());
        testCases.put(null, roleDeterminationService.determineRole(null).toString());
        
        response.put("success", true);
        response.put("testCases", testCases);
        response.put("patterns", Map.of(
            "student1", "YY-####-### (e.g., 22-1234-567)",
            "student2", "YYYY-##### (e.g., 2010-12345)",
            "teacher", "1-#### (e.g., 1-1234)",
            "admin", "Contains 'ADMIN' or 'ADMINISTRATOR'"
        ));
        
        return ResponseEntity.ok(response);
    }

    private String determinePattern(String institutionalId) {
        if (institutionalId == null || institutionalId.isEmpty()) {
            return "NONE";
        }
        if (institutionalId.matches("\\d{2}-\\d{4}-\\d{3}")) {
            return "STUDENT_PATTERN_1 (YY-####-###)";
        }
        if (institutionalId.matches("\\d{4}-\\d{5}")) {
            return "STUDENT_PATTERN_2 (YYYY-#####)";
        }
        if (institutionalId.matches("1-\\d{4}")) {
            return "TEACHER_PATTERN (1-####)";
        }
        if (institutionalId.matches(".*ADMIN.*|.*ADMINISTRATOR.*")) {
            return "ADMIN_PATTERN";
        }
        return "UNKNOWN";
    }

    private Map<String, Object> mapUserToResponse(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("email", user.getEmail());
        userMap.put("microsoftId", user.getMicrosoftId());
        userMap.put("displayName", user.getDisplayName());
        userMap.put("jobTitle", user.getJobTitle());
        userMap.put("role", user.getRole().toString());
        userMap.put("emailVerified", user.getEmailVerified());
        userMap.put("isActive", user.getIsActive());
        userMap.put("accountCreatedAt", user.getAccountCreatedAt());
        return userMap;
    }
}

