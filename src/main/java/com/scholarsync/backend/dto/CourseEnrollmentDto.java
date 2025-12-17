package com.scholarsync.backend.dto;

import com.scholarsync.backend.model.EnrollmentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseEnrollmentDto {
    private UUID enrollmentId;
    private UUID userId;
    private String institutionalId;
    private String email;
    private String displayName;
    private String firstName;
    private String lastName;
    private Long courseId;
    private EnrollmentType enrollmentType;
    private String groupId;
    private LocalDateTime enrolledAt;
    private Boolean isActive;
}

