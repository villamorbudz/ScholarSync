package com.scholarsync.backend.controller;

import com.scholarsync.backend.exception.ImportValidationException;
import com.scholarsync.backend.model.GroupEntity;
import com.scholarsync.backend.service.GroupImportService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestBody;
import com.scholarsync.backend.dto.GroupCreateRequest;

@RestController
public class GroupImportController {

    private final GroupImportService importService;
    private final String professorKey;

    public GroupImportController(GroupImportService importService, @org.springframework.beans.factory.annotation.Value("${app.professor.key:}") String professorKey) {
        this.importService = importService;
        this.professorKey = professorKey;
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @PostMapping(path = "/api/groups/import", consumes = {"multipart/form-data"})
    public ResponseEntity<?> importGroups(@RequestParam("file") MultipartFile file, @RequestParam("courseId") Long courseId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Professor-Key", required = false) String key) {
        // If a professor key is configured, require it for import operations
        String configured = this.professorKey == null ? "" : this.professorKey;
        if (configured != null && !configured.isEmpty() && (key == null || !configured.equals(key))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden: missing or invalid professor key");
        }
        List<GroupEntity> created = importService.importFromExcel(file, courseId);
        return ResponseEntity.ok(created);
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @PostMapping(path = "/api/groups/manual", consumes = {"application/json"})
    public ResponseEntity<?> createManualGroup(@RequestBody GroupCreateRequest req) {
        GroupEntity created = importService.createManualGroup(req.getGroupName(), req.getLeaderStudentId(), req.getCourseId(), req.getMemberStudentIds());
        return ResponseEntity.ok(created);
    }

    @ExceptionHandler(ImportValidationException.class)
    public ResponseEntity<?> handleValidation(ImportValidationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("errors", ex.getErrors());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
