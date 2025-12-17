package com.scholarsync.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Service that loads and manages the list of institutional IDs that should be assigned TEACHER role.
 * Reads from teacher-institutional-ids.properties file.
 */
@Slf4j
@Service
public class TeacherInstitutionalIdService {

    private Set<String> teacherInstitutionalIds = new HashSet<>();

    /**
     * Loads the teacher institutional IDs from the properties file on service initialization.
     */
    @PostConstruct
    public void loadTeacherIds() {
        try {
            ClassPathResource resource = new ClassPathResource("teacher-institutional-ids.properties");
            Properties properties = new Properties();
            
            try (InputStream inputStream = resource.getInputStream()) {
                properties.load(inputStream);
                
                // Extract all teacher IDs from properties file
                teacherInstitutionalIds.clear();
                for (String key : properties.stringPropertyNames()) {
                    if (key.startsWith("teacher.")) {
                        String institutionalId = properties.getProperty(key).trim();
                        if (!institutionalId.isEmpty()) {
                            teacherInstitutionalIds.add(institutionalId);
                            log.info("Loaded teacher institutional ID: {}", institutionalId);
                        }
                    }
                }
                
                log.info("Successfully loaded {} teacher institutional ID(s) from configuration file", 
                    teacherInstitutionalIds.size());
            }
        } catch (IOException e) {
            log.error("Failed to load teacher-institutional-ids.properties file: {}", e.getMessage(), e);
            log.warn("No teacher IDs loaded - all users will default to STUDENT role");
        }
    }

    /**
     * Checks if the given institutional ID is in the teacher list.
     * 
     * @param institutionalId The institutional ID to check
     * @return true if the ID is in the teacher list, false otherwise
     */
    public boolean isTeacherInstitutionalId(String institutionalId) {
        if (institutionalId == null || institutionalId.isEmpty()) {
            return false;
        }
        
        // Trim whitespace and check (case-sensitive)
        String trimmedId = institutionalId.trim();
        boolean isTeacher = teacherInstitutionalIds.contains(trimmedId);
        
        if (isTeacher) {
            log.info("Institutional ID '{}' matches teacher list - will assign TEACHER role", trimmedId);
        }
        
        return isTeacher;
    }

    /**
     * Gets all teacher institutional IDs (for debugging/admin purposes).
     * 
     * @return Set of all teacher institutional IDs
     */
    public Set<String> getAllTeacherIds() {
        return new HashSet<>(teacherInstitutionalIds);
    }
}
