package com.scholarsync.backend.service;

import com.scholarsync.backend.model.CourseEnrollment;
import com.scholarsync.backend.model.EnrollmentType;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.repository.CourseEnrollmentRepository;
import com.scholarsync.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseEnrollmentService {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    /**
     * Enroll a user in a course
     */
    @Transactional
    public CourseEnrollment enrollUserInCourse(User user, Long courseId, EnrollmentType enrollmentType) {
        Optional<CourseEnrollment> existing = enrollmentRepository
            .findByUserIdAndCourseId(user.getId(), courseId);
        
        if (existing.isPresent()) {
            CourseEnrollment enrollment = existing.get();
            enrollment.setEnrollmentType(enrollmentType);
            enrollment.setIsActive(true);
            return enrollmentRepository.save(enrollment);
        }
        
        CourseEnrollment enrollment = CourseEnrollment.builder()
            .user(user)
            .courseId(courseId)
            .enrollmentType(enrollmentType)
            .groupId(null)
            .isActive(true)
            .build();
        
        return enrollmentRepository.save(enrollment);
    }

    /**
     * Get all enrollments for a user
     */
    public List<CourseEnrollment> getEnrollmentsByUser(UUID userId) {
        return enrollmentRepository.findByUserId(userId);
    }

    /**
     * Get all enrollments for a course
     */
    public List<CourseEnrollment> getEnrollmentsByCourse(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    /**
     * Get students in a course
     */
    public List<CourseEnrollment> getStudentsInCourse(Long courseId) {
        return enrollmentRepository.findByCourseIdAndEnrollmentType(courseId, EnrollmentType.STUDENT);
    }

    /**
     * Get teachers/advisers in a course
     */
    public List<CourseEnrollment> getTeachersInCourse(Long courseId) {
        return enrollmentRepository.findByCourseIdAndEnrollmentTypeIn(
            courseId, 
            List.of(EnrollmentType.TEACHER, EnrollmentType.ADVISER)
        );
    }

    /**
     * Get enrollment by user and course
     */
    public Optional<CourseEnrollment> getEnrollment(UUID userId, Long courseId) {
        return enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
    }

    /**
     * Check if user is enrolled in course
     */
    public boolean isEnrolled(UUID userId, Long courseId) {
        return enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }
}

