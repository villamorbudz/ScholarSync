package com.scholarsync.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migrates the groups table to use member_student_ids as a TEXT column
 * instead of the old group_member_student_ids table.
 */
@Slf4j
@Component
@Order(3)
public class GroupTableMigrationListener {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateGroupTable() {
        if (jdbcTemplate == null) {
            log.warn("JdbcTemplate not available, skipping group table migration");
            return;
        }

        try {
            // Check if groups table exists
            String tableExistsSql = 
                "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'groups'";
            
            Integer tableExists = jdbcTemplate.queryForObject(tableExistsSql, Integer.class);
            if (tableExists == null || tableExists == 0) {
                log.info("Groups table does not exist yet - will be created with correct schema");
                return;
            }

            // Check if member_student_ids column already exists
            String columnExistsSql = 
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = 'groups' " +
                "AND COLUMN_NAME = 'member_student_ids'";
            
            Integer columnExists = jdbcTemplate.queryForObject(columnExistsSql, Integer.class);
            
            if (columnExists == null || columnExists == 0) {
                // Column doesn't exist - add it
                log.info("Adding member_student_ids column to groups table");
                try {
                    jdbcTemplate.execute(
                        "ALTER TABLE `groups` ADD COLUMN `member_student_ids` TEXT"
                    );
                    log.info("Successfully added member_student_ids column");
                } catch (Exception e) {
                    log.error("Failed to add member_student_ids column: {}", e.getMessage(), e);
                }
            } else {
                log.info("member_student_ids column already exists in groups table");
            }

            log.info("Group table migration completed successfully!");
        } catch (Exception e) {
            log.error("Error during group table migration: {}", e.getMessage(), e);
        }
    }
}
