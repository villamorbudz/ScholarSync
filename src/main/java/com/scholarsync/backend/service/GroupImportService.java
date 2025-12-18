package com.scholarsync.backend.service;

import com.scholarsync.backend.exception.ImportValidationException;
import com.scholarsync.backend.model.GroupEntity;
import com.scholarsync.backend.model.Student;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarsync.backend.repository.GroupRepository;
import com.scholarsync.backend.repository.StudentRepository;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class GroupImportService {

    private final GroupRepository groupRepository;
    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper;

    public GroupImportService(GroupRepository groupRepository, StudentRepository studentRepository, ObjectMapper objectMapper) {
        this.groupRepository = groupRepository;
        this.studentRepository = studentRepository;
        this.objectMapper = objectMapper;
    }
    
    private String listToJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize member IDs to JSON", e);
        }
    }
    
    private List<String> jsonToList(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize member IDs from JSON", e);
        }
    }

    private static class RowRecord {
        String teamCode;
        int memberNo;
        String studentId;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<GroupEntity> importFromExcel(MultipartFile file, Long courseId, String createdBy) {
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, List<RowRecord>> teams = new HashMap<>();
            Set<String> seenStudents = new HashSet<>();
            List<String> errors = new ArrayList<>();

            boolean firstRowHeader = false;
            Row firstRow = sheet.getRow(sheet.getFirstRowNum());
            if (firstRow != null) {
                String v0 = firstRow.getCell(0) != null ? firstRow.getCell(0).toString().trim().toUpperCase() : "";
                if (v0.contains("TEAM")) {
                    firstRowHeader = true;
                }
            }

            int start = firstRowHeader ? sheet.getFirstRowNum() + 1 : sheet.getFirstRowNum();
            for (int r = start; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String teamCode = row.getCell(0) != null ? row.getCell(0).toString().trim() : null;
                if (teamCode == null || teamCode.isEmpty()) continue;
                String memberNoStr = row.getCell(1) != null ? row.getCell(1).toString().trim() : "";
                int memberNo;
                try {
                    memberNo = (int) Double.parseDouble(memberNoStr);
                } catch (Exception ex) {
                    errors.add(String.format("TEAM CODE=%s: invalid MEMBER # '%s'", teamCode, memberNoStr));
                    continue;
                }
                String studentId = row.getCell(2) != null ? row.getCell(2).toString().trim() : null;
                if (studentId == null || studentId.isEmpty()) {
                    errors.add(String.format("TEAM CODE=%s: empty STUDENT ID on row %d", teamCode, r + 1));
                    continue;
                }
                if (!seenStudents.add(studentId)) {
                    errors.add(String.format("DUPLICATE STUDENT ID across groups: %s", studentId));
                }

                RowRecord rec = new RowRecord();
                rec.teamCode = teamCode;
                rec.memberNo = memberNo;
                rec.studentId = studentId;
                teams.computeIfAbsent(teamCode, k -> new ArrayList<>()).add(rec);
            }

            if (!errors.isEmpty()) throw new ImportValidationException(errors);

            // per-team validations
            for (Map.Entry<String, List<RowRecord>> e : teams.entrySet()) {
                String team = e.getKey();
                List<RowRecord> rows = e.getValue();
                long leaders = rows.stream().filter(r -> r.memberNo == 1).count();
                if (leaders == 0) {
                    errors.add(String.format("TEAM CODE=%s: no MEMBER # = 1", team));
                } else if (leaders > 1) {
                    errors.add(String.format("TEAM CODE=%s: multiple MEMBER # = 1", team));
                }
            }

            if (!errors.isEmpty()) throw new ImportValidationException(errors);

            // Create groups - only using groups table
            List<GroupEntity> created = new ArrayList<>();
            for (Map.Entry<String, List<RowRecord>> e : teams.entrySet()) {
                String team = e.getKey();
                List<RowRecord> rows = e.getValue();
                String leaderId = rows.stream().filter(r -> r.memberNo == 1).findFirst().get().studentId;
                String gid = UUID.randomUUID().toString();
                List<String> members = rows.stream().map(r -> r.studentId).collect(Collectors.toList());
                String membersJson = listToJson(members);
                GroupEntity g = GroupEntity.builder()
                    .groupId(gid)
                    .groupName(team)
                    .courseId(courseId)
                    .leaderStudentId(leaderId)
                    .memberStudentIds(membersJson)
                    .adviserId(null)
                    .createdBy(createdBy)
                    .createdAt(Instant.now())
                    .build();
                groupRepository.save(g);
                created.add(g);
            }
            return created;

        } catch (ImportValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to import groups: " + ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public GroupEntity createManualGroup(String groupName, String leaderStudentId, Long courseId, List<String> memberStudentIds, String createdBy) {
        List<String> errors = new ArrayList<>();
        if (groupName == null || groupName.isEmpty()) {
            errors.add("GROUP NAME: cannot be empty");
        }
        if (leaderStudentId == null || leaderStudentId.isEmpty()) {
            errors.add("LEADER STUDENT ID: cannot be empty");
        }
        if (memberStudentIds == null || memberStudentIds.isEmpty()) {
            errors.add(String.format("GROUP NAME=%s: no members specified", groupName));
        }
        // Ensure leader is part of members
        if (!memberStudentIds.contains(leaderStudentId)) {
            memberStudentIds = new ArrayList<>(memberStudentIds);
            memberStudentIds.add(0, leaderStudentId);
        }

        // duplicate student IDs
        Set<String> seen = new HashSet<>();
        for (String sid : memberStudentIds) {
            if (!seen.add(sid)) {
                errors.add(String.format("GROUP NAME=%s: duplicate STUDENT ID %s", groupName, sid));
            }
        }

        if (!errors.isEmpty()) throw new ImportValidationException(errors);

        // Check if any student is already in a group for this course
        List<GroupEntity> existingGroups = groupRepository.findByCourseId(courseId);
        for (GroupEntity existingGroup : existingGroups) {
            List<String> existingMembers = jsonToList(existingGroup.getMemberStudentIds());
            for (String memberId : memberStudentIds) {
                if (existingMembers.contains(memberId)) {
                    errors.add(String.format("GROUP NAME=%s: STUDENT ID=%s already assigned to group %s in course %d", 
                            groupName, memberId, existingGroup.getGroupName(), courseId));
                }
            }
        }

        if (!errors.isEmpty()) throw new ImportValidationException(errors);

        // Create group
        String gid = UUID.randomUUID().toString();
        String membersJson = listToJson(memberStudentIds);
        GroupEntity g = GroupEntity.builder()
            .groupId(gid)
            .groupName(groupName)
            .courseId(courseId)
            .leaderStudentId(leaderStudentId)
            .memberStudentIds(membersJson)
            .adviserId(null)
            .createdBy(createdBy)
            .createdAt(Instant.now())
            .build();
        groupRepository.save(g);
        
        // Update student records to set group_id for all members
        for (String studentId : memberStudentIds) {
            Optional<Student> studentOpt = studentRepository.findByStudentIdAndCourseId(studentId, courseId);
            if (studentOpt.isPresent()) {
                Student student = studentOpt.get();
                student.setGroupId(gid);
                studentRepository.save(student);
            }
        }
        
        return g;
    }

    @Transactional(rollbackFor = Exception.class)
    public GroupEntity updateGroup(String groupId, String groupName, String leaderStudentId, List<String> memberStudentIds) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        List<String> errors = new ArrayList<>();
        
        // Validate inputs
        if (groupName != null && groupName.isEmpty()) {
            errors.add("GROUP NAME: cannot be empty");
        }
        if (leaderStudentId != null && leaderStudentId.isEmpty()) {
            errors.add("LEADER STUDENT ID: cannot be empty");
        }
        if (memberStudentIds != null && memberStudentIds.isEmpty()) {
            errors.add("No members specified");
        }
        
        // Ensure leader is part of members if memberStudentIds is provided
        if (memberStudentIds != null) {
            if (!memberStudentIds.contains(leaderStudentId != null ? leaderStudentId : group.getLeaderStudentId())) {
                String finalLeaderId = leaderStudentId != null ? leaderStudentId : group.getLeaderStudentId();
                memberStudentIds = new ArrayList<>(memberStudentIds);
                memberStudentIds.add(0, finalLeaderId);
            }
            
            // Check for duplicate student IDs
            Set<String> seen = new HashSet<>();
            for (String sid : memberStudentIds) {
                if (!seen.add(sid)) {
                    errors.add(String.format("Duplicate STUDENT ID %s", sid));
                }
            }
            
            // Check if any student is already in another group for this course
            List<GroupEntity> existingGroups = groupRepository.findByCourseId(group.getCourseId());
            for (GroupEntity existingGroup : existingGroups) {
                if (existingGroup.getGroupId().equals(groupId)) continue; // Skip current group
                List<String> existingMembers = jsonToList(existingGroup.getMemberStudentIds());
                for (String memberId : memberStudentIds) {
                    if (existingMembers.contains(memberId)) {
                        errors.add(String.format("STUDENT ID=%s already assigned to group %s", 
                                memberId, existingGroup.getGroupName()));
                    }
                }
            }
        }
        
        if (!errors.isEmpty()) throw new ImportValidationException(errors);
        
        // Update group fields
        if (groupName != null) {
            group.setGroupName(groupName);
        }
        if (leaderStudentId != null) {
            group.setLeaderStudentId(leaderStudentId);
        }
        if (memberStudentIds != null) {
            // Get old members BEFORE updating the group
            List<String> oldMemberIds = jsonToList(group.getMemberStudentIds());
            Long courseId = group.getCourseId();
            
            // Update the group's member list
            String membersJson = listToJson(memberStudentIds);
            group.setMemberStudentIds(membersJson);
            
            // Remove group_id from students no longer in group
            for (String oldMemberId : oldMemberIds) {
                if (!memberStudentIds.contains(oldMemberId)) {
                    Optional<Student> studentOpt = studentRepository.findByStudentIdAndCourseId(oldMemberId, courseId);
                    if (studentOpt.isPresent()) {
                        Student student = studentOpt.get();
                        if (groupId.equals(student.getGroupId())) {
                            student.setGroupId(null);
                            studentRepository.save(student);
                        }
                    }
                }
            }
            
            // Update group_id for all new/remaining members
            for (String studentId : memberStudentIds) {
                Optional<Student> studentOpt = studentRepository.findByStudentIdAndCourseId(studentId, courseId);
                if (studentOpt.isPresent()) {
                    Student student = studentOpt.get();
                    student.setGroupId(groupId);
                    studentRepository.save(student);
                }
            }
        }
        
        groupRepository.save(group);
        return group;
    }
}
