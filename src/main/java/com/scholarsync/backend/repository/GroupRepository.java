package com.scholarsync.backend.repository;

import com.scholarsync.backend.model.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, String> {
    
    // Find all groups in a course
    List<GroupEntity> findByCourseId(Long courseId);
    
    // Find groups where user is the leader
    List<GroupEntity> findByLeaderUserId(UUID leaderUserId);
    
    // Find groups where user is a member (using custom query for ElementCollection)
    @Query("SELECT g FROM GroupEntity g WHERE :userId MEMBER OF g.memberUserIds")
    List<GroupEntity> findByMemberUserIdsContaining(@Param("userId") UUID userId);
    
    // Find groups where user is the adviser
    List<GroupEntity> findByAdviserUserId(UUID adviserUserId);
    
    // Find group by course and user (leader or member)
    @Query("SELECT g FROM GroupEntity g WHERE g.courseId = :courseId AND (g.leaderUserId = :userId OR :userId MEMBER OF g.memberUserIds)")
    Optional<GroupEntity> findByCourseIdAndUserId(@Param("courseId") Long courseId, @Param("userId") UUID userId);
    
    // Find groups in course where user is leader
    List<GroupEntity> findByCourseIdAndLeaderUserId(Long courseId, UUID leaderUserId);
    
    // Find groups in course where user is member
    @Query("SELECT g FROM GroupEntity g WHERE g.courseId = :courseId AND :userId MEMBER OF g.memberUserIds")
    List<GroupEntity> findByCourseIdAndMemberUserIdsContaining(@Param("courseId") Long courseId, @Param("userId") UUID userId);
}
