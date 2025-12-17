package com.scholarsync.backend.service;

import com.scholarsync.backend.exception.ImportValidationException;
import com.scholarsync.backend.model.CourseEnrollment;
import com.scholarsync.backend.model.EnrollmentType;
import com.scholarsync.backend.model.GroupEntity;
import com.scholarsync.backend.model.Role;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.repository.CourseEnrollmentRepository;
import com.scholarsync.backend.repository.GroupRepository;
import com.scholarsync.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    /**
     * Get group by ID
     */
    public Optional<GroupEntity> getGroupById(String groupId) {
        return groupRepository.findById(groupId);
    }

    /**
     * Get all groups in a course
     */
    public List<GroupEntity> getGroupsByCourse(Long courseId) {
        return groupRepository.findByCourseId(courseId);
    }

    /**
     * Get user's group in a course
     */
    public Optional<GroupEntity> getUserGroupInCourse(UUID userId, Long courseId) {
        return groupRepository.findByCourseIdAndUserId(courseId, userId);
    }

    /**
     * Get groups where user is leader
     */
    public List<GroupEntity> getGroupsByLeader(UUID leaderUserId) {
        return groupRepository.findByLeaderUserId(leaderUserId);
    }

    /**
     * Get groups where user is member
     */
    public List<GroupEntity> getGroupsByMember(UUID memberUserId) {
        return groupRepository.findByMemberUserIdsContaining(memberUserId);
    }

    /**
     * Get groups where user is adviser
     */
    public List<GroupEntity> getGroupsByAdviser(UUID adviserUserId) {
        return groupRepository.findByAdviserUserId(adviserUserId);
    }

    /**
     * Update group (change name, leader, members, or adviser)
     */
    @Transactional
    public GroupEntity updateGroup(String groupId, String groupName, UUID leaderUserId, 
                                   List<UUID> memberUserIds, UUID adviserUserId, Long courseId) {
        GroupEntity group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        List<String> errors = new ArrayList<>();

        // Validate leader exists and is enrolled in course
        if (leaderUserId != null) {
            User leader = userRepository.findById(leaderUserId)
                .orElseThrow(() -> new RuntimeException("Leader user not found: " + leaderUserId));
            
            if (leader.getRole() != Role.STUDENT) {
                errors.add("Leader must be a student");
            }
            
            CourseEnrollment leaderEnrollment = enrollmentRepository
                .findByUserIdAndCourseId(leaderUserId, courseId)
                .orElseThrow(() -> new RuntimeException("Leader not enrolled in course"));
            
            if (leaderEnrollment.getEnrollmentType() != EnrollmentType.STUDENT) {
                errors.add("Leader enrollment type must be STUDENT");
            }
        }

        // Validate all members exist and are enrolled
        if (memberUserIds != null && !memberUserIds.isEmpty()) {
            for (UUID memberId : memberUserIds) {
                User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Member user not found: " + memberId));
                
                if (member.getRole() != Role.STUDENT) {
                    errors.add("All members must be students");
                }
                
                CourseEnrollment memberEnrollment = enrollmentRepository
                    .findByUserIdAndCourseId(memberId, courseId)
                    .orElse(null);
                
                if (memberEnrollment == null) {
                    errors.add("Member " + member.getInstitutionalId() + " not enrolled in course");
                } else if (memberEnrollment.getEnrollmentType() != EnrollmentType.STUDENT) {
                    errors.add("Member " + member.getInstitutionalId() + " enrollment type must be STUDENT");
                } else if (memberEnrollment.getGroupId() != null && 
                          !memberEnrollment.getGroupId().equals(groupId)) {
                    errors.add("Member " + member.getInstitutionalId() + " already assigned to another group");
                }
            }
        }

        // Validate adviser exists and is a teacher/adviser
        if (adviserUserId != null) {
            User adviser = userRepository.findById(adviserUserId)
                .orElseThrow(() -> new RuntimeException("Adviser user not found: " + adviserUserId));
            
            if (adviser.getRole() != Role.TEACHER && adviser.getRole() != Role.ADMIN) {
                errors.add("Adviser must be a teacher or admin");
            }
        }

        if (!errors.isEmpty()) {
            throw new ImportValidationException(errors);
        }

        // Update group fields
        if (groupName != null) {
            group.setGroupName(groupName);
        }
        if (leaderUserId != null) {
            group.setLeaderUserId(leaderUserId);
        }
        if (memberUserIds != null) {
            group.setMemberUserIds(memberUserIds);
        }
        if (adviserUserId != null) {
            group.setAdviserUserId(adviserUserId);
        }

        // Update enrollments
        if (memberUserIds != null) {
            // Remove groupId from old members
            List<CourseEnrollment> oldEnrollments = enrollmentRepository.findByGroupId(groupId);
            for (CourseEnrollment enrollment : oldEnrollments) {
                if (!memberUserIds.contains(enrollment.getUser().getId())) {
                    enrollment.setGroupId(null);
                }
            }
            
            // Set groupId for new members
            for (UUID memberId : memberUserIds) {
                CourseEnrollment enrollment = enrollmentRepository
                    .findByUserIdAndCourseId(memberId, courseId)
                    .orElse(null);
                if (enrollment != null) {
                    enrollment.setGroupId(groupId);
                }
            }
            
            enrollmentRepository.saveAll(oldEnrollments);
        }

        return groupRepository.save(group);
    }

    /**
     * Delete group and remove groupId from all enrollments
     */
    @Transactional
    public void deleteGroup(String groupId) {
        GroupEntity group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Remove groupId from all enrollments
        List<CourseEnrollment> enrollments = enrollmentRepository.findByGroupId(groupId);
        for (CourseEnrollment enrollment : enrollments) {
            enrollment.setGroupId(null);
        }
        enrollmentRepository.saveAll(enrollments);

        // Delete group
        groupRepository.delete(group);
    }

    /**
     * Assign adviser to group
     */
    @Transactional
    public GroupEntity assignAdviser(String groupId, UUID adviserUserId) {
        GroupEntity group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        if (adviserUserId == null) {
            group.setAdviserUserId(null);
            return groupRepository.save(group);
        }

        User adviser = userRepository.findById(adviserUserId)
            .orElseThrow(() -> new RuntimeException("Adviser user not found: " + adviserUserId));

        if (adviser.getRole() != Role.TEACHER && adviser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Adviser must be a teacher or admin");
        }

        group.setAdviserUserId(adviserUserId);
        return groupRepository.save(group);
    }
}

