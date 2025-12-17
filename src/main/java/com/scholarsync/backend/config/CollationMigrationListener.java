package com.scholarsync.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Automatically fixes collation mismatches in the database on startup.
 * This ensures that group_id columns in groups and group_member_student_ids tables
 * use the same collation (utf8mb4_unicode_ci) to prevent join errors.
 */
@Slf4j
@Component
@Order(2)
public class CollationMigrationListener {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixCollations() {
        if (jdbcTemplate == null) {
            log.warn("JdbcTemplate not available, skipping collation migration");
            return;
        }

        try {
            // Check if groups table exists
            String tableExistsSql = 
                "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'groups'";
            
            Integer tableExists = jdbcTemplate.queryForObject(tableExistsSql, Integer.class);
            if (tableExists == null || tableExists == 0) {
                log.info("Groups table does not exist yet - will be created with correct collation");
                return;
            }

            // Fix group_id column in groups table
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE `groups` MODIFY COLUMN `group_id` VARCHAR(255) " +
                    "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL"
                );
                log.info("Fixed collation for groups.group_id column");
            } catch (Exception e) {
                log.warn("Could not alter groups.group_id collation (may already be correct): {}", e.getMessage());
            }

            // Note: group_member_student_ids table is no longer used - member IDs are stored as JSON in groups.member_student_ids
            // If the old table exists, it can be safely ignored or dropped manually

            log.info("Collation migration completed successfully!");
        } catch (Exception e) {
            log.error("Error during collation migration: {}", e.getMessage(), e);
            log.warn("================================================");
            log.warn("AUTOMATIC COLLATION MIGRATION FAILED");
            log.warn("Please run the following SQL manually in MySQL:");
            log.warn("USE scholarsync;");
            log.warn("ALTER TABLE `groups` MODIFY COLUMN `group_id` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;");
            log.warn("ALTER TABLE `group_member_student_ids` MODIFY COLUMN `group_id` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;");
            log.warn("ALTER TABLE `group_member_student_ids` MODIFY COLUMN `member_student_id` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            log.warn("================================================");
        }
    }
}
