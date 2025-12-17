package com.scholarsync.backend.controller;

import com.scholarsync.backend.repository.StudentRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class CourseController {

    private final StudentRepository studentRepository;

    public CourseController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @GetMapping("/api/courses")
    public ResponseEntity<?> listCourses(@RequestParam(required = false) String q) {
        List<Long> ids = studentRepository.findDistinctCourseIds();
        List<com.scholarsync.backend.dto.CourseDto> out = ids.stream()
            .map(id -> new com.scholarsync.backend.dto.CourseDto(id, "Course " + id))
            .filter(c -> {
                if (q == null || q.isBlank()) return true;
                String lq = q.toLowerCase();
                return c.label().toLowerCase().contains(lq) || c.courseId().toString().contains(lq);
            })
            .sorted((a,b) -> Long.compare(a.courseId(), b.courseId()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }
}
