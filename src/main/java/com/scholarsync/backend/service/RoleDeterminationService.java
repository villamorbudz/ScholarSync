package com.scholarsync.backend.service;

import com.scholarsync.backend.model.Role;
import org.springframework.stereotype.Service;

@Service
public class RoleDeterminationService {

    /**
     * Determines user role based on institutional ID patterns stored in jobTitle.
     *
     * Student Patterns:
     * - YY-####-### (e.g., 22-1234-567)
     * - YYYY-##### (e.g., 2010-12345)
     *
     * Teacher Pattern:
     * - Plain incremental number from 1 up to 4 digits (e.g., 1, 12, 133, 1643)
     *
     * @param jobTitle The jobTitle from Microsoft account (institutional ID)
     * @return Role enum (STUDENT, TEACHER, ADMIN, or UNKNOWN if no pattern matches)
     */
    public Role determineRole(String jobTitle) {
        if (jobTitle == null || jobTitle.isEmpty()) {
            return Role.UNKNOWN; // Placeholder - no fallback to prove parsing works
        }

        // Student Pattern 1: YY-####-### (e.g., 22-1234-567)
        // Regex: 2 digits, dash, 4 digits, dash, 3 digits
        if (jobTitle.matches("\\d{2}-\\d{4}-\\d{3}")) {
            return Role.STUDENT;
        }

        // Student Pattern 2: YYYY-##### (e.g., 2010-12345)
        // Regex: 4 digits, dash, 5 digits
        if (jobTitle.matches("\\d{4}-\\d{5}")) {
            return Role.STUDENT;
        }

        // Teacher Pattern: plain incremental number from 1 up to 4 digits
        // Regex: 1 to 4 digits, entire string (no dashes)
        if (jobTitle.matches("\\d{1,4}")) {
            return Role.TEACHER;
        }

        // Admin pattern (if needed in future)
        if (jobTitle.matches(".*ADMIN.*|.*ADMINISTRATOR.*")) {
            return Role.ADMIN;
        }

        // No fallback - return UNKNOWN to prove parsing is working correctly
        return Role.UNKNOWN;
    }
}


