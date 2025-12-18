package com.scholarsync.backend.controller;

import com.scholarsync.backend.dto.UserSearchDto;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.repository.StudentRepository;
import com.scholarsync.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for user-related endpoints, including user search for group member selection
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    /**
     * Search users by name or institutional ID
     * Used for adding members to groups
     * 
     * @param q Search query (searches display name and institutional ID)
     * @param courseId Optional course ID to filter by enrollment. If provided, only returns users enrolled in that course.
     * @return List of matching users
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchDto>> searchUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long courseId) {
        List<User> users;
        
        if (q != null && !q.trim().isEmpty()) {
            String searchQuery = q.trim().toLowerCase();
            // Get all users and filter by display name or institutional ID (case-insensitive)
            // This approach works even if the repository method doesn't match exactly
            List<User> allUsers = userRepository.findAll();
            users = allUsers.stream()
                .filter(user -> {
                    String displayName = user.getDisplayName() != null ? user.getDisplayName().toLowerCase() : "";
                    String institutionalId = user.getInstitutionalId() != null ? user.getInstitutionalId().toLowerCase() : "";
                    String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                    return displayName.contains(searchQuery) || 
                           institutionalId.contains(searchQuery) ||
                           email.contains(searchQuery);
                })
                .collect(Collectors.toList());
        } else {
            // If no query, return empty list (don't return all users)
            users = List.of();
        }
        
        // If courseId is provided, filter to only show users enrolled in that course
        if (courseId != null) {
            // Get all student IDs enrolled in this course
            Set<String> enrolledStudentIds = studentRepository.findAllByCourseId(courseId)
                .stream()
                .map(com.scholarsync.backend.model.Student::getStudentId)
                .collect(Collectors.toSet());
            
            // Filter users to only include those whose institutionalId matches enrolled students
            users = users.stream()
                .filter(user -> {
                    String institutionalId = user.getInstitutionalId();
                    return institutionalId != null && enrolledStudentIds.contains(institutionalId);
                })
                .collect(Collectors.toList());
        }
        
        // Convert to DTOs
        List<UserSearchDto> dtos = users.stream()
            .map(user -> new UserSearchDto(
                user.getId().toString(),
                user.getInstitutionalId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : "STUDENT"
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
}
