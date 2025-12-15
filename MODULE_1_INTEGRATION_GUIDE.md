# Module 1 Integration Guide - Authentication System

**Version**: 1.0  
**Last Updated**: December 15, 2025  
**Status**: Production Ready

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication Flow](#authentication-flow)
3. [Available APIs](#available-apis)
4. [Getting Current User](#getting-current-user)
5. [Role-Based Access Control](#role-based-access-control)
6. [Data Models](#data-models)
7. [Integration Patterns](#integration-patterns)
8. [Security Configuration](#security-configuration)
9. [Error Handling](#error-handling)
10. [Examples](#examples)
11. [Best Practices](#best-practices)

---

## Overview

The authentication system provides **Microsoft OAuth 2.0** integration with automatic user registration and role determination. All user data is synced from Microsoft accounts to maintain data integrity.

### Key Features

- ✅ **Unified Sign-up/Login**: Single "Sign in with Microsoft" button handles both registration and login
- ✅ **Automatic Role Assignment**: Roles (STUDENT, TEACHER, ADMIN) determined from institutional IDs
- ✅ **Session Management**: Spring Security sessions with JSESSIONID cookies
- ✅ **Data Integrity**: All user data synced from Microsoft (no manual updates)

### Important Notes

- **Registration**: ONLY possible via Microsoft OAuth (no registration forms)
- **User Updates**: NOT allowed - all data synced from Microsoft on login
- **Role Changes**: Automatic - re-evaluated on each login based on `jobTitle`

---

## Authentication Flow

### User Registration/Login Process

```
1. User clicks "Sign in with Microsoft" → /login
2. Redirects to Microsoft OAuth login
3. User authenticates with Microsoft account
4. Microsoft redirects back with authorization code
5. Backend exchanges code for access token
6. Backend fetches user profile from Microsoft Graph API
7. System checks if user exists (by microsoftId)
   - If NEW: Auto-creates user account
   - If EXISTS: Updates profile data
8. Determines role from jobTitle (institutional ID)
9. Creates session and redirects to /api/auth/success
```

### Session Management

- **Session Type**: Spring Security HTTP Sessions
- **Session ID**: JSESSIONID cookie
- **Session Duration**: 30 minutes (default Spring timeout)
- **Max Sessions**: 1 session per user

---

## Available APIs

### Base URL
```
http://localhost:8080
```

### Public Endpoints (No Authentication Required)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/login` | GET | Login page HTML |
| `/login/oauth2/authorization/microsoft` | GET | Start OAuth flow |
| `/api/public/**` | * | Public API endpoints |

### Protected Endpoints (Authentication Required)

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/auth/success` | GET | Get authentication success details | ✅ Yes |
| `/api/auth/me` | GET | Get current authenticated user | ✅ Yes |
| `/api/**` | * | All other API endpoints | ✅ Yes |

---

## Getting Current User

### Method 1: Using `/api/auth/me` Endpoint

**Request**:
```http
GET /api/auth/me
Cookie: JSESSIONID=<session_id>
```

**Response** (200 OK):
```json
{
  "id": "1b6c720e-cf5d-44a0-826e-3a7e1d48ad43",
  "email": "gilesanthony.villamorii@cit.edu",
  "displayName": "Giles Anthony I. Villamor II",
  "role": "STUDENT",
  "jobTitle": "22-0369-330"
}
```

**Error Response** (401 Unauthorized):
```json
{
  "error": "Not authenticated"
}
```

### Method 2: Using Spring Security Annotation in Controllers

**In Your Controller**:
```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.service.UserService;

@RestController
@RequestMapping("/api/your-feature")
@RequiredArgsConstructor
public class YourController {
    
    private final UserService userService;
    
    @GetMapping("/example")
    public ResponseEntity<?> yourEndpoint(
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        // Get email from OAuth2User
        String email = oauth2User.getAttribute("email");
        if (email == null) {
            email = oauth2User.getAttribute("userPrincipalName");
        }
        
        // Get User entity from database
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        User user = userOpt.get();
        
        // Use user data
        UUID userId = user.getId();
        Role userRole = user.getRole();
        String displayName = user.getDisplayName();
        
        // Your business logic here...
        
        return ResponseEntity.ok(Map.of("message", "Success"));
    }
}
```

### Method 3: Direct UserService Injection

**In Your Service**:
```java
import com.scholarsync.backend.model.User;
import com.scholarsync.backend.service.UserService;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class YourService {
    
    private final UserService userService;
    
    public void yourMethod(UUID userId) {
        // Get user by ID
        Optional<User> userOpt = userService.findById(userId);
        // Or get by email
        Optional<User> userOpt = userService.findByEmail("user@example.com");
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Role role = user.getRole();
            // Use user data...
        }
    }
}
```

---

## Role-Based Access Control

### Available Roles

```java
public enum Role {
    STUDENT,   // Student users (institutional ID patterns: 22-####-###, 2010-#####)
    TEACHER,   // Teacher users (institutional ID pattern: 1-####)
    ADMIN,     // Admin users (jobTitle contains "ADMIN" or "ADMINISTRATOR")
    UNKNOWN    // Unmatched patterns (for testing)
}
```

### Role Determination Logic

Roles are automatically determined from `jobTitle` (institutional ID):

- **STUDENT**: 
  - Pattern 1: `YY-####-###` (e.g., `22-1234-567`)
  - Pattern 2: `YYYY-#####` (e.g., `2010-12345`)
- **TEACHER**: 
  - Pattern: `1-####` (e.g., `1-0001`, `1-1234`)
- **ADMIN**: 
  - Pattern: Contains "ADMIN" or "ADMINISTRATOR"
- **UNKNOWN**: 
  - Default for unmatched patterns

### Implementing Role-Based Access Control

#### Option 1: Method-Level Security (Recommended)

```java
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    
    // Only teachers can create courses
    @PostMapping("/create")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> createCourse(@RequestBody CourseDTO course) {
        // Only TEACHER role can access this
        return ResponseEntity.ok("Course created");
    }
    
    // Only admins can delete courses
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCourse(@PathVariable UUID id) {
        // Only ADMIN role can access this
        return ResponseEntity.ok("Course deleted");
    }
    
    // Students and teachers can view courses
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<?> getCourse(@PathVariable UUID id) {
        // STUDENT or TEACHER can access this
        return ResponseEntity.ok("Course details");
    }
}
```

**Note**: You'll need to enable method security in your configuration:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Add this annotation
public class SecurityConfig {
    // ... existing configuration
}
```

#### Option 2: Manual Role Checking

```java
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    
    private final UserService userService;
    
    @PostMapping("/create")
    public ResponseEntity<?> createCourse(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestBody CourseDTO course) {
        
        // Get current user
        String email = oauth2User.getAttribute("email");
        Optional<User> userOpt = userService.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        User user = userOpt.get();
        
        // Check role manually
        if (user.getRole() != Role.TEACHER && user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(
                Map.of("error", "Only teachers and admins can create courses")
            );
        }
        
        // Proceed with course creation
        return ResponseEntity.ok("Course created");
    }
}
```

#### Option 3: Service Layer Role Checking

```java
@Service
@RequiredArgsConstructor
public class CourseService {
    
    private final UserService userService;
    
    public Course createCourse(UUID userId, CourseDTO courseDTO) {
        // Get user and verify role
        User user = userService.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        if (user.getRole() != Role.TEACHER && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only teachers and admins can create courses");
        }
        
        // Create course logic...
        return course;
    }
}
```

---

## Data Models

### User Entity

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;                    // Primary key
    
    @Column(nullable = false, unique = true)
    private String email;               // User email (from Microsoft)
    
    @Column(nullable = false, unique = true)
    private String microsoftId;         // Microsoft Object ID (primary identifier)
    
    private String displayName;         // Full name (from Microsoft)
    private String jobTitle;            // Institutional ID (from Microsoft)
    
    @Enumerated(EnumType.STRING)
    private Role role;                  // STUDENT, TEACHER, ADMIN, UNKNOWN
    
    private LocalDateTime accountCreatedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastOAuthSyncAt;
    
    private Boolean isActive;           // Account status
    private Boolean emailVerified;      // From Microsoft
}
```

### User JSON Representation

```json
{
  "id": "1b6c720e-cf5d-44a0-826e-3a7e1d48ad43",
  "email": "gilesanthony.villamorii@cit.edu",
  "microsoftId": "4c09bc03-ed4a-46ec-b37c-644ab628d4b6",
  "displayName": "Giles Anthony I. Villamor II",
  "jobTitle": "22-0369-330",
  "role": "STUDENT",
  "emailVerified": true,
  "accountCreatedAt": "2025-12-15T12:29:05.979753",
  "lastLoginAt": "2025-12-15T14:30:00.000000",
  "isActive": true
}
```

### Role Enum

```java
public enum Role {
    STUDENT,   // Default for student institutional IDs
    TEACHER,   // For teacher institutional IDs (1-####)
    ADMIN,     // For admin users
    UNKNOWN    // For unmatched patterns
}
```

---

## Integration Patterns

### Pattern 1: Get Current User in Controller

```java
@RestController
@RequestMapping("/api/your-feature")
@RequiredArgsConstructor
public class YourController {
    
    private final UserService userService;
    
    @GetMapping("/protected-endpoint")
    public ResponseEntity<?> protectedEndpoint(
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        // Extract email
        String email = oauth2User.getAttribute("email");
        if (email == null) {
            email = oauth2User.getAttribute("userPrincipalName");
        }
        
        // Get user entity
        User user = userService.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Use user data
        UUID userId = user.getId();
        Role userRole = user.getRole();
        
        // Your business logic...
        
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "role", userRole.toString()
        ));
    }
}
```

### Pattern 2: Pass User ID to Service Layer

```java
@RestController
@RequestMapping("/api/your-feature")
@RequiredArgsConstructor
public class YourController {
    
    private final YourService yourService;
    private final UserService userService;
    
    @PostMapping("/create")
    public ResponseEntity<?> createResource(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestBody YourDTO dto) {
        
        // Get current user
        String email = oauth2User.getAttribute("email");
        User user = userService.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Pass user ID to service
        YourResource resource = yourService.createResource(user.getId(), dto);
        
        return ResponseEntity.ok(resource);
    }
}
```

### Pattern 3: Role-Based Feature Access

```java
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    
    private final CourseService courseService;
    private final UserService userService;
    
    @GetMapping("/my-courses")
    public ResponseEntity<?> getMyCourses(
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        // Get current user
        String email = oauth2User.getAttribute("email");
        User user = userService.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Different logic based on role
        List<Course> courses;
        if (user.getRole() == Role.TEACHER) {
            courses = courseService.getCoursesByTeacher(user.getId());
        } else if (user.getRole() == Role.STUDENT) {
            courses = courseService.getCoursesByStudent(user.getId());
        } else {
            return ResponseEntity.status(403).body(
                Map.of("error", "Access denied")
            );
        }
        
        return ResponseEntity.ok(courses);
    }
}
```

### Pattern 4: Foreign Key Relationships

When creating entities that reference users:

```java
@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String name;
    private String description;
    
    // Reference to User entity
    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;  // Use User entity, not just UUID
    
    // Or if you prefer UUID reference:
    @Column(name = "teacher_id")
    private UUID teacherId;
    
    // ... other fields
}
```

**Service Layer**:
```java
@Service
@RequiredArgsConstructor
public class CourseService {
    
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    
    public Course createCourse(UUID teacherId, CourseDTO dto) {
        // Verify teacher exists
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new UserNotFoundException("Teacher not found"));
        
        // Verify user is actually a teacher
        if (teacher.getRole() != Role.TEACHER) {
            throw new UnauthorizedException("User is not a teacher");
        }
        
        // Create course
        Course course = Course.builder()
            .name(dto.getName())
            .description(dto.getDescription())
            .teacher(teacher)  // Set User entity
            .build();
        
        return courseRepository.save(course);
    }
}
```

---

## Security Configuration

### Current Security Setup

- **CSRF**: Disabled (for API usage)
- **Session Management**: `IF_REQUIRED` (sessions created on authentication)
- **Max Sessions**: 1 per user
- **Public Endpoints**: `/`, `/login`, `/static/**`, `/api/public/**`
- **Protected Endpoints**: All `/api/**` except `/api/public/**`

### Adding Your Endpoints to Security Config

If you need to add public endpoints, update `SecurityConfig.java`:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .maximumSessions(1)
            .maxSessionsPreventsLogin(false))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/login", "/login.html", "/static/**", "/error").permitAll()
            .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
            .requestMatchers("/api/public/**").permitAll()
            .requestMatchers("/api/your-public-endpoint/**").permitAll()  // Add your public endpoints
            .anyRequest().authenticated())
        // ... rest of config
}
```

---

## Error Handling

### Common Error Responses

#### 401 Unauthorized (Not Authenticated)
```json
{
  "error": "Not authenticated"
}
```
**When**: User not logged in or session expired

#### 403 Forbidden (Access Denied)
```json
{
  "error": "Access denied",
  "message": "Only teachers can access this resource"
}
```
**When**: User doesn't have required role/permission

#### 404 Not Found (User Not Found)
```json
{
  "error": "User not found"
}
```
**When**: User doesn't exist in database

### Error Handling Pattern

```java
@RestController
@RequestMapping("/api/your-feature")
@RequiredArgsConstructor
public class YourController {
    
    private final UserService userService;
    
    @GetMapping("/example")
    public ResponseEntity<?> example(
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        // Check authentication
        if (oauth2User == null) {
            return ResponseEntity.status(401).body(
                Map.of("error", "Not authenticated")
            );
        }
        
        // Get user
        String email = oauth2User.getAttribute("email");
        Optional<User> userOpt = userService.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(
                Map.of("error", "User not found")
            );
        }
        
        User user = userOpt.get();
        
        // Check role
        if (user.getRole() != Role.TEACHER) {
            return ResponseEntity.status(403).body(
                Map.of("error", "Access denied", 
                       "message", "Only teachers can access this endpoint")
            );
        }
        
        // Your logic...
        return ResponseEntity.ok("Success");
    }
}
```

---

## Examples

### Example 1: Create Course (Teacher Only)

```java
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    
    private final CourseService courseService;
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<?> createCourse(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestBody CreateCourseDTO dto) {
        
        // Get current user
        String email = oauth2User.getAttribute("email");
        User teacher = userService.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Verify teacher role
        if (teacher.getRole() != Role.TEACHER && teacher.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(
                Map.of("error", "Only teachers can create courses")
            );
        }
        
        // Create course
        Course course = courseService.createCourse(teacher.getId(), dto);
        
        return ResponseEntity.status(201).body(course);
    }
}
```

### Example 2: Get User Profile

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        String email = oauth2User.getAttribute("email");
        User user = userService.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        Map<String, Object> profile = Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "displayName", user.getDisplayName(),
            "role", user.getRole().toString(),
            "jobTitle", user.getJobTitle() != null ? user.getJobTitle() : "",
            "emailVerified", user.getEmailVerified(),
            "accountCreatedAt", user.getAccountCreatedAt()
        );
        
        return ResponseEntity.ok(profile);
    }
}
```

### Example 3: List Students (Teacher/Admin Only)

```java
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {
    
    private final UserService userService;
    
    @GetMapping
    public ResponseEntity<?> listStudents(
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        String email = oauth2User.getAttribute("email");
        User currentUser = userService.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Check permissions
        if (currentUser.getRole() != Role.TEACHER && 
            currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(
                Map.of("error", "Access denied")
            );
        }
        
        // Get all students
        List<User> students = userService.findByRole(Role.STUDENT);
        
        List<Map<String, Object>> studentList = students.stream()
            .map(student -> Map.of(
                "id", student.getId(),
                "email", student.getEmail(),
                "displayName", student.getDisplayName(),
                "jobTitle", student.getJobTitle()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(studentList);
    }
}
```

---

## Best Practices

### ✅ DO

1. **Always check authentication** before accessing protected resources
2. **Use `@AuthenticationPrincipal OAuth2User`** to get current user in controllers
3. **Get User entity from database** using `UserService.findByEmail()`
4. **Check roles** before allowing access to role-specific features
5. **Use User entity** in foreign key relationships (not just UUID)
6. **Handle Optional<User>** properly (use `orElseThrow()` or check `isPresent()`)
7. **Return appropriate HTTP status codes** (401, 403, 404, etc.)
8. **Include user ID** in created resources for audit trails

### ❌ DON'T

1. **Don't allow manual user updates** - data synced from Microsoft only
2. **Don't store user data in frontend** - always fetch from backend
3. **Don't trust client-side role checks** - always verify on backend
4. **Don't create users manually** - registration only via OAuth
5. **Don't modify role directly** - role is auto-determined from `jobTitle`
6. **Don't bypass authentication** - all `/api/**` endpoints require auth

---

## Service Layer Integration

### Available Services

#### UserService

```java
@Service
public class UserService {
    // Find user by Microsoft ID
    Optional<User> findByMicrosoftId(String microsoftId);
    
    // Find user by email
    Optional<User> findByEmail(String email);
    
    // Create/update user from OAuth (internal use only)
    User createOrUpdateUserFromOAuth(...);
}
```

**Usage Example**:
```java
@Service
@RequiredArgsConstructor
public class YourService {
    
    private final UserService userService;
    
    public void yourMethod(UUID userId) {
        // Get user
        User user = userService.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Use user data
        Role role = user.getRole();
        String email = user.getEmail();
        
        // Your business logic...
    }
}
```

#### RoleDeterminationService

```java
@Service
public class RoleDeterminationService {
    // Determine role from jobTitle (institutional ID)
    Role determineRole(String jobTitle);
}
```

**Usage Example**:
```java
@Service
@RequiredArgsConstructor
public class YourService {
    
    private final RoleDeterminationService roleDeterminationService;
    
    public void validateRole(String jobTitle) {
        Role role = roleDeterminationService.determineRole(jobTitle);
        // role will be STUDENT, TEACHER, ADMIN, or UNKNOWN
    }
}
```

---

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    microsoft_id VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    job_title VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    local_password_hash VARCHAR(255),
    account_created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    last_oauth_sync_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    email_verified BOOLEAN NOT NULL DEFAULT false
);
```

### Foreign Key Relationships

When creating tables that reference users:

```sql
CREATE TABLE courses (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    teacher_id UUID NOT NULL,
    FOREIGN KEY (teacher_id) REFERENCES users(id),
    -- ... other fields
);

CREATE TABLE enrollments (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL,
    student_id UUID NOT NULL,
    FOREIGN KEY (course_id) REFERENCES courses(id),
    FOREIGN KEY (student_id) REFERENCES users(id),
    -- ... other fields
);
```

---

## Testing Integration

### Testing with Postman

1. **Get Session ID**:
   - Complete OAuth login in browser
   - Copy JSESSIONID from browser cookies
   - Use in Postman: `Cookie: JSESSIONID=<value>`

2. **Test Protected Endpoints**:
   ```
   GET /api/auth/me
   Headers: Cookie: JSESSIONID=<your_session_id>
   ```

3. **Test Role-Based Access**:
   - Login as different users (student, teacher, admin)
   - Test endpoints with different roles
   - Verify 403 errors for unauthorized access

### Testing in Code

```java
@SpringBootTest
@AutoConfigureMockMvc
class YourFeatureTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testProtectedEndpoint() throws Exception {
        // Mock authenticated user
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("email")).thenReturn("test@example.com");
        
        mockMvc.perform(get("/api/your-endpoint")
                .with(user(oauth2User)))
            .andExpect(status().isOk());
    }
}
```

---

## Troubleshooting

### Common Issues

#### Issue: 401 Unauthorized
**Cause**: User not authenticated or session expired  
**Solution**: 
- Check if JSESSIONID cookie is present
- Verify session hasn't expired (30 min default)
- Re-authenticate via OAuth

#### Issue: 404 User Not Found
**Cause**: User doesn't exist in database  
**Solution**: 
- User must register via OAuth first
- Check if email matches Microsoft account

#### Issue: Role Not Updating
**Cause**: `jobTitle` hasn't changed or role determination failed  
**Solution**: 
- Role updates automatically on next OAuth login
- Verify `jobTitle` matches expected pattern
- Check `RoleDeterminationService` logic

#### Issue: Cannot Access Protected Endpoint
**Cause**: Missing authentication or wrong role  
**Solution**: 
- Verify user is authenticated
- Check if user has required role
- Review security configuration

---

## Contact & Support

For questions or issues with authentication integration:

1. **Check this documentation** first
2. **Review code examples** in `/src/main/java/com/scholarsync/backend/`
3. **Check existing implementations** in `AuthController` and `UserService`
4. **Contact**: Authentication module maintainer

---

## Changelog

### Version 1.0 (December 15, 2025)
- Initial release
- Microsoft OAuth integration
- Role-based access control
- Session management
- User data sync from Microsoft

---

**End of Integration Guide**

