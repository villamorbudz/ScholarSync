package com.scholarsync.backend.service;

import com.scholarsync.backend.dto.CourseCreateRequest;
import com.scholarsync.backend.model.Course;
import com.scholarsync.backend.model.GroupEntity;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.repository.CourseRepository;
import com.scholarsync.backend.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {
    
    private final CourseRepository courseRepository;
    private final GroupRepository groupRepository;
    
    @Transactional
    public Course createCourse(CourseCreateRequest request, User creator) {
        log.info("Creating course: name={}, code={}, creator={}", 
                request.getCourseName(), request.getCourseCode(), creator.getEmail());
        
        // Validate required fields
        if (request.getCourseName() == null || request.getCourseName().trim().isEmpty()) {
            throw new IllegalArgumentException("Course name is required");
        }
        if (request.getCourseCode() == null || request.getCourseCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Course code is required");
        }
        if (request.getCourseDesc() == null || request.getCourseDesc().trim().isEmpty()) {
            throw new IllegalArgumentException("Course description is required");
        }
        if (request.getCourseDur() == null) {
            throw new IllegalArgumentException("Course duration is required");
        }
        
        // Automatically set the adviser to the creator's display name
        // If display name is too long (max 30 chars), truncate it
        String adviserName = creator.getDisplayName();
        if (adviserName == null || adviserName.isEmpty()) {
            adviserName = creator.getEmail();
        }
        if (adviserName == null || adviserName.isEmpty()) {
            adviserName = "Unknown";
        }
        if (adviserName.length() > 30) {
            adviserName = adviserName.substring(0, 30);
        }
        
        log.info("Setting course adviser to: {}", adviserName);
        
        // Generate a unique 6-character invitation code
        String invitationCode = generateUniqueInvitationCode();
        log.info("Generated invitation code: {}", invitationCode);
        
        Course course = Course.builder()
                .courseName(request.getCourseName().trim())
                .courseCode(request.getCourseCode().trim())
                .courseDesc(request.getCourseDesc().trim())
                .courseDur(request.getCourseDur())
                .courseAdviser(adviserName)
                .courseCap(request.getCourseCap() != null ? request.getCourseCap() : 0)
                .courseStat(request.getCourseStat() != null ? request.getCourseStat() : 1)
                .courseInv(invitationCode)
                .build();
        
        try {
            Course savedCourse = courseRepository.save(course);
            log.info("Course created successfully: id={}, name={}", savedCourse.getCourseId(), savedCourse.getCourseName());
            return savedCourse;
        } catch (Exception e) {
            log.error("Error saving course to database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save course: " + e.getMessage(), e);
        }
    }
    
    public List<Course> getAllCourses() {
        return courseRepository.findAllByOrderByCourseIdDesc();
    }
    
    public Course getCourseById(Integer courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }
    
    public List<GroupEntity> getGroupsByCourse(Long courseId) {
        return groupRepository.findByCourseId(courseId);
    }
    
    public Optional<Course> getCourseByInvitationCode(String courseInv) {
        return courseRepository.findByCourseInv(courseInv);
    }
    
    public List<Course> getCoursesByIds(List<Long> courseIds) {
        List<Integer> intIds = courseIds.stream()
                .map(Long::intValue)
                .collect(java.util.stream.Collectors.toList());
        return courseRepository.findAllById(intIds);
    }
    
    @Transactional
    public Course updateCourse(Integer courseId, CourseCreateRequest request, User updater) {
        log.info("Updating course: id={}, name={}, updater={}", 
                courseId, request.getCourseName(), updater.getEmail());
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        // Validate required fields
        if (request.getCourseName() == null || request.getCourseName().trim().isEmpty()) {
            throw new IllegalArgumentException("Course name is required");
        }
        if (request.getCourseCode() == null || request.getCourseCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Course code is required");
        }
        if (request.getCourseDesc() == null || request.getCourseDesc().trim().isEmpty()) {
            throw new IllegalArgumentException("Course description is required");
        }
        if (request.getCourseDur() == null) {
            throw new IllegalArgumentException("Course duration is required");
        }
        
        // Update course fields (preserve invitation code and adviser)
        course.setCourseName(request.getCourseName().trim());
        course.setCourseCode(request.getCourseCode().trim());
        course.setCourseDesc(request.getCourseDesc().trim());
        course.setCourseDur(request.getCourseDur());
        course.setCourseCap(request.getCourseCap() != null ? request.getCourseCap() : course.getCourseCap());
        course.setCourseStat(request.getCourseStat() != null ? request.getCourseStat() : course.getCourseStat());
        
        try {
            Course updatedCourse = courseRepository.save(course);
            log.info("Course updated successfully: id={}, name={}", updatedCourse.getCourseId(), updatedCourse.getCourseName());
            return updatedCourse;
        } catch (Exception e) {
            log.error("Error updating course in database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update course: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public void deleteCourse(Integer courseId) {
        log.info("Deleting course: id={}", courseId);
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        try {
            courseRepository.delete(course);
            log.info("Course deleted successfully: id={}", courseId);
        } catch (Exception e) {
            log.error("Error deleting course from database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete course: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generates a unique 6-character alphanumeric invitation code.
     * Uses uppercase letters and numbers (A-Z, 0-9).
     * Retries if code already exists in database.
     */
    private String generateUniqueInvitationCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String code;
        int maxAttempts = 100; // Prevent infinite loop
        int attempts = 0;
        
        do {
            StringBuilder codeBuilder = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                codeBuilder.append(characters.charAt(random.nextInt(characters.length())));
            }
            code = codeBuilder.toString();
            attempts++;
            
            // Check if code already exists
            if (courseRepository.existsByCourseInv(code)) {
                log.warn("Generated code {} already exists, retrying... (attempt {})", code, attempts);
                code = null; // Force retry
            }
        } while (code == null && attempts < maxAttempts);
        
        if (code == null) {
            throw new RuntimeException("Failed to generate unique invitation code after " + maxAttempts + " attempts");
        }
        
        return code;
    }
}
