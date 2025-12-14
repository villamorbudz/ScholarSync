package com.scholarsync.backend.controller;

import com.scholarsync.backend.service.GroupAdviserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin
public class GroupAdviserController {

    private final GroupAdviserService service;

    public GroupAdviserController(GroupAdviserService service) {
        this.service = service;
    }

    @PostMapping("/{groupId}/assign-adviser")
    public ResponseEntity<String> assign(
            @PathVariable Long groupId,
            @RequestParam Long adviserId) {

        service.assignAdviser(groupId, adviserId);
        return ResponseEntity.ok("Adviser assigned successfully");
    }

    @PostMapping("/{groupId}/claim")
    public ResponseEntity<String> claim(
            @PathVariable Long groupId,
            @RequestParam Long adviserId) {

        service.assignAdviser(groupId, adviserId);
        return ResponseEntity.ok("Group claimed successfully");
    }
}
