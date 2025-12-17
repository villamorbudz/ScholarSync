package com.scholarsync.backend.controller;

import com.scholarsync.backend.dto.AddStudentRequest;
import com.scholarsync.backend.dto.CourseCreateRequest;
import com.scholarsync.backend.dto.EnrollCourseRequest;
import com.scholarsync.backend.model.Course;
import com.scholarsync.backend.model.GroupEntity;
import com.scholarsync.backend.model.Role;
import com.scholarsync.backend.model.Student;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.repository.StudentRepository;
import com.scholarsync.backend.service.CourseService;
import com.scholarsync.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class CourseController {
    
    private final CourseService courseService;
    private final UserService userService;
    private final StudentRepository studentRepository;
    
    public CourseController(CourseService courseService, UserService userService, StudentRepository studentRepository) {
        this.courseService = courseService;
        this.userService = userService;
        this.studentRepository = studentRepository;
    }
    
    @PostMapping
    public ResponseEntity<?> createCourse(
            @RequestBody CourseCreateRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        try {
            log.info("Received course creation request - Name: {}, Code: {}, Duration: {}", 
                    request.getCourseName(), request.getCourseCode(), request.getCourseDur());
            
            // Check if user is authenticated and is a TEACHER
            if (oauth2User == null) {
                log.warn("Course creation attempted without authentication");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }
            
            String email = oauth2User.getAttribute("email");
            if (email == null) {
                email = oauth2User.getAttribute("userPrincipalName");
            }
            
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("Course creation attempted by non-existent user: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            if (user.getRole() != Role.TEACHER) {
                log.warn("Course creation attempted by non-teacher user: {} (role: {})", email, user.getRole());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only teachers can create courses"));
            }
            
            // Automatically set the creator as the adviser
            Course course = courseService.createCourse(request, user);
            log.info("Course created successfully: id={}, name={}", course.getCourseId(), course.getCourseName());
            return ResponseEntity.ok(course);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating course - Exception type: {}, Message: {}", 
                    e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        List<Course> courses = courseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }
    
    @GetMapping("/{courseId}")
    public ResponseEntity<?> getCourseById(@PathVariable Integer courseId) {
        try {
            Course course = courseService.getCourseById(courseId);
            return ResponseEntity.ok(course);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{courseId}/groups")
    public ResponseEntity<?> getGroupsByCourse(@PathVariable Integer courseId) {
        try {
            List<GroupEntity> groups = courseService.getGroupsByCourse(courseId.longValue());
            return ResponseEntity.ok(groups);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{courseId}/students")
    public ResponseEntity<?> addStudentToCourse(
            @PathVariable Integer courseId,
            @RequestBody AddStudentRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        try {
            // Check if user is authenticated and is a TEACHER
            if (oauth2User == null) {
                log.warn("Add student attempted without authentication");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }
            
            String email = oauth2User.getAttribute("email");
            if (email == null) {
                email = oauth2User.getAttribute("userPrincipalName");
            }
            
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("Add student attempted by non-existent user: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            User teacher = userOpt.get();
            if (teacher.getRole() != Role.TEACHER) {
                log.warn("Add student attempted by non-teacher user: {} (role: {})", email, teacher.getRole());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only teachers can add students to courses"));
            }
            
            // Validate course exists (will throw exception if not found)
            courseService.getCourseById(courseId);
            
            // Find user by institutional ID
            if (request.getInstitutionalId() == null || request.getInstitutionalId().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Institutional ID is required"));
            }
            
            Optional<User> studentUserOpt = userService.findByInstitutionalId(request.getInstitutionalId().trim());
            if (studentUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User with institutional ID " + request.getInstitutionalId() + " not found"));
            }
            
            User studentUser = studentUserOpt.get();
            
            // Check if student is already enrolled in this course
            Optional<Student> existingEnrollment = studentRepository.findByStudentIdAndCourseId(
                    studentUser.getInstitutionalId(), 
                    courseId.longValue()
            );
            if (existingEnrollment.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Student is already enrolled in this course"));
            }
            
            // Always create a new Student record for this course enrollment
            // This allows students to be enrolled in multiple courses
            Student student = new Student();
            student.setStudentId(studentUser.getInstitutionalId());
            student.setCourseId(courseId.longValue());
            student.setGroupId(null);
            
            // Set student name and email from user
            String displayName = studentUser.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                String[] nameParts = displayName.trim().split("\\s+");
                if (nameParts.length > 0) {
                    student.setFirstName(nameParts[0]);
                }
                if (nameParts.length > 1) {
                    // Join all parts after the first as last name
                    student.setLastName(String.join(" ", java.util.Arrays.copyOfRange(nameParts, 1, nameParts.length)));
                }
            } else {
                student.setFirstName("");
                student.setLastName("");
            }
            student.setEmail(studentUser.getEmail());
            
            Student savedStudent = studentRepository.save(student);
            log.info("Student {} added to course {}", savedStudent.getStudentId(), courseId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Student added successfully",
                "student", savedStudent
            ));
            
        } catch (RuntimeException e) {
            log.error("Error adding student to course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/enroll")
    public ResponseEntity<?> enrollInCourse(
            @RequestBody EnrollCourseRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        try {
            // Check if user is authenticated
            if (oauth2User == null) {
                log.warn("Course enrollment attempted without authentication");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }
            
            String email = oauth2User.getAttribute("email");
            if (email == null) {
                email = oauth2User.getAttribute("userPrincipalName");
            }
            
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("Course enrollment attempted by non-existent user: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            
            // Validate invitation code
            if (request.getCourseInv() == null || request.getCourseInv().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Course invitation code is required"));
            }
            
            String courseInv = request.getCourseInv().trim().toUpperCase();
            
            // Find course by invitation code
            Optional<Course> courseOpt = courseService.getCourseByInvitationCode(courseInv);
            if (courseOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Invalid course invitation code"));
            }
            
            Course course = courseOpt.get();
            
            // Check if user is already enrolled in this specific course
            Optional<Student> existingEnrollment = studentRepository.findByStudentIdAndCourseId(
                    user.getInstitutionalId(), 
                    course.getCourseId().longValue()
            );
            if (existingEnrollment.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "You are already enrolled in this course"));
            }
            
            // Always create a new Student record for this course enrollment
            // This allows students to be enrolled in multiple courses
            Student student = new Student();
            student.setStudentId(user.getInstitutionalId());
            student.setCourseId(course.getCourseId().longValue());
            student.setGroupId(null);
            
            // Set student name and email from user
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                String[] nameParts = displayName.trim().split("\\s+");
                if (nameParts.length > 0) {
                    student.setFirstName(nameParts[0]);
                }
                if (nameParts.length > 1) {
                    student.setLastName(String.join(" ", java.util.Arrays.copyOfRange(nameParts, 1, nameParts.length)));
                }
            } else {
                student.setFirstName("");
                student.setLastName("");
            }
            student.setEmail(user.getEmail());
            
            try {
                Student savedStudent = studentRepository.save(student);
                log.info("Student {} enrolled in course {} (invitation code: {})", 
                        savedStudent.getStudentId(), course.getCourseId(), courseInv);
                
                return ResponseEntity.ok(Map.of(
                    "message", "Successfully enrolled in course",
                    "course", course
                ));
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Handle database constraint violation (likely primary key issue)
                if (e.getMessage() != null && e.getMessage().contains("PRIMARY")) {
                    log.error("Database constraint violation - primary key issue. The database schema may need to be updated.");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Database configuration error. Please ensure the students table has a composite primary key (student_id, course_id). Contact administrator."));
                }
                throw e;
            }
            
        } catch (RuntimeException e) {
            log.error("Error enrolling in course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/my-courses")
    public ResponseEntity<?> getMyCourses(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            // Check if user is authenticated
            if (oauth2User == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }
            
            String email = oauth2User.getAttribute("email");
            if (email == null) {
                email = oauth2User.getAttribute("userPrincipalName");
            }
            
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            
            // Get all courses the student is enrolled in
            List<Student> studentRecords = studentRepository.findAllByStudentId(user.getInstitutionalId());
            List<Long> courseIds = studentRecords.stream()
                    .map(Student::getCourseId)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
            
            if (courseIds.isEmpty()) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
            
            List<Course> courses = courseService.getCoursesByIds(courseIds);
            return ResponseEntity.ok(courses);
            
        } catch (Exception e) {
            log.error("Error fetching student courses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch courses"));
        }
    }
    
    @PutMapping("/{courseId}")
    public ResponseEntity<?> updateCourse(
            @PathVariable Integer courseId,
            @RequestBody CourseCreateRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        try {
            log.info("Received course update request - ID: {}, Name: {}, Code: {}", 
                    courseId, request.getCourseName(), request.getCourseCode());
            
            // Check if user is authenticated and is a TEACHER
            if (oauth2User == null) {
                log.warn("Course update attempted without authentication");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }
            
            String email = oauth2User.getAttribute("email");
            if (email == null) {
                email = oauth2User.getAttribute("userPrincipalName");
            }
            
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("Course update attempted by non-existent user: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            if (user.getRole() != Role.TEACHER) {
                log.warn("Course update attempted by non-teacher user: {} (role: {})", email, user.getRole());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only teachers can update courses"));
            }
            
            Course updatedCourse = courseService.updateCourse(courseId, request, user);
            log.info("Course updated successfully: id={}, name={}", updatedCourse.getCourseId(), updatedCourse.getCourseName());
            return ResponseEntity.ok(updatedCourse);
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error updating course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating course - Exception type: {}, Message: {}", 
                    e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> deleteCourse(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        try {
            log.info("Received course deletion request - ID: {}", courseId);
            
            // Check if user is authenticated and is a TEACHER
            if (oauth2User == null) {
                log.warn("Course deletion attempted without authentication");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }
            
            String email = oauth2User.getAttribute("email");
            if (email == null) {
                email = oauth2User.getAttribute("userPrincipalName");
            }
            
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("Course deletion attempted by non-existent user: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            if (user.getRole() != Role.TEACHER) {
                log.warn("Course deletion attempted by non-teacher user: {} (role: {})", email, user.getRole());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only teachers can delete courses"));
            }
            
            courseService.deleteCourse(courseId);
            log.info("Course deleted successfully: id={}", courseId);
            return ResponseEntity.ok(Map.of("message", "Course deleted successfully"));
        } catch (RuntimeException e) {
            log.error("Error deleting course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting course - Exception type: {}, Message: {}", 
                    e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}
