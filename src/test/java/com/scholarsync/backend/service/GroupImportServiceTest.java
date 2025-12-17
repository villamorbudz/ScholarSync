package com.scholarsync.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.scholarsync.backend.model.CourseEnrollment;
import com.scholarsync.backend.model.EnrollmentType;
import com.scholarsync.backend.model.Role;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.repository.CourseEnrollmentRepository;
import com.scholarsync.backend.repository.GroupRepository;
import com.scholarsync.backend.repository.UserRepository;
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
    UserRepository userRepository;

    @Autowired
    CourseEnrollmentRepository enrollmentRepository;

    @Autowired
    GroupRepository groupRepository;

    @Autowired
    GroupImportService importService;

    @BeforeEach
    void setup() {
        enrollmentRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
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

    private User createUser(String institutionalId, String email, String displayName) {
        User user = User.builder()
            .institutionalId(institutionalId)
            .email(email)
            .displayName(displayName)
            .microsoftId("ms-" + institutionalId)
            .role(Role.STUDENT)
            .isActive(true)
            .build();
        return userRepository.save(user);
    }

    private CourseEnrollment createEnrollment(User user, Long courseId) {
        CourseEnrollment enrollment = CourseEnrollment.builder()
            .user(user)
            .courseId(courseId)
            .enrollmentType(EnrollmentType.STUDENT)
            .groupId(null)
            .isActive(true)
            .build();
        return enrollmentRepository.save(enrollment);
    }

    @Test
    void importSuccess() throws Exception {
        Long courseId = 1L;
        User u1 = createUser("S1", "a@b.c", "F1 L1");
        User u2 = createUser("S2", "b@b.c", "F2 L2");
        User u3 = createUser("S3", "c@b.c", "F3 L3");
        createEnrollment(u1, courseId);
        createEnrollment(u2, courseId);
        createEnrollment(u3, courseId);

        String[][] rows = new String[][]{
            {"TEAM CODE","MEMBER #","STUDENT ID","LASTNAME","FIRSTNAME","EMAIL"},
            {"TEAM-A","1","S1","L1","F1","a@b.c"},
            {"TEAM-A","2","S2","L2","F2","b@b.c"},
            {"TEAM-B","1","S3","L3","F3","c@b.c"}
        };
        MockMultipartFile file = buildExcel(rows);
        var created = importService.importFromExcel(file, courseId);
        assertThat(created).hasSize(2);
        
        // Verify enrollments updated with groupId
        CourseEnrollment e1 = enrollmentRepository.findByUserIdAndCourseId(u1.getId(), courseId).get();
        CourseEnrollment e2 = enrollmentRepository.findByUserIdAndCourseId(u2.getId(), courseId).get();
        assertThat(e1.getGroupId()).isNotNull();
        assertThat(e1.getGroupId()).isEqualTo(e2.getGroupId());
    }

    @Test
    void manualCreateSuccess() throws Exception {
        Long courseId = 2L;
        User u1 = createUser("L1", "a@b.c", "F1 L1");
        User u2 = createUser("M1", "b@b.c", "F2 L2");
        createEnrollment(u1, courseId);
        createEnrollment(u2, courseId);

        var created = importService.createManualGroup("TEAM-M", "L1", courseId, List.of("L1", "M1"));
        assertThat(created).isNotNull();
        assertThat(created.getGroupName()).isEqualTo("TEAM-M");
        
        CourseEnrollment leaderEnrollment = enrollmentRepository.findByUserIdAndCourseId(u1.getId(), courseId).get();
        CourseEnrollment memberEnrollment = enrollmentRepository.findByUserIdAndCourseId(u2.getId(), courseId).get();
        assertThat(leaderEnrollment.getGroupId()).isEqualTo(created.getGroupId());
        assertThat(memberEnrollment.getGroupId()).isEqualTo(created.getGroupId());
    }

    @Test
    void manualCreateFailsIfMemberAlreadyAssigned() throws Exception {
        Long courseId = 3L;
        User u1 = createUser("L2", "a@b.c", "F1 L1");
        User u2 = createUser("M2", "b@b.c", "F2 L2");
        createEnrollment(u1, courseId);
        CourseEnrollment e2 = createEnrollment(u2, courseId);
        e2.setGroupId("existing-group");
        enrollmentRepository.save(e2);

        var ex = org.assertj.core.api.Assertions.catchThrowable(() -> 
            importService.createManualGroup("TEAM-N", "L2", courseId, List.of("L2", "M2")));
        assertThat(ex).isInstanceOf(com.scholarsync.backend.exception.ImportValidationException.class);
        var ive = (com.scholarsync.backend.exception.ImportValidationException) ex;
        assertThat(ive.getErrors()).anyMatch(s -> s.contains("already assigned"));
    }

    @Test
    void importFailsWhenNoLeader() throws Exception {
        Long courseId = 1L;
        User u1 = createUser("S1", "a@b.c", "F1 L1");
        User u2 = createUser("S2", "b@b.c", "F2 L2");
        createEnrollment(u1, courseId);
        createEnrollment(u2, courseId);

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
