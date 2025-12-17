package com.scholarsync.backend.repository;

import com.scholarsync.backend.entity.Group;
import com.scholarsync.backend.entity.GroupAdviser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupAdviserRepository extends JpaRepository<GroupAdviser, Long> {

    boolean existsByGroup(Group group);

    Optional<GroupAdviser> findByGroup(Group group);
}
