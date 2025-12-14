package com.scholarsync.backend.repository;

import com.scholarsync.backend.entity.GroupAdviser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupAdviserRepository extends JpaRepository<GroupAdviser, Long> {

    boolean existsByGroupId(Long groupId);
    long countByAdviserId(Long adviserId);
}
