package com.scholarsync.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarsync.backend.model.GroupEntity;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.repository.GroupRepository;
import com.scholarsync.backend.repository.StudentRepository;
import com.scholarsync.backend.service.GroupImportService;
import com.scholarsync.backend.service.GroupTaskService;
import com.scholarsync.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class GroupController {
    
    private final GroupRepository groupRepository;
    private final UserService userService;
    private final GroupTaskService groupTaskService;
    private final GroupImportService groupImportService;
    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper;
    
    private List<String> jsonToList(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
    
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupDetails(
            @PathVariable String groupId,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        // Get group members (users) by their institutional IDs
        List<String> memberIds = jsonToList(group.getMemberStudentIds());
        List<Map<String, Object>> members = memberIds.stream()
                .map(institutionalId -> {
                    User user = userService.findByInstitutionalId(institutionalId).orElse(null);
                    Map<String, Object> memberInfo = new HashMap<>();
                    memberInfo.put("institutionalId", institutionalId);
                    if (user != null) {
                        memberInfo.put("displayName", user.getDisplayName());
                        memberInfo.put("email", user.getEmail());
                    } else {
                        memberInfo.put("displayName", "Unknown");
                        memberInfo.put("email", "");
                    }
                    return memberInfo;
                })
                .collect(Collectors.toList());
        
        // Get leader info
        User leader = userService.findByInstitutionalId(group.getLeaderStudentId()).orElse(null);
        Map<String, Object> leaderInfo = new HashMap<>();
        leaderInfo.put("institutionalId", group.getLeaderStudentId());
        if (leader != null) {
            leaderInfo.put("displayName", leader.getDisplayName());
            leaderInfo.put("email", leader.getEmail());
        } else {
            leaderInfo.put("displayName", "Unknown");
            leaderInfo.put("email", "");
        }
        
        // Get analytics
        Map<String, Object> analytics = groupTaskService.getGroupAnalytics(groupId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("groupId", group.getGroupId());
        response.put("groupName", group.getGroupName());
        response.put("courseId", group.getCourseId());
        response.put("leader", leaderInfo);
        response.put("members", members);
        response.put("adviserId", group.getAdviserId());
        response.put("analytics", analytics);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<GroupEntity>> getAllGroups() {
        List<GroupEntity> groups = groupRepository.findAll();
        return ResponseEntity.ok(groups);
    }
    
    @PutMapping("/{groupId}")
    public ResponseEntity<?> updateGroup(
            @PathVariable String groupId,
            @RequestBody com.scholarsync.backend.dto.GroupUpdateRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        if (oauth2User == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }
        
        String email = oauth2User.getAttribute("email");
        if (email == null) {
            email = oauth2User.getAttribute("userPrincipalName");
        }
        
        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Only teachers can edit groups
        if (currentUser.getRole() != com.scholarsync.backend.model.Role.TEACHER) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only teachers can edit groups"));
        }
        
        try {
            GroupEntity updatedGroup = groupImportService.updateGroup(
                groupId,
                request.getGroupName(),
                request.getLeaderStudentId(),
                request.getMemberStudentIds()
            );
            return ResponseEntity.ok(updatedGroup);
        } catch (com.scholarsync.backend.exception.ImportValidationException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .body(Map.of("errors", e.getErrors()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(
            @PathVariable String groupId,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        if (oauth2User == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }
        
        String email = oauth2User.getAttribute("email");
        if (email == null) {
            email = oauth2User.getAttribute("userPrincipalName");
        }
        
        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Only teachers can delete groups
        if (currentUser.getRole() != com.scholarsync.backend.model.Role.TEACHER) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only teachers can delete groups"));
        }
        
        try {
            GroupEntity group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));
            
            // Clear group_id from student records
            List<String> memberIds = jsonToList(group.getMemberStudentIds());
            Long courseId = group.getCourseId();
            
            for (String memberId : memberIds) {
                java.util.Optional<com.scholarsync.backend.model.Student> studentOpt = 
                    studentRepository.findByStudentIdAndCourseId(memberId, courseId);
                if (studentOpt.isPresent()) {
                    com.scholarsync.backend.model.Student student = studentOpt.get();
                    if (groupId.equals(student.getGroupId())) {
                        student.setGroupId(null);
                        studentRepository.save(student);
                    }
                }
            }
            
            // Delete the group
            groupRepository.delete(group);
            
            return ResponseEntity.ok(Map.of("message", "Group deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
