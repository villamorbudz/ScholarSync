package com.scholarsync.backend.controller;

import com.scholarsync.backend.exception.ImportValidationException;
import com.scholarsync.backend.model.GroupEntity;
import com.scholarsync.backend.model.Role;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.service.GroupImportService;
import com.scholarsync.backend.service.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestBody;
import com.scholarsync.backend.dto.GroupCreateRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class GroupImportController {

    private final GroupImportService importService;
    private final UserService userService;
    private final String professorKey;

    public GroupImportController(
            GroupImportService importService, 
            UserService userService,
            @Value("${app.professor.key:}") String professorKey) {
        this.importService = importService;
        this.userService = userService;
        this.professorKey = professorKey;
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @PostMapping(path = "/api/groups/import", consumes = {"multipart/form-data"})
    public ResponseEntity<?> importGroups(
            @RequestParam("file") MultipartFile file, 
            @RequestParam("courseId") Long courseId,
            @RequestHeader(value = "X-Professor-Key", required = false) String key) {
        // If a professor key is configured, require it for import operations
        String configured = this.professorKey == null ? "" : this.professorKey;
        if (configured != null && !configured.isEmpty() && (key == null || !configured.equals(key))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden: missing or invalid professor key");
        }
        // For Excel imports, createdBy is set to null (can be updated later if authentication is added)
        List<GroupEntity> created = importService.importFromExcel(file, courseId, null);
        return ResponseEntity.ok(created);
    }

    @CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
    @PostMapping(path = "/api/groups/manual", consumes = {"application/json"})
    public ResponseEntity<?> createManualGroup(
            @RequestBody GroupCreateRequest req,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        // Get current user from OAuth2
        if (oauth2User == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        
        String email = oauth2User.getAttribute("email");
        if (email == null) {
            email = oauth2User.getAttribute("userPrincipalName");
        }
        
        User currentUser = userService.findByEmail(email).orElse(null);
        
        if (currentUser == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "User not found in database");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        
        // Role-based leader validation
        if (currentUser.getRole() == Role.STUDENT) {
            // STUDENT: Creator must be the leader
            String creatorInstitutionalId = currentUser.getInstitutionalId();
            if (creatorInstitutionalId == null || creatorInstitutionalId.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Student creator must have an institutional ID");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Override leader to be the creator
            req.setLeaderStudentId(creatorInstitutionalId);
            log.info("Student {} creating group - auto-assigned as leader: {}", 
                currentUser.getEmail(), creatorInstitutionalId);
            
            // Ensure creator is in members list
            if (req.getMemberStudentIds() != null && 
                !req.getMemberStudentIds().contains(creatorInstitutionalId)) {
                req.getMemberStudentIds().add(0, creatorInstitutionalId);
            }
        } else if (currentUser.getRole() == Role.TEACHER) {
            // TEACHER: Can assign any member as leader (validated in service)
            log.info("Teacher {} creating group - assigned leader: {}", 
                currentUser.getEmail(), req.getLeaderStudentId());
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Only students and teachers can create groups");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        // Get creator's institutional ID
        String creatorInstitutionalId = currentUser.getInstitutionalId();
        
        GroupEntity created = importService.createManualGroup(
            req.getGroupName(), 
            req.getLeaderStudentId(), 
            req.getCourseId(), 
            req.getMemberStudentIds(),
            creatorInstitutionalId
        );
        return ResponseEntity.ok(created);
    }

    @ExceptionHandler(ImportValidationException.class)
    public ResponseEntity<?> handleValidation(ImportValidationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("errors", ex.getErrors());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
