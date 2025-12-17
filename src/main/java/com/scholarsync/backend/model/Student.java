package com.scholarsync.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(StudentId.class)
public class Student {
    @Id
    @Column(name = "student_id", nullable = false, length = 255)
    private String studentId;

    @Id
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "group_id")
    private String groupId;

    @Column(name = "lastname")
    private String lastName;

    @Column(name = "firstname")
    private String firstName;

    @Column(name = "email")
    private String email;
}
