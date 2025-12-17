package com.scholarsync.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer courseId;
    
    @Column(name = "course_name", nullable = false, length = 30)
    private String courseName;
    
    @Column(name = "course_code", nullable = false, length = 10)
    private String courseCode;
    
    @Column(name = "course_desc", nullable = false, columnDefinition = "TEXT")
    private String courseDesc;
    
    @Column(name = "course_dur", nullable = false)
    private LocalDateTime courseDur;
    
    @Column(name = "course_adviser", nullable = false, length = 30)
    private String courseAdviser;
    
    @Column(name = "course_cap", nullable = false)
    private Integer courseCap;
    
    @Column(name = "course_stat", nullable = false)
    @Builder.Default
    private Integer courseStat = 1; // 1 = active, 0 = inactive
    
    @Column(name = "course_inv", nullable = false, length = 6, unique = true)
    private String courseInv; // 6-character invitation code
}
