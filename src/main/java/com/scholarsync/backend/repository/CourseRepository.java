package com.scholarsync.backend.repository;

import com.scholarsync.backend.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {
    List<Course> findAllByOrderByCourseIdDesc();
    boolean existsByCourseInv(String courseInv);
    Optional<Course> findByCourseInv(String courseInv);
}
