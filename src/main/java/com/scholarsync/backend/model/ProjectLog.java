package com.scholarsync.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;
    
    @Column(name = "group_id", nullable = false)
    private String groupId;
    
    @Column(name = "task_id")
    private Integer taskId; // Reference to GroupTask
    
    @Column(name = "user_id")
    private String userId; // User's institutional ID or UUID
    
    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType; // "ADD", "UPDATE", "DELETE"
    
    @Column(name = "action_description", nullable = false, columnDefinition = "TEXT")
    private String actionDescription; // e.g., "Member_3 added a new note on Functionality#4"
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
