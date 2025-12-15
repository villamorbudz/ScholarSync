package com.scholarsync.backend.service;

import com.scholarsync.backend.model.Role;
import org.springframework.stereotype.Service;

@Service
public class RoleDeterminationService {

    /**
     * Determines user role based on jobTitle institutional ID patterns
     * 
     * Student Patterns:
     * - YY-####-### (e.g., 22-1234-567)
     * - YYYY-##### (e.g., 2010-12345)
     * 
     * Teacher Pattern:
     * - 1-#### (incremental starting from 1-0000, e.g., 1-0001, 1-1234)
     * 
     * @param jobTitle The jobTitle from Microsoft account
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

        // Teacher Pattern: 1-#### (incremental starting from 1-0000)
        // Regex: "1-", followed by 4 digits
        if (jobTitle.matches("1-\\d{4}")) {
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


