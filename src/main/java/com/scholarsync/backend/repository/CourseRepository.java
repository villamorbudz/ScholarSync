package com.scholarsync.backend.repository;

import com.scholarsync.backend.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    // Basic CRUD methods inherited from JpaRepository
}