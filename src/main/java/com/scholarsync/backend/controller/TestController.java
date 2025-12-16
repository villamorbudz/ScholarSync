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

    /**
     * Test role determination with a given institutional ID
     *
     * POST /api/public/test/role
     * Body: { "institutionalId": "1643" }
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
        if (institutionalId.matches("\\d{1,4}")) {
            return "TEACHER_PATTERN (Plain number 1–4 digits)";
        }
        if (institutionalId.matches(".*ADMIN.*|.*ADMINISTRATOR.*")) {
            return "ADMIN_PATTERN";
        }
        return "UNKNOWN";
    }

}

