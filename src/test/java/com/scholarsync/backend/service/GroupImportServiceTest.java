package com.scholarsync.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scholarsync.backend.model.Student;
import com.scholarsync.backend.repository.GroupRepository;
import com.scholarsync.backend.repository.StudentRepository;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class GroupImportServiceTest {

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    GroupRepository groupRepository;

    @Autowired
    GroupImportService importService;

    @BeforeEach
    void setup() {
        studentRepository.deleteAll();
        groupRepository.deleteAll();
    }

    private MockMultipartFile buildExcel(String[][] rows) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("Sheet1");
            int r = 0;
            for (String[] row : rows) {
                Row ro = sh.createRow(r++);
                for (int c = 0; c < row.length; c++) {
                    ro.createCell(c).setCellValue(row[c]);
                }
            }
            wb.write(baos);
            return new MockMultipartFile("file", "groups.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", baos.toByteArray());
        }
    }

    @Test
    void importSuccess() throws Exception {
        Long courseId = 1L;
        Student s1 = new Student("S1", courseId, null, "L1", "F1", "a@b.c");
        Student s2 = new Student("S2", courseId, null, "L2", "F2", "b@b.c");
        Student s3 = new Student("S3", courseId, null, "L3", "F3", "c@b.c");
        studentRepository.saveAll(List.of(s1, s2, s3));

        String[][] rows = new String[][]{
            {"TEAM CODE","MEMBER #","STUDENT ID","LASTNAME","FIRSTNAME","EMAIL"},
            {"TEAM-A","1","S1","L1","F1","a@b.c"},
            {"TEAM-A","2","S2","L2","F2","b@b.c"},
            {"TEAM-B","1","S3","L3","F3","c@b.c"}
        };
        MockMultipartFile file = buildExcel(rows);
        var created = importService.importFromExcel(file, courseId);
        assertThat(created).hasSize(2);
        // verify students updated
        Student ss1 = studentRepository.findByStudentIdAndCourseId("S1", courseId).get();
        Student ss2 = studentRepository.findByStudentIdAndCourseId("S2", courseId).get();
        assertThat(ss1.getGroupId()).isNotNull();
        assertThat(ss1.getGroupId()).isEqualTo(ss2.getGroupId());
    }

    @Test
    void manualCreateSuccess() throws Exception {
        Long courseId = 2L;
        Student s1 = new Student("L1", courseId, null, "L1", "F1", "a@b.c");
        Student s2 = new Student("M1", courseId, null, "L2", "F2", "b@b.c");
        studentRepository.saveAll(List.of(s1, s2));

        var created = importService.createManualGroup("TEAM-M", "L1", courseId, List.of("L1", "M1"), "L1", true);
        assertThat(created).isNotNull();
        assertThat(created.getGroupName()).isEqualTo("TEAM-M");
        Student leader = studentRepository.findByStudentIdAndCourseId("L1", courseId).get();
        Student member = studentRepository.findByStudentIdAndCourseId("M1", courseId).get();
        assertThat(leader.getGroupId()).isEqualTo(created.getGroupId());
        assertThat(member.getGroupId()).isEqualTo(created.getGroupId());
    }

    @Test
    void manualCreateFailsIfMemberAlreadyAssigned() throws Exception {
        Long courseId = 3L;
        Student s1 = new Student("L2", courseId, null, "L1", "F1", "a@b.c");
        Student s2 = new Student("M2", courseId, "existing-group", "L2", "F2", "b@b.c");
        studentRepository.saveAll(List.of(s1, s2));

        var ex = org.assertj.core.api.Assertions.catchThrowable(() -> importService.createManualGroup("TEAM-N", "L2", courseId, List.of("L2", "M2"), "L2", false));
        assertThat(ex).isInstanceOf(com.scholarsync.backend.exception.ImportValidationException.class);
        var ive = (com.scholarsync.backend.exception.ImportValidationException) ex;
        assertThat(ive.getErrors()).anyMatch(s -> s.contains("already assigned"));
    }

    @Test
    void importFailsWhenNoLeader() throws Exception {
        Long courseId = 1L;
        Student s1 = new Student("S1", courseId, null, "L1", "F1", "a@b.c");
        Student s2 = new Student("S2", courseId, null, "L2", "F2", "b@b.c");
        studentRepository.saveAll(List.of(s1, s2));

        String[][] rows = new String[][]{
            {"TEAM CODE","MEMBER #","STUDENT ID","LASTNAME","FIRSTNAME","EMAIL"},
            {"TEAM-A","2","S1","L1","F1","a@b.c"},
            {"TEAM-A","2","S2","L2","F2","b@b.c"}
        };
        MockMultipartFile file = buildExcel(rows);
        var ex = org.assertj.core.api.Assertions.catchThrowable(() -> importService.importFromExcel(file, courseId));
        assertThat(ex).isInstanceOf(com.scholarsync.backend.exception.ImportValidationException.class);
        var ive = (com.scholarsync.backend.exception.ImportValidationException) ex;
        assertThat(ive.getErrors()).anyMatch(s -> s.contains("no MEMBER # = 1") && s.contains("TEAM CODE=TEAM-A"));
    }
}
