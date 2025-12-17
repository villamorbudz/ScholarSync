package com.scholarsync.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user search results in group member search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchDto {
    private String id; // User UUID as string
    private String institutionalId;
    private String displayName;
    private String email;
    private String role;
}
