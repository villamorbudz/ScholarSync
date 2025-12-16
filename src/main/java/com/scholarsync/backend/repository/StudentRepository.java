package com.scholarsync.backend.repository;

import com.scholarsync.backend.model.Student;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    List<Student> findAllByStudentIdIn(List<String> studentIds);
    List<Student> findAllByCourseId(Long courseId);
}
