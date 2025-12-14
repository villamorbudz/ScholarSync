package com.scholarsync.backend.config;

import com.scholarsync.backend.entity.Group;
import com.scholarsync.backend.entity.User;
import com.scholarsync.backend.repository.GroupRepository;
import com.scholarsync.backend.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TestDataLoader implements CommandLineRunner {

    private final UserRepository userRepo;
    private final GroupRepository groupRepo;

    public TestDataLoader(UserRepository userRepo, GroupRepository groupRepo) {
        this.userRepo = userRepo;
        this.groupRepo = groupRepo;
    }

    @Override
    public void run(String... args) {
        User adviser = new User();
        adviser.setEmail("adviser@test.com");
        adviser.setRole("TEACHER");
        userRepo.save(adviser);

        Group group = new Group();
        group.setName("Group Alpha");
        groupRepo.save(group);
    }
}
