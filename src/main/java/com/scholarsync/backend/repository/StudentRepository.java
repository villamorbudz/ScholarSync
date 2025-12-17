package com.scholarsync.backend.repository;

import com.scholarsync.backend.model.Student;
import com.scholarsync.backend.model.StudentId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, StudentId> {
    List<Student> findAllByStudentIdIn(List<String> studentIds);
    List<Student> findAllByCourseId(Long courseId);
    List<Student> findAllByStudentId(String studentId);
    Optional<Student> findByStudentIdAndCourseId(String studentId, Long courseId);
    List<Student> findByStudentIdInAndCourseId(List<String> studentIds, Long courseId);
}
