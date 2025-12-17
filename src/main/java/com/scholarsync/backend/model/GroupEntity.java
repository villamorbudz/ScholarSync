package com.scholarsync.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    // Changed from leader_student_id to leader_user_id (UUID)
    @Column(name = "leader_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID leaderUserId;

    // Changed from member_student_ids to member_user_ids (List<UUID>)
    @ElementCollection
    @Column(name = "member_user_id", columnDefinition = "BINARY(16)")
    private List<UUID> memberUserIds;

    // Changed from adviser_id (String) to adviser_user_id (UUID)
    @Column(name = "adviser_user_id", columnDefinition = "BINARY(16)")
    private UUID adviserUserId;

    @Column(name = "created_at")
    private Instant createdAt;
}
