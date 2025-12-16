package com.scholarsync.backend.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupDto {
    private String groupId;
    private String groupName;
    private Long courseId;
    private String leaderStudentId;
    private List<String> memberStudentIds;
    private String adviserId;
    private Instant createdAt;
}
