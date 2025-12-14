package com.scholarsync.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDto {
    private String studentId;
    private Long courseId;
    private String groupId;
    private String lastName;
    private String firstName;
    private String email;
}
