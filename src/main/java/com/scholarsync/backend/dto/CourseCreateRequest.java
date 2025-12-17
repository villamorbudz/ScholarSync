package com.scholarsync.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateRequest {
    private String courseName;
    private String courseCode;
    private String courseDesc;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime courseDur;
    
    private String courseAdviser; // Optional - will be set automatically
    private Integer courseCap;
    private Integer courseStat;
}
