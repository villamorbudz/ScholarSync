package com.scholarsync.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "courses")
@Data 
@NoArgsConstructor 
public class Course {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String courseCode;
    
    @Column(nullable = false)
    private String courseName;
    
    @Lob 
    private String courseDetails;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate startDate;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate endDate;
    
    private String adviserName;
    
    // ADD THIS FIELD to map to the required database column
    // The type should match the data type of 'professor_id' in your MySQL table (likely Long or Integer)
    private Long professorId; 
}