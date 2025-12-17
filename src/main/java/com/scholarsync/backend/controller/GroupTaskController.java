package com.scholarsync.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarsync.backend.dto.TaskCreateRequest;
import com.scholarsync.backend.dto.TaskUpdateRequest;
import com.scholarsync.backend.model.GroupEntity;
import com.scholarsync.backend.model.GroupTask;
import com.scholarsync.backend.model.ProjectLog;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.repository.GroupRepository;
import com.scholarsync.backend.service.GroupTaskService;
import com.scholarsync.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups/{groupId}/tasks")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class GroupTaskController {
    
    private final GroupTaskService groupTaskService;
    private final UserService userService;
    private final GroupRepository groupRepository;
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
    
    @GetMapping
    public ResponseEntity<List<GroupTask>> getGroupTasks(@PathVariable String groupId) {
        List<GroupTask> tasks = groupTaskService.getTasksByGroup(groupId);
        return ResponseEntity.ok(tasks);
    }
    
    @PostMapping
    public ResponseEntity<?> createTask(
            @PathVariable String groupId,
            @RequestBody TaskCreateRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        if (oauth2User == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }
        
        // Verify user is a member of the group
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        String email = oauth2User.getAttribute("email");
        if (email == null) {
            email = oauth2User.getAttribute("userPrincipalName");
        }
        
        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String userInstitutionalId = currentUser.getInstitutionalId();
        List<String> memberIds = jsonToList(group.getMemberStudentIds());
        if (userInstitutionalId == null || 
            (!memberIds.contains(userInstitutionalId) && 
             !group.getLeaderStudentId().equals(userInstitutionalId))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not a member of this group"));
        }
        
        request.setGroupId(groupId);
        request.setGctaskOwner(userInstitutionalId); // Use institutional ID as owner identifier
        
        GroupTask task = groupTaskService.createTask(
                request, 
                userInstitutionalId, 
                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : currentUser.getEmail()
        );
        
        return ResponseEntity.ok(task);
    }
    
    @PutMapping("/{taskId}")
    public ResponseEntity<?> updateTask(
            @PathVariable String groupId,
            @PathVariable Integer taskId,
            @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        if (oauth2User == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }
        
        // Verify user is a member of the group
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        String email = oauth2User.getAttribute("email");
        if (email == null) {
            email = oauth2User.getAttribute("userPrincipalName");
        }
        
        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String userInstitutionalId = currentUser.getInstitutionalId();
        List<String> memberIds = jsonToList(group.getMemberStudentIds());
        if (userInstitutionalId == null || 
            (!memberIds.contains(userInstitutionalId) && 
             !group.getLeaderStudentId().equals(userInstitutionalId))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not a member of this group"));
        }
        
        GroupTask task = groupTaskService.updateTask(
                taskId,
                request,
                userInstitutionalId,
                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : currentUser.getEmail()
        );
        
        return ResponseEntity.ok(task);
    }
    
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(
            @PathVariable String groupId,
            @PathVariable Integer taskId,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        if (oauth2User == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }
        
        // Verify user is a member of the group
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        String email = oauth2User.getAttribute("email");
        if (email == null) {
            email = oauth2User.getAttribute("userPrincipalName");
        }
        
        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String userInstitutionalId = currentUser.getInstitutionalId();
        List<String> memberIds = jsonToList(group.getMemberStudentIds());
        if (userInstitutionalId == null || 
            (!memberIds.contains(userInstitutionalId) && 
             !group.getLeaderStudentId().equals(userInstitutionalId))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not a member of this group"));
        }
        
        groupTaskService.deleteTask(
                taskId,
                userInstitutionalId,
                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : currentUser.getEmail()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Task deleted successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/logs")
    public ResponseEntity<List<ProjectLog>> getProjectLogs(@PathVariable String groupId) {
        List<ProjectLog> logs = groupTaskService.getProjectLogs(groupId);
        return ResponseEntity.ok(logs);
    }
}
