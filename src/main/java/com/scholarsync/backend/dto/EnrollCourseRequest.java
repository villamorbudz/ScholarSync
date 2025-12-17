package com.scholarsync.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollCourseRequest {
    private String courseInv; // 6-character invitation code
}
