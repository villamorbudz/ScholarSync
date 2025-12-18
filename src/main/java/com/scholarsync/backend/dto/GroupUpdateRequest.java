package com.scholarsync.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class GroupUpdateRequest {
    private String groupName;
    private String leaderStudentId;
    private List<String> memberStudentIds;
}
