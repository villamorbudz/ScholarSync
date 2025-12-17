package com.scholarsync.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddStudentRequest {
    private String institutionalId; // The user's institutional ID
}
