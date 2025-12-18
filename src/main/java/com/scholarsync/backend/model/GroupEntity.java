package com.scholarsync.backend.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupEntity {
    @Id
    @Column(name = "group_id", nullable = false, length = 255)
    private String groupId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "leader_student_id", nullable = false)
    private String leaderStudentId;

    @Lob
    @Column(name = "member_student_ids", length = 65535)
    private String memberStudentIds; // JSON array stored as string: ["id1", "id2", ...]

    @Column(name = "adviser_id")
    private String adviserId;

    @Column(name = "created_by")
    private String createdBy; // Institutional ID of the user who created the group

    @Column(name = "allow_leader_edit")
    private Boolean allowLeaderEdit; // If true, student leader can edit the group (only applies when created by teacher)

    @Column(name = "created_at")
    private Instant createdAt;
}
