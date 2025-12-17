package com.scholarsync.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Automatically migrates the students table to use a composite primary key
 * if it's still using a single-column primary key.
 */
@Slf4j
@Component
@Order(1)
public class DatabaseMigrationListener {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateStudentsTable() {
        if (jdbcTemplate == null) {
            log.warn("JdbcTemplate not available, skipping database migration");
            return;
        }

        try {
            // Check if students table exists
            String tableExistsSql = 
                "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'students'";
            
            Integer tableExists = jdbcTemplate.queryForObject(tableExistsSql, Integer.class);
            if (tableExists == null || tableExists == 0) {
                log.info("Students table does not exist yet - will be created with correct schema");
                return;
            }

            // Check current primary key structure - count columns in PRIMARY key
            String checkPrimaryKeySql = 
                "SELECT COUNT(*) " +
                "FROM information_schema.KEY_COLUMN_USAGE kcu " +
                "WHERE kcu.TABLE_SCHEMA = DATABASE() " +
                "AND kcu.TABLE_NAME = 'students' " +
                "AND kcu.CONSTRAINT_NAME = 'PRIMARY'";

            Integer pkColumnCount = jdbcTemplate.queryForObject(checkPrimaryKeySql, Integer.class);
            
            if (pkColumnCount != null && pkColumnCount == 1) {
                // Only one column in primary key - need to migrate
                log.info("Migrating students table: Changing primary key from single column to composite (student_id, course_id)");
                
                // Drop existing primary key
                jdbcTemplate.execute("ALTER TABLE `students` DROP PRIMARY KEY");
                log.info("Dropped existing primary key");
                
                // Add composite primary key
                jdbcTemplate.execute("ALTER TABLE `students` ADD PRIMARY KEY (`student_id`, `course_id`)");
                log.info("Added composite primary key (student_id, course_id)");
                
                log.info("Migration completed successfully! Students can now enroll in multiple courses.");
            } else if (pkColumnCount != null && pkColumnCount == 2) {
                log.info("Students table already has composite primary key - no migration needed");
            } else {
                log.warn("Students table has unexpected primary key structure ({} columns) - skipping migration", pkColumnCount);
            }
        } catch (Exception e) {
            log.error("Error during database migration: {}", e.getMessage(), e);
            log.warn("================================================");
            log.warn("AUTOMATIC MIGRATION FAILED");
            log.warn("Please run the following SQL manually in MySQL:");
            log.warn("USE scholarsync;");
            log.warn("ALTER TABLE `students` DROP PRIMARY KEY;");
            log.warn("ALTER TABLE `students` ADD PRIMARY KEY (`student_id`, `course_id`);");
            log.warn("================================================");
        }
    }
}
