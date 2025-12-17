package com.scholarsync.backend.service;

import com.scholarsync.backend.exception.ImportValidationException;
import com.scholarsync.backend.model.CourseEnrollment;
import com.scholarsync.backend.model.EnrollmentType;
import com.scholarsync.backend.model.GroupEntity;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.repository.CourseEnrollmentRepository;
import com.scholarsync.backend.repository.GroupRepository;
import com.scholarsync.backend.repository.UserRepository;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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

    private final UserRepository userRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final GroupRepository groupRepository;

    public GroupImportService(UserRepository userRepository, 
                              CourseEnrollmentRepository enrollmentRepository,
                              GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.groupRepository = groupRepository;
    }

    private static class RowRecord {
        String teamCode;
        int memberNo;
        String institutionalId; // Changed from studentId to institutionalId
    }

    @Transactional(rollbackFor = Exception.class)
    public List<GroupEntity> importFromExcel(MultipartFile file, Long courseId) {
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, List<RowRecord>> teams = new HashMap<>();
            Set<String> seenInstitutionalIds = new HashSet<>();
            List<String> errors = new ArrayList<>();

            // Detect header row
            boolean firstRowHeader = false;
            Row firstRow = sheet.getRow(sheet.getFirstRowNum());
            if (firstRow != null) {
                String v0 = firstRow.getCell(0) != null ? firstRow.getCell(0).toString().trim().toUpperCase() : "";
                if (v0.contains("TEAM")) {
                    firstRowHeader = true;
                }
            }

            // Parse Excel rows
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
                
                String institutionalId = row.getCell(2) != null ? row.getCell(2).toString().trim() : null;
                if (institutionalId == null || institutionalId.isEmpty()) {
                    errors.add(String.format("TEAM CODE=%s: empty STUDENT ID on row %d", teamCode, r + 1));
                    continue;
                }
                
                if (!seenInstitutionalIds.add(institutionalId)) {
                    errors.add(String.format("DUPLICATE STUDENT ID across groups: %s", institutionalId));
                }

                RowRecord rec = new RowRecord();
                rec.teamCode = teamCode;
                rec.memberNo = memberNo;
                rec.institutionalId = institutionalId;
                teams.computeIfAbsent(teamCode, k -> new ArrayList<>()).add(rec);
            }

            if (!errors.isEmpty()) throw new ImportValidationException(errors);

            // Extract all institutional IDs and lookup users
            Set<String> allInstitutionalIds = teams.values().stream()
                .flatMap(List::stream)
                .map(r -> r.institutionalId)
                .collect(Collectors.toSet());
            
            List<User> users = userRepository.findByInstitutionalIdIn(new ArrayList<>(allInstitutionalIds));
            Map<String, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getInstitutionalId, Function.identity()));
            
            // Validate all users exist
            Set<String> foundIds = userMap.keySet();
            for (String institutionalId : allInstitutionalIds) {
                if (!foundIds.contains(institutionalId)) {
                    errors.add(String.format("STUDENT ID=%s: user does not exist", institutionalId));
                }
            }
            
            // Validate users are students
            for (User user : users) {
                if (user.getRole() != com.scholarsync.backend.model.Role.STUDENT) {
                    errors.add(String.format("STUDENT ID=%s: user is not a student (role: %s)", 
                        user.getInstitutionalId(), user.getRole()));
                }
            }

            if (!errors.isEmpty()) throw new ImportValidationException(errors);

            // Get user IDs and validate enrollments
            List<UUID> userIds = users.stream().map(User::getId).collect(Collectors.toList());
            List<CourseEnrollment> enrollments = enrollmentRepository
                .findByUserIdInAndCourseId(userIds, courseId);
            
            Map<UUID, CourseEnrollment> enrollmentMap = enrollments.stream()
                .collect(Collectors.toMap(e -> e.getUser().getId(), Function.identity()));

            // Validate enrollments
            for (User user : users) {
                CourseEnrollment enrollment = enrollmentMap.get(user.getId());
                if (enrollment == null) {
                    errors.add(String.format("STUDENT ID=%s: not enrolled in course %d", 
                        user.getInstitutionalId(), courseId));
                } else if (enrollment.getEnrollmentType() != EnrollmentType.STUDENT) {
                    errors.add(String.format("STUDENT ID=%s: enrollment type is not STUDENT in course %d", 
                        user.getInstitutionalId(), courseId));
                } else if (enrollment.getGroupId() != null && !enrollment.getGroupId().isEmpty()) {
                    errors.add(String.format("STUDENT ID=%s: already assigned to group %s", 
                        user.getInstitutionalId(), enrollment.getGroupId()));
                }
            }

            // Per-team validations
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

            // Create groups and update enrollments
            List<GroupEntity> created = new ArrayList<>();
            Map<String, String> teamToGroupId = new HashMap<>();
            
            for (Map.Entry<String, List<RowRecord>> e : teams.entrySet()) {
                String team = e.getKey();
                List<RowRecord> rows = e.getValue();
                
                // Find leader (memberNo == 1)
                String leaderInstitutionalId = rows.stream()
                    .filter(r -> r.memberNo == 1)
                    .findFirst()
                    .get()
                    .institutionalId;
                
                User leader = userMap.get(leaderInstitutionalId);
                
                // Get all member User IDs
                List<UUID> memberUserIds = rows.stream()
                    .map(r -> userMap.get(r.institutionalId).getId())
                    .collect(Collectors.toList());
                
                // Create group
                String groupId = UUID.randomUUID().toString();
                GroupEntity group = GroupEntity.builder()
                    .groupId(groupId)
                    .groupName(team)
                    .courseId(courseId)
                    .leaderUserId(leader.getId())
                    .memberUserIds(memberUserIds)
                    .adviserUserId(null)
                    .createdAt(Instant.now())
                    .build();
                
                groupRepository.save(group);
                created.add(group);
                teamToGroupId.put(team, groupId);
            }

            // Update enrollments with groupId
            for (UUID userId : userIds) {
                CourseEnrollment enrollment = enrollmentMap.get(userId);
                if (enrollment != null) {
                    // Find which team this user belongs to
                    String team = teams.entrySet().stream()
                        .filter(entry -> entry.getValue().stream()
                            .anyMatch(r -> userMap.get(r.institutionalId).getId().equals(userId)))
                        .findFirst()
                        .map(Map.Entry::getKey)
                        .orElse(null);
                    
                    if (team != null) {
                        enrollment.setGroupId(teamToGroupId.get(team));
                    }
                }
            }
            
            enrollmentRepository.saveAll(enrollments);
            return created;

        } catch (ImportValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to import groups: " + ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public GroupEntity createManualGroup(String groupName, String leaderInstitutionalId, Long courseId, List<String> memberInstitutionalIds) {
        List<String> errors = new ArrayList<>();
        
        if (groupName == null || groupName.isEmpty()) {
            errors.add("GROUP NAME: cannot be empty");
        }
        if (leaderInstitutionalId == null || leaderInstitutionalId.isEmpty()) {
            errors.add("LEADER STUDENT ID: cannot be empty");
        }
        if (memberInstitutionalIds == null || memberInstitutionalIds.isEmpty()) {
            errors.add(String.format("GROUP NAME=%s: no members specified", groupName));
        }
        
        // Ensure leader is part of members
        if (!memberInstitutionalIds.contains(leaderInstitutionalId)) {
            memberInstitutionalIds = new ArrayList<>(memberInstitutionalIds);
            memberInstitutionalIds.add(0, leaderInstitutionalId);
        }

        // Check for duplicate student IDs
        Set<String> seen = new HashSet<>();
        for (String sid : memberInstitutionalIds) {
            if (!seen.add(sid)) {
                errors.add(String.format("GROUP NAME=%s: duplicate STUDENT ID %s", groupName, sid));
            }
        }

        if (!errors.isEmpty()) throw new ImportValidationException(errors);

        // Fetch users by institutional IDs
        List<User> users = userRepository.findByInstitutionalIdIn(memberInstitutionalIds);
        Map<String, User> userMap = users.stream()
            .collect(Collectors.toMap(User::getInstitutionalId, Function.identity()));
        
        Set<String> found = userMap.keySet();
        for (String sid : memberInstitutionalIds) {
            if (!found.contains(sid)) {
                errors.add(String.format("GROUP NAME=%s: STUDENT ID=%s does not exist", groupName, sid));
            }
        }
        
        // Validate users are students
        for (User user : users) {
            if (user.getRole() != com.scholarsync.backend.model.Role.STUDENT) {
                errors.add(String.format("GROUP NAME=%s: STUDENT ID=%s is not a student (role: %s)", 
                    groupName, user.getInstitutionalId(), user.getRole()));
            }
        }

        if (!errors.isEmpty()) throw new ImportValidationException(errors);

        // Get enrollments
        List<UUID> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        List<CourseEnrollment> enrollments = enrollmentRepository
            .findByUserIdInAndCourseId(userIds, courseId);
        
        Map<UUID, CourseEnrollment> enrollmentMap = enrollments.stream()
            .collect(Collectors.toMap(e -> e.getUser().getId(), Function.identity()));

        // Validate enrollments
        for (User user : users) {
            CourseEnrollment enrollment = enrollmentMap.get(user.getId());
            if (enrollment == null) {
                errors.add(String.format("GROUP NAME=%s: STUDENT ID=%s not enrolled in course %d", 
                    groupName, user.getInstitutionalId(), courseId));
            } else if (enrollment.getEnrollmentType() != EnrollmentType.STUDENT) {
                errors.add(String.format("GROUP NAME=%s: STUDENT ID=%s enrollment type is not STUDENT", 
                    groupName, user.getInstitutionalId()));
            } else if (enrollment.getGroupId() != null && !enrollment.getGroupId().isEmpty()) {
                errors.add(String.format("GROUP NAME=%s: STUDENT ID=%s already assigned to group %s", 
                    groupName, user.getInstitutionalId(), enrollment.getGroupId()));
            }
        }

        if (!errors.isEmpty()) throw new ImportValidationException(errors);

        // Create group
        String groupId = UUID.randomUUID().toString();
        User leader = userMap.get(leaderInstitutionalId);
        List<UUID> memberUserIds = users.stream().map(User::getId).collect(Collectors.toList());
        
        GroupEntity group = GroupEntity.builder()
            .groupId(groupId)
            .groupName(groupName)
            .courseId(courseId)
            .leaderUserId(leader.getId())
            .memberUserIds(memberUserIds)
            .adviserUserId(null)
            .createdAt(Instant.now())
            .build();
        
        groupRepository.save(group);

        // Update enrollments with groupId
        for (CourseEnrollment enrollment : enrollments) {
            enrollment.setGroupId(groupId);
        }
        enrollmentRepository.saveAll(enrollments);
        
        return group;
    }
}
