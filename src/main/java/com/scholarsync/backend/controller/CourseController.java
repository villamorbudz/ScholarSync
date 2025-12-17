package com.scholarsync.backend.controller;

import com.scholarsync.backend.model.Course;
import com.scholarsync.backend.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 1. Add @CrossOrigin annotation here
@CrossOrigin(origins = "http://localhost:5173") 
@RestController
@RequestMapping("/api/courses")
public class CourseController {
    
    @Autowired
    private CourseService courseService;
    
    // C - CREATE: POST /api/courses
    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        Course savedCourse = courseService.createCourse(course);
        return new ResponseEntity<>(savedCourse, HttpStatus.CREATED);
    }

    // R - READ ALL: GET /api/courses
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        List<Course> courses = courseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    // R - READ ONE: GET /api/courses/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // U - UPDATE: PUT /api/courses/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course courseDetails) {
        try {
            Course updatedCourse = courseService.updateCourse(id, courseDetails);
            return ResponseEntity.ok(updatedCourse);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // D - DELETE: DELETE /api/courses/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        if (courseService.deleteCourse(id)) {
            return ResponseEntity.noContent().build(); 
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}