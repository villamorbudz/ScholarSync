package com.scholarsync.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_task")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gctask_id")
    private Integer gctaskId;
    
    @Column(name = "group_id", nullable = false)
    private String groupId;
    
    @Column(name = "gctask_title", nullable = false, length = 60)
    private String gctaskTitle;
    
    @Column(name = "gctask_desc", nullable = false, columnDefinition = "LONGTEXT")
    private String gctaskDesc;
    
    @Column(name = "gctask_progress", nullable = false)
    @Builder.Default
    private Integer gctaskProgress = 0;
    
    @Column(name = "gctask_start", nullable = false)
    private LocalDateTime gctaskStart;
    
    @Column(name = "gctask_end", nullable = false)
    private LocalDateTime gctaskEnd;
    
    @Column(name = "gctask_owner", nullable = false, columnDefinition = "VARCHAR(255)")
    private String gctaskOwner; // User's institutional ID (stored as string to match database)
}
