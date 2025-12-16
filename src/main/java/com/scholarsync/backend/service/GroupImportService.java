package com.scholarsync.backend.service;

import com.scholarsync.backend.exception.ImportValidationException;
import com.scholarsync.backend.model.GroupEntity;
import com.scholarsync.backend.model.Student;
import com.scholarsync.backend.repository.GroupRepository;
import com.scholarsync.backend.repository.StudentRepository;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;

    public GroupImportService(StudentRepository studentRepository, GroupRepository groupRepository) {
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
    }

    private static class RowRecord {
        String teamCode;
        int memberNo;
        String studentId;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<GroupEntity> importFromExcel(MultipartFile file, Long courseId) {
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

            // Validate existence and enrollment
            Set<String> allStudentIds = teams.values().stream().flatMap(List::stream).map(r -> r.studentId).collect(Collectors.toSet());
            List<Student> studentsFound = studentRepository.findAllByStudentIdIn(new ArrayList<>(allStudentIds));
            Set<String> foundIds = studentsFound.stream().map(Student::getStudentId).collect(Collectors.toSet());
            for (String sid : allStudentIds) {
                if (!foundIds.contains(sid)) {
                    errors.add(String.format("STUDENT ID=%s: student does not exist", sid));
                }
            }
            // check enrollment and group membership
            for (Student s : studentsFound) {
                if (!s.getCourseId().equals(courseId)) {
                    errors.add(String.format("STUDENT ID=%s: not enrolled in course %d", s.getStudentId(), courseId));
                }
                if (s.getGroupId() != null && !s.getGroupId().isEmpty()) {
                    errors.add(String.format("STUDENT ID=%s: already assigned to group %s", s.getStudentId(), s.getGroupId()));
                }
            }

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

            // Create groups and update students
            List<GroupEntity> created = new ArrayList<>();
            Map<String, String> teamToGroupId = new HashMap<>();
            for (Map.Entry<String, List<RowRecord>> e : teams.entrySet()) {
                String team = e.getKey();
                List<RowRecord> rows = e.getValue();
                String leaderId = rows.stream().filter(r -> r.memberNo == 1).findFirst().get().studentId;
                String gid = UUID.randomUUID().toString();
                List<String> members = rows.stream().map(r -> r.studentId).collect(Collectors.toList());
                GroupEntity g = new GroupEntity(gid, team, courseId, leaderId, members, null, Instant.now());
                groupRepository.save(g);
                created.add(g);
                teamToGroupId.put(team, gid);
            }

            // update students
            List<Student> toUpdate = studentsFound.stream().filter(s -> allStudentIds.contains(s.getStudentId())).collect(Collectors.toList());
            for (Student s : toUpdate) {
                // find which team
                String team = teams.entrySet().stream().filter(en -> en.getValue().stream().anyMatch(r -> r.studentId.equals(s.getStudentId()))).findFirst().get().getKey();
                s.setGroupId(teamToGroupId.get(team));
            }
            studentRepository.saveAll(toUpdate);
            return created;

        } catch (ImportValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to import groups: " + ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public GroupEntity createManualGroup(String groupName, String leaderStudentId, Long courseId, List<String> memberStudentIds) {
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

        // fetch students
        List<Student> students = studentRepository.findAllByStudentIdIn(memberStudentIds);
        Set<String> found = students.stream().map(Student::getStudentId).collect(Collectors.toSet());
        for (String sid : memberStudentIds) {
            if (!found.contains(sid)) {
                errors.add(String.format("GROUP NAME=%s: STUDENT ID=%s does not exist", groupName, sid));
            }
        }
        for (Student s : students) {
            if (!s.getCourseId().equals(courseId)) {
                errors.add(String.format("GROUP NAME=%s: STUDENT ID=%s not enrolled in course %d", groupName, s.getStudentId(), courseId));
            }
            if (s.getGroupId() != null && !s.getGroupId().isEmpty()) {
                errors.add(String.format("GROUP NAME=%s: STUDENT ID=%s already assigned to group %s", groupName, s.getStudentId(), s.getGroupId()));
            }
        }

        if (!errors.isEmpty()) throw new ImportValidationException(errors);

        String gid = UUID.randomUUID().toString();
        GroupEntity g = new GroupEntity(gid, groupName, courseId, leaderStudentId, new ArrayList<>(memberStudentIds), null, Instant.now());
        groupRepository.save(g);

        // update students
        for (Student s : students) {
            s.setGroupId(gid);
        }
        studentRepository.saveAll(students);
        return g;
    }
}
