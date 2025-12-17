package com.scholarsync.backend.controller;

import com.scholarsync.backend.entity.GroupAdviser;
import com.scholarsync.backend.service.GroupAdviserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group-adviser")
public class GroupAdviserController {

    private final GroupAdviserService groupAdviserService;

    public GroupAdviserController(GroupAdviserService groupAdviserService) {
        this.groupAdviserService = groupAdviserService;
    }

    // CREATE
    @PostMapping("/assign/{groupId}")
    public ResponseEntity<GroupAdviser> assignAdviser(
            @PathVariable Long groupId,
            @RequestParam Long adviserId
    ) {
        return ResponseEntity.ok(
                groupAdviserService.assignAdviser(groupId, adviserId)
        );
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<GroupAdviser>> getAllAssignments() {
        return ResponseEntity.ok(groupAdviserService.getAll());
    }

    // READ BY GROUP
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupAdviser> getByGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupAdviserService.getByGroupId(groupId));
    }

    // UPDATE
    @PutMapping("/reassign/{groupId}")
    public ResponseEntity<GroupAdviser> reassign(
            @PathVariable Long groupId,
            @RequestParam Long adviserId
    ) {
        return ResponseEntity.ok(
                groupAdviserService.reassignAdviser(groupId, adviserId)
        );
    }

    // DELETE
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> delete(@PathVariable Long groupId) {
        groupAdviserService.removeByGroupId(groupId);
        return ResponseEntity.noContent().build();
    }
}
