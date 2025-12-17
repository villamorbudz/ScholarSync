package com.scholarsync.backend.controller;

import com.scholarsync.backend.dto.GroupDto;
import com.scholarsync.backend.model.GroupEntity;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.repository.UserRepository;
import com.scholarsync.backend.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final UserRepository userRepository;

    /**
     * Get group by ID
     * GET /api/groups/{groupId}
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroup(@PathVariable String groupId) {
        Optional<GroupEntity> groupOpt = groupService.getGroupById(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }
        
        GroupEntity group = groupOpt.get();
        GroupDto dto = mapGroupToDto(group);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get all groups in a course
     * GET /api/groups?courseId=1
     */
    @GetMapping
    public ResponseEntity<?> getGroupsByCourse(@RequestParam Long courseId) {
        List<GroupEntity> groups = groupService.getGroupsByCourse(courseId);
        List<GroupDto> dtos = groups.stream()
            .map(this::mapGroupToDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get user's group in a course
     * GET /api/groups/user/{userId}?courseId=1
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserGroup(@PathVariable UUID userId, @RequestParam Long courseId) {
        Optional<GroupEntity> groupOpt = groupService.getUserGroupInCourse(userId, courseId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("User is not in any group for this course");
        }
        
        GroupEntity group = groupOpt.get();
        GroupDto dto = mapGroupToDto(group);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get user's group by institutional ID
     * GET /api/groups/user/institutional/{institutionalId}?courseId=1
     */
    @GetMapping("/user/institutional/{institutionalId}")
    public ResponseEntity<?> getUserGroupByInstitutionalId(
            @PathVariable String institutionalId, 
            @RequestParam Long courseId) {
        Optional<User> userOpt = userRepository.findByInstitutionalId(institutionalId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        
        UUID userId = userOpt.get().getId();
        Optional<GroupEntity> groupOpt = groupService.getUserGroupInCourse(userId, courseId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("User is not in any group for this course");
        }
        
        GroupEntity group = groupOpt.get();
        GroupDto dto = mapGroupToDto(group);
        return ResponseEntity.ok(dto);
    }

    /**
     * Update group
     * PUT /api/groups/{groupId}
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<?> updateGroup(
            @PathVariable String groupId,
            @RequestBody GroupUpdateRequest request) {
        try {
            // Convert institutional IDs to UUIDs if provided
            UUID leaderUserId = null;
            if (request.getLeaderStudentId() != null) {
                Optional<User> leader = userRepository.findByInstitutionalId(request.getLeaderStudentId());
                if (leader.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Leader user not found: " + request.getLeaderStudentId());
                }
                leaderUserId = leader.get().getId();
            }

            List<UUID> memberUserIds = null;
            if (request.getMemberStudentIds() != null && !request.getMemberStudentIds().isEmpty()) {
                List<User> members = userRepository.findByInstitutionalIdIn(request.getMemberStudentIds());
                if (members.size() != request.getMemberStudentIds().size()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Some member users not found");
                }
                memberUserIds = members.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            }

            UUID adviserUserId = null;
            if (request.getAdviserId() != null) {
                Optional<User> adviser = userRepository.findByInstitutionalId(request.getAdviserId());
                if (adviser.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Adviser user not found: " + request.getAdviserId());
                }
                adviserUserId = adviser.get().getId();
            }

            // Get courseId from existing group
            GroupEntity existingGroup = groupService.getGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
            Long courseId = existingGroup.getCourseId();

            GroupEntity updated = groupService.updateGroup(
                groupId,
                request.getGroupName(),
                leaderUserId,
                memberUserIds,
                adviserUserId,
                courseId
            );

            GroupDto dto = mapGroupToDto(updated);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error updating group: " + e.getMessage());
        }
    }

    /**
     * Delete group
     * DELETE /api/groups/{groupId}
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable String groupId) {
        try {
            groupService.deleteGroup(groupId);
            return ResponseEntity.ok().body("Group deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error deleting group: " + e.getMessage());
        }
    }

    /**
     * Assign adviser to group
     * POST /api/groups/{groupId}/adviser
     */
    @PostMapping("/{groupId}/adviser")
    public ResponseEntity<?> assignAdviser(
            @PathVariable String groupId,
            @RequestBody AdviserAssignmentRequest request) {
        try {
            UUID adviserUserId = null;
            if (request.getAdviserId() != null) {
                Optional<User> adviser = userRepository.findByInstitutionalId(request.getAdviserId());
                if (adviser.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Adviser user not found: " + request.getAdviserId());
                }
                adviserUserId = adviser.get().getId();
            }

            GroupEntity group = groupService.assignAdviser(groupId, adviserUserId);
            GroupDto dto = mapGroupToDto(group);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error assigning adviser: " + e.getMessage());
        }
    }

    private GroupDto mapGroupToDto(GroupEntity group) {
        // Convert UUIDs to institutional IDs for API response
        User leader = userRepository.findById(group.getLeaderUserId()).orElse(null);
        String leaderInstitutionalId = leader != null ? leader.getInstitutionalId() : null;
        
        List<String> memberInstitutionalIds = group.getMemberUserIds().stream()
            .map(userId -> {
                User user = userRepository.findById(userId).orElse(null);
                return user != null ? user.getInstitutionalId() : null;
            })
            .filter(id -> id != null)
            .collect(Collectors.toList());
        
        String adviserInstitutionalId = null;
        if (group.getAdviserUserId() != null) {
            User adviser = userRepository.findById(group.getAdviserUserId()).orElse(null);
            adviserInstitutionalId = adviser != null ? adviser.getInstitutionalId() : null;
        }
        
        return new GroupDto(
            group.getGroupId(),
            group.getGroupName(),
            group.getCourseId(),
            leaderInstitutionalId,
            memberInstitutionalIds,
            adviserInstitutionalId,
            group.getCreatedAt()
        );
    }

    // Request DTOs
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GroupUpdateRequest {
        private String groupName;
        private String leaderStudentId; // institutional ID
        private List<String> memberStudentIds; // institutional IDs
        private String adviserId; // institutional ID
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AdviserAssignmentRequest {
        private String adviserId; // institutional ID, null to remove
    }
}

