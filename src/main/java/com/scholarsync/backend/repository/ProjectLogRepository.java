package com.scholarsync.backend.repository;

import com.scholarsync.backend.model.ProjectLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectLogRepository extends JpaRepository<ProjectLog, Integer> {
    List<ProjectLog> findByGroupIdOrderByCreatedAtDesc(String groupId);
}
