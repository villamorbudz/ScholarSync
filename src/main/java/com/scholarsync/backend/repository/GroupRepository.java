package com.scholarsync.backend.repository;

import com.scholarsync.backend.model.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, String> {
    List<GroupEntity> findByCourseId(Long courseId);
}
