package com.scholarsync.backend.repository;

import com.scholarsync.backend.model.CourseEnrollment;
import com.scholarsync.backend.model.EnrollmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {
    
    // Find all enrollments for a user
    List<CourseEnrollment> findByUserId(UUID userId);
    
    // Find all enrollments for a course
    List<CourseEnrollment> findByCourseId(Long courseId);
    
    // Find specific enrollment
    Optional<CourseEnrollment> findByUserIdAndCourseId(UUID userId, Long courseId);
    
    // Find enrollments by user IDs and course (for Excel import validation)
    List<CourseEnrollment> findByUserIdInAndCourseId(List<UUID> userIds, Long courseId);
    
    // Find students in a course
    List<CourseEnrollment> findByCourseIdAndEnrollmentType(Long courseId, EnrollmentType type);
    
    // Find teachers/advisers in a course
    List<CourseEnrollment> findByCourseIdAndEnrollmentTypeIn(Long courseId, List<EnrollmentType> types);
    
    // Find enrollment by group
    List<CourseEnrollment> findByGroupId(String groupId);
    
    // Check if user is enrolled
    boolean existsByUserIdAndCourseId(UUID userId, Long courseId);
}

