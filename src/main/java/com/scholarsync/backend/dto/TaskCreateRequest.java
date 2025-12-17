package com.scholarsync.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {
    private String groupId;
    private String gctaskTitle;
    private String gctaskDesc;
    private LocalDateTime gctaskStart;
    private LocalDateTime gctaskEnd;
    private String gctaskOwner; // User's institutional ID
}
