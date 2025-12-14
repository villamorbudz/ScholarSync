package com.scholarsync.backend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupCreateRequest {
    private String groupName;
    private String leaderStudentId;
    private Long courseId;
    private List<String> memberStudentIds;
}
