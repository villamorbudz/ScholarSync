package com.scholarsync.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequest {
    private String gctaskTitle;
    private String gctaskDesc;
    private Integer gctaskProgress;
    private LocalDateTime gctaskStart;
    private LocalDateTime gctaskEnd;
}
