package com.scholarsync.backend.controller;

import com.scholarsync.backend.dto.CourseEnrollmentDto;
import com.scholarsync.backend.dto.GroupDto;
import com.scholarsync.backend.model.CourseEnrollment;
import com.scholarsync.backend.model.GroupEntity;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.repository.GroupRepository;
import com.scholarsync.backend.repository.UserRepository;
import com.scholarsync.backend.service.CourseEnrollmentService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class StudentController {

    // Helper class for response
    private static class EnrollmentWithGroup {
        public CourseEnrollmentDto student;
        public GroupDto group;
        
        public EnrollmentWithGroup(CourseEnrollmentDto student, GroupDto group) {
            this.student = student;
            this.group = group;
        }
    }

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final CourseEnrollmentService enrollmentService;

    public StudentController(UserRepository userRepository, 
                            GroupRepository groupRepository,
                            CourseEnrollmentService enrollmentService) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.enrollmentService = enrollmentService;
    }

    /**
     * Get student enrollment by institutional ID and course
     * GET /api/students/{institutionalId}?courseId=1
     */
    @GetMapping("/api/students/{institutionalId}")
    public ResponseEntity<?> getStudent(@PathVariable String institutionalId, @RequestParam Long courseId) {
        Optional<User> userOpt = userRepository.findByInstitutionalId(institutionalId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        
        User user = userOpt.get();
        Optional<CourseEnrollment> enrollmentOpt = enrollmentService.getEnrollment(user.getId(), courseId);
        
        if (enrollmentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("User not enrolled in the given course");
        }
        
        CourseEnrollment enrollment = enrollmentOpt.get();
        CourseEnrollmentDto dto = mapToDto(enrollment, user);
        
        // If student has a group, include group details
        if (enrollment.getGroupId() != null && !enrollment.getGroupId().isEmpty()) {
            Optional<GroupEntity> groupOpt = groupRepository.findById(enrollment.getGroupId());
            if (groupOpt.isPresent()) {
                GroupEntity group = groupOpt.get();
                GroupDto groupDto = mapGroupToDto(group);
                return ResponseEntity.ok(new EnrollmentWithGroup(dto, groupDto));
            }
        }
        
        return ResponseEntity.ok(dto);
    }

    /**
     * List students in a course
     * GET /api/students?courseId=1&q=searchTerm
     */
    @GetMapping("/api/students")
    public ResponseEntity<?> listStudents(@RequestParam Long courseId, @RequestParam(required = false) String q) {
        List<CourseEnrollment> enrollments = enrollmentService.getStudentsInCourse(courseId);
        
        // Apply search filter if provided
        if (q != null && !q.isBlank()) {
            String lq = q.toLowerCase();
            enrollments = enrollments.stream()
                .filter(e -> {
                    User user = e.getUser();
                    return (user.getInstitutionalId() != null && user.getInstitutionalId().toLowerCase().contains(lq))
                        || (user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(lq))
                        || (user.getEmail() != null && user.getEmail().toLowerCase().contains(lq));
                })
                .collect(Collectors.toList());
        }
        
        List<CourseEnrollmentDto> dtos = enrollments.stream()
            .map(e -> mapToDto(e, e.getUser()))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    private CourseEnrollmentDto mapToDto(CourseEnrollment enrollment, User user) {
        return new CourseEnrollmentDto(
            enrollment.getId(),
            user.getId(),
            user.getInstitutionalId(),
            user.getEmail(),
            user.getDisplayName(),
            null, // firstName - not in User model yet
            null, // lastName - not in User model yet
            enrollment.getCourseId(),
            enrollment.getEnrollmentType(),
            enrollment.getGroupId(),
            enrollment.getEnrolledAt(),
            enrollment.getIsActive()
        );
    }

    private GroupDto mapGroupToDto(GroupEntity group) {
        // Convert UUIDs to institutional IDs for API response
        User leader = userRepository.findById(group.getLeaderUserId()).orElse(null);
        String leaderInstitutionalId = leader != null ? leader.getInstitutionalId() : null;
        
        List<String> memberInstitutionalIds = group.getMemberUserIds().stream()
            .map(userId -> {
                User user = userRepository.findById(userId).orElse(null);
                return user != null ? user.getInstitutionalId() : null;
            })
            .filter(id -> id != null)
            .collect(Collectors.toList());
        
        String adviserInstitutionalId = null;
        if (group.getAdviserUserId() != null) {
            User adviser = userRepository.findById(group.getAdviserUserId()).orElse(null);
            adviserInstitutionalId = adviser != null ? adviser.getInstitutionalId() : null;
        }
        
        return new GroupDto(
            group.getGroupId(),
            group.getGroupName(),
            group.getCourseId(),
            leaderInstitutionalId,
            memberInstitutionalIds,
            adviserInstitutionalId,
            group.getCreatedAt()
        );
    }
}
