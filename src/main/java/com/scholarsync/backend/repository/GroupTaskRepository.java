package com.scholarsync.backend.repository;

import com.scholarsync.backend.model.GroupTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupTaskRepository extends JpaRepository<GroupTask, Integer> {
    List<GroupTask> findByGroupIdOrderByGctaskEndAsc(String groupId);
    List<GroupTask> findByGroupId(String groupId);
    List<GroupTask> findByGctaskOwner(String ownerId);
}
