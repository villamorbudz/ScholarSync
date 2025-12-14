package com.scholarsync.backend.service;

import com.scholarsync.backend.entity.Group;
import com.scholarsync.backend.entity.GroupAdviser;
import com.scholarsync.backend.entity.User;
import com.scholarsync.backend.repository.GroupAdviserRepository;
import com.scholarsync.backend.repository.GroupRepository;
import com.scholarsync.backend.repository.UserRepository;

import org.springframework.stereotype.Service;

@Service
public class GroupAdviserService {

    private static final int MAX_GROUPS = 5;

    private final GroupAdviserRepository repo;
    private final GroupRepository groupRepo;
    private final UserRepository userRepo;

    public GroupAdviserService(GroupAdviserRepository repo,
                               GroupRepository groupRepo,
                               UserRepository userRepo) {
        this.repo = repo;
        this.groupRepo = groupRepo;
        this.userRepo = userRepo;
    }

    public void assignAdviser(Long groupId, Long adviserId) {

        if (repo.existsByGroupId(groupId)) {
            throw new RuntimeException("Group already has an adviser");
        }

        if (repo.countByAdviserId(adviserId) >= MAX_GROUPS) {
            throw new RuntimeException("Adviser has reached max group limit");
        }

        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User adviser = userRepo.findById(adviserId)
                .orElseThrow(() -> new RuntimeException("Adviser not found"));

        if (!"TEACHER".equals(adviser.getRole())) {
            throw new RuntimeException("User is not an adviser");
        }

        GroupAdviser ga = new GroupAdviser();
        ga.setGroup(group);
        ga.setAdviser(adviser);

        repo.save(ga);
    }
}
