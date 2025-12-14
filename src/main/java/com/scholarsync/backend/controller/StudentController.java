package com.scholarsync.backend.controller;

import com.scholarsync.backend.dto.GroupDto;
import com.scholarsync.backend.dto.StudentDto;
import com.scholarsync.backend.model.GroupEntity;
import com.scholarsync.backend.model.Student;
import com.scholarsync.backend.repository.GroupRepository;
import com.scholarsync.backend.repository.StudentRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class StudentController {

    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;

    public StudentController(StudentRepository studentRepository, GroupRepository groupRepository) {
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/api/students/{studentId}")
    public ResponseEntity<?> getStudent(@PathVariable String studentId, @RequestParam Long courseId) {
        Optional<Student> sOpt = studentRepository.findById(studentId);
        if (sOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found");
        }
        Student s = sOpt.get();
        if (!s.getCourseId().equals(courseId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Student not enrolled in the given course");
        }
        StudentDto sd = new StudentDto(s.getStudentId(), s.getCourseId(), s.getGroupId(), s.getLastName(), s.getFirstName(), s.getEmail());
        if (s.getGroupId() == null || s.getGroupId().isEmpty()) {
            return ResponseEntity.ok(sd);
        }
        Optional<GroupEntity> gOpt = groupRepository.findById(s.getGroupId());
        if (gOpt.isPresent()) {
            GroupEntity g = gOpt.get();
            GroupDto gd = new GroupDto(g.getGroupId(), g.getGroupName(), g.getCourseId(), g.getLeaderStudentId(), g.getMemberStudentIds(), g.getAdviserId(), g.getCreatedAt());
            return ResponseEntity.ok(new Object() { public StudentDto student = sd; public GroupDto group = gd; });
        }
        return ResponseEntity.ok(sd);
    }

    @GetMapping("/api/students")
    public ResponseEntity<?> listStudents(@RequestParam Long courseId, @RequestParam(required = false) String q) {
        List<Student> list = studentRepository.findAllByCourseId(courseId);
        if (q != null && !q.isBlank()) {
            String lq = q.toLowerCase();
            list = list.stream().filter(s -> (s.getStudentId() != null && s.getStudentId().toLowerCase().contains(lq))
                || (s.getFirstName() != null && s.getFirstName().toLowerCase().contains(lq))
                || (s.getLastName() != null && s.getLastName().toLowerCase().contains(lq))
                || (s.getEmail() != null && s.getEmail().toLowerCase().contains(lq))
            ).collect(Collectors.toList());
        }
        List<StudentDto> dtos = list.stream().map(s -> new StudentDto(s.getStudentId(), s.getCourseId(), s.getGroupId(), s.getLastName(), s.getFirstName(), s.getEmail())).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
