package com.scholarsync.backend.service;

import com.scholarsync.backend.model.Course;
import com.scholarsync.backend.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {
    
    @Autowired
    private CourseRepository courseRepository;
    
    // C - Create
    public Course createCourse(Course course) {
        // ASSIGN THE REQUIRED professor_id before saving
        // This simulates the professor being logged in (using ID 1 as placeholder)
        if (course.getProfessorId() == null) {
            course.setProfessorId(1L); 
        }
        return courseRepository.save(course);
    }
    

    // R - Read All
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    // R - Read One
    public Optional<Course> getCourseById(Long id) {
        return courseRepository.findById(id);
    }

    // U - Update
    public Course updateCourse(Long id, Course courseDetails) {
        return courseRepository.findById(id)
            .map(existingCourse -> {
                existingCourse.setCourseName(courseDetails.getCourseName());
                existingCourse.setCourseCode(courseDetails.getCourseCode());
                existingCourse.setCourseDetails(courseDetails.getCourseDetails());
                existingCourse.setStartDate(courseDetails.getStartDate());
                existingCourse.setEndDate(courseDetails.getEndDate());
                existingCourse.setAdviserName(courseDetails.getAdviserName());
                
                return courseRepository.save(existingCourse);
            }).orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
    }

    // D - Delete
    public boolean deleteCourse(Long id) {
        if (courseRepository.existsById(id)) {
            courseRepository.deleteById(id);
            return true;
        }
        return false;
    }
}