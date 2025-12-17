package com.scholarsync.backend.service;

import com.scholarsync.backend.entity.Group;
import com.scholarsync.backend.entity.GroupAdviser;
import com.scholarsync.backend.entity.User;
import com.scholarsync.backend.repository.GroupAdviserRepository;
import com.scholarsync.backend.repository.GroupRepository;
import com.scholarsync.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupAdviserService {

    private final GroupAdviserRepository groupAdviserRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupAdviserService(
            GroupAdviserRepository groupAdviserRepository,
            GroupRepository groupRepository,
            UserRepository userRepository
    ) {
        this.groupAdviserRepository = groupAdviserRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    // CREATE
    public GroupAdviser assignAdviser(Long groupId, Long adviserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User adviser = userRepository.findById(adviserId)
                .orElseThrow(() -> new RuntimeException("Adviser not found"));

        if (groupAdviserRepository.existsByGroup(group)) {
            throw new RuntimeException("Group already has an adviser");
        }

        GroupAdviser assignment = new GroupAdviser();
        assignment.setGroup(group);
        assignment.setAdviser(adviser);
        assignment.setAssignedAt(LocalDateTime.now());

        return groupAdviserRepository.save(assignment);
    }

    // READ ALL
    public List<GroupAdviser> getAll() {
        return groupAdviserRepository.findAll();
    }

    // READ BY GROUP
    public GroupAdviser getByGroupId(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        return groupAdviserRepository.findByGroup(group)
                .orElseThrow(() -> new RuntimeException("No adviser assigned"));
    }

    // UPDATE
    public GroupAdviser reassignAdviser(Long groupId, Long adviserId) {
        GroupAdviser existing = getByGroupId(groupId);

        User adviser = userRepository.findById(adviserId)
                .orElseThrow(() -> new RuntimeException("Adviser not found"));

        existing.setAdviser(adviser);
        existing.setAssignedAt(LocalDateTime.now());

        return groupAdviserRepository.save(existing);
    }

    // DELETE
    public void removeByGroupId(Long groupId) {
        GroupAdviser existing = getByGroupId(groupId);
        groupAdviserRepository.delete(existing);
    }
}
