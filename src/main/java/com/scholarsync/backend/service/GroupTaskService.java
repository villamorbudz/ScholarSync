package com.scholarsync.backend.service;

import com.scholarsync.backend.dto.TaskCreateRequest;
import com.scholarsync.backend.dto.TaskUpdateRequest;
import com.scholarsync.backend.model.GroupTask;
import com.scholarsync.backend.model.ProjectLog;
import com.scholarsync.backend.repository.GroupTaskRepository;
import com.scholarsync.backend.repository.ProjectLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupTaskService {
    
    private final GroupTaskRepository groupTaskRepository;
    private final ProjectLogRepository projectLogRepository;
    
    @Transactional
    public GroupTask createTask(TaskCreateRequest request, String userId, String userName) {
        GroupTask task = GroupTask.builder()
                .groupId(request.getGroupId())
                .gctaskTitle(request.getGctaskTitle())
                .gctaskDesc(request.getGctaskDesc())
                .gctaskProgress(0)
                .gctaskStart(request.getGctaskStart())
                .gctaskEnd(request.getGctaskEnd())
                .gctaskOwner(request.getGctaskOwner())
                .build();
        
        GroupTask saved = groupTaskRepository.save(task);
        
        // Create project log entry
        createLog(request.getGroupId(), saved.getGctaskId(), userId, userName, 
                "ADD", userName + " added a new task: " + request.getGctaskTitle());
        
        return saved;
    }
    
    @Transactional
    public GroupTask updateTask(Integer taskId, TaskUpdateRequest request, String userId, String userName) {
        GroupTask task = groupTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        String oldTitle = task.getGctaskTitle();
        task.setGctaskTitle(request.getGctaskTitle());
        task.setGctaskDesc(request.getGctaskDesc());
        task.setGctaskProgress(request.getGctaskProgress() != null ? request.getGctaskProgress() : task.getGctaskProgress());
        task.setGctaskStart(request.getGctaskStart());
        task.setGctaskEnd(request.getGctaskEnd());
        
        GroupTask saved = groupTaskRepository.save(task);
        
        // Create project log entry
        String actionDesc = userName + " updated the task: " + oldTitle;
        if (request.getGctaskProgress() != null && !request.getGctaskProgress().equals(task.getGctaskProgress())) {
            actionDesc = userName + " updated the progress on " + oldTitle + " to " + request.getGctaskProgress() + "%";
        }
        createLog(task.getGroupId(), taskId, userId, userName, "UPDATE", actionDesc);
        
        return saved;
    }
    
    @Transactional
    public void deleteTask(Integer taskId, String userId, String userName) {
        GroupTask task = groupTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        String taskTitle = task.getGctaskTitle();
        String groupId = task.getGroupId();
        
        // Create project log entry before deletion
        createLog(groupId, taskId, userId, userName, "DELETE", 
                userName + " deleted the task: " + taskTitle);
        
        groupTaskRepository.delete(task);
    }
    
    public List<GroupTask> getTasksByGroup(String groupId) {
        return groupTaskRepository.findByGroupIdOrderByGctaskEndAsc(groupId);
    }
    
    public GroupTask getTaskById(Integer taskId) {
        return groupTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }
    
    public Map<String, Object> getGroupAnalytics(String groupId) {
        List<GroupTask> tasks = getTasksByGroup(groupId);
        
        // Calculate member progress (tasks assigned to each member)
        Map<String, List<GroupTask>> memberTasks = new HashMap<>();
        Map<String, Integer> memberProgress = new HashMap<>();
        
        for (GroupTask task : tasks) {
            String owner = task.getGctaskOwner();
            memberTasks.putIfAbsent(owner, new java.util.ArrayList<>());
            memberTasks.get(owner).add(task);
            
            // Calculate average progress for this member
            int totalProgress = memberTasks.get(owner).stream()
                    .mapToInt(GroupTask::getGctaskProgress)
                    .sum();
            int avgProgress = memberTasks.get(owner).isEmpty() ? 0 : 
                    totalProgress / memberTasks.get(owner).size();
            memberProgress.put(owner, avgProgress);
        }
        
        // Task numbers analytics (completion percentages)
        List<Map<String, Object>> taskNumbers = new java.util.ArrayList<>();
        for (GroupTask task : tasks) {
            Map<String, Object> taskData = new HashMap<>();
            taskData.put("taskId", task.getGctaskId());
            taskData.put("title", task.getGctaskTitle());
            taskData.put("progress", task.getGctaskProgress());
            taskNumbers.add(taskData);
        }
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("memberProgress", memberProgress);
        analytics.put("taskNumbers", taskNumbers);
        
        return analytics;
    }
    
    private void createLog(String groupId, Integer taskId, String userId, String userName, 
                          String actionType, String description) {
        ProjectLog log = ProjectLog.builder()
                .groupId(groupId)
                .taskId(taskId)
                .userId(userId)
                .actionType(actionType)
                .actionDescription(description)
                .createdAt(LocalDateTime.now())
                .build();
        projectLogRepository.save(log);
    }
    
    public List<ProjectLog> getProjectLogs(String groupId) {
        return projectLogRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }
}
