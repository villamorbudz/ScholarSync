package com.scholarsync.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupEntity {
    @Id
    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "leader_student_id", nullable = false)
    private String leaderStudentId;

    @ElementCollection
    @Column(name = "member_student_ids")
    private List<String> memberStudentIds;

    @Column(name = "adviser_id")
    private String adviserId;

    @Column(name = "created_at")
    private Instant createdAt;
}
