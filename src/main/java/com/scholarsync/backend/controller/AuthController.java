package com.scholarsync.backend.controller;

import com.scholarsync.backend.model.User;
import com.scholarsync.backend.service.MicrosoftGraphService;
import com.scholarsync.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> authSuccess(
            @AuthenticationPrincipal OAuth2User oauth2User,
            jakarta.servlet.http.HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        // First, check if user was already saved by OAuth2LoginSuccessHandler
        // Check session first (for redirects), then request attributes (for forwards)
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        User savedUser = null;
        if (session != null) {
            savedUser = (User) session.getAttribute("savedUser");
        }
        if (savedUser == null) {
            savedUser = (User) request.getAttribute("savedUser");
        }
        
        if (savedUser != null) {
            // User was successfully created/updated by OAuth2LoginSuccessHandler
            response.put("success", true);
            
            // Check if this was a new registration or existing login
            Boolean isNewUser = null;
            if (session != null) {
                isNewUser = (Boolean) session.getAttribute("isNewUser");
            }
            if (isNewUser == null) {
                isNewUser = (Boolean) request.getAttribute("isNewUser");
            }
            if (isNewUser != null && isNewUser) {
                response.put("message", "Account created and signed in successfully");
                response.put("isNewUser", true);
            } else {
                response.put("message", "Signed in successfully");
                response.put("isNewUser", false);
            }
            
            // authenticationMethod is handled via OAuth flow only
            
            // User info from database
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", savedUser.getId());
            userInfo.put("email", savedUser.getEmail());
            userInfo.put("displayName", savedUser.getDisplayName());
            userInfo.put("role", savedUser.getRole());
            userInfo.put("jobTitle", savedUser.getJobTitle());
            userInfo.put("microsoftId", savedUser.getMicrosoftId());
            userInfo.put("emailVerified", savedUser.getEmailVerified());
            userInfo.put("accountCreatedAt", savedUser.getAccountCreatedAt());
            userInfo.put("lastLoginAt", savedUser.getLastLoginAt());
            
            response.put("user", userInfo);
            
            // Microsoft Graph API fetched info (if available from session or request)
            MicrosoftGraphService.UserProfile microsoftProfile = null;
            if (session != null) {
                microsoftProfile = (MicrosoftGraphService.UserProfile) session.getAttribute("microsoftProfile");
            }
            if (microsoftProfile == null) {
                microsoftProfile = (MicrosoftGraphService.UserProfile) request.getAttribute("microsoftProfile");
            }
            
            if (microsoftProfile != null) {
                Map<String, Object> microsoftInfo = new HashMap<>();
                microsoftInfo.put("id", microsoftProfile.getId());
                microsoftInfo.put("displayName", microsoftProfile.getDisplayName());
                microsoftInfo.put("mail", microsoftProfile.getMail());
                microsoftInfo.put("userPrincipalName", microsoftProfile.getUserPrincipalName());
                microsoftInfo.put("jobTitle", microsoftProfile.getJobTitle());
                response.put("microsoftProfile", microsoftInfo);
            }
            
            // Clean up session attributes after reading
            if (session != null) {
                session.removeAttribute("microsoftProfile");
                session.removeAttribute("savedUser");
                session.removeAttribute("isNewUser");
            }
            
            return ResponseEntity.ok(response);
        }
        
        // Fallback: If savedUser is not in request, try to look up by email
        if (oauth2User != null) {
            String email = oauth2User.getAttribute("email");
            if (email == null) {
                email = oauth2User.getAttribute("userPrincipalName");
            }
            
            Optional<User> user = userService.findByEmail(email);
            
            if (user.isPresent()) {
                User userEntity = user.get();
                response.put("success", true);
                
                // Check if this was a new registration or existing login
                Boolean isNewUser = (Boolean) request.getAttribute("isNewUser");
                if (isNewUser != null && isNewUser) {
                    response.put("message", "Account created and signed in successfully");
                    response.put("isNewUser", true);
                } else {
                    response.put("message", "Signed in successfully");
                    response.put("isNewUser", false);
                }
                
                // User info from database
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", userEntity.getId());
                userInfo.put("email", userEntity.getEmail());
                userInfo.put("displayName", userEntity.getDisplayName());
                userInfo.put("role", userEntity.getRole());
                userInfo.put("jobTitle", userEntity.getJobTitle());
                userInfo.put("microsoftId", userEntity.getMicrosoftId());
                userInfo.put("emailVerified", userEntity.getEmailVerified());
                userInfo.put("accountCreatedAt", userEntity.getAccountCreatedAt());
                userInfo.put("lastLoginAt", userEntity.getLastLoginAt());
                
                response.put("user", userInfo);
                
                // Microsoft Graph API fetched info (if available from request)
                MicrosoftGraphService.UserProfile microsoftProfile = 
                    (MicrosoftGraphService.UserProfile) request.getAttribute("microsoftProfile");
                
                if (microsoftProfile != null) {
                    Map<String, Object> microsoftInfo = new HashMap<>();
                    microsoftInfo.put("id", microsoftProfile.getId());
                    microsoftInfo.put("displayName", microsoftProfile.getDisplayName());
                    microsoftInfo.put("mail", microsoftProfile.getMail());
                    microsoftInfo.put("userPrincipalName", microsoftProfile.getUserPrincipalName());
                    microsoftInfo.put("jobTitle", microsoftProfile.getJobTitle());
                    response.put("microsoftProfile", microsoftInfo);
                }
            } else {
                // User not found - try to create from OAuth2User attributes as fallback
                // This handles cases where Microsoft Graph API call failed in OAuth2LoginSuccessHandler
                String microsoftId = oauth2User.getAttribute("oid"); // Microsoft Object ID
                String displayName = oauth2User.getAttribute("name");
                
                // Extract institutional ID from given_name (e.g., "22-0369-330 Giles Anthony" -> "22-0369-330")
                // OAuth2User doesn't have jobTitle, but institutional ID is in given_name
                String givenName = oauth2User.getAttribute("given_name");
                String jobTitle = null;
                if (givenName != null && !givenName.isEmpty()) {
                    // Extract the institutional ID pattern from given_name
                    // Pattern: "22-0369-330 Giles Anthony" or "2010-12345 Name" or "1-1234 Name"
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "(\\d{2}-\\d{4}-\\d{3}|\\d{4}-\\d{5}|1-\\d{4})"
                    );
                    java.util.regex.Matcher matcher = pattern.matcher(givenName);
                    if (matcher.find()) {
                        jobTitle = matcher.group(1);
                    }
                }
                
                // Fallback: try jobTitle attribute if given_name extraction failed
                if (jobTitle == null) {
                    jobTitle = oauth2User.getAttribute("jobTitle");
                }
                
                if (microsoftId != null && email != null) {
                    // Try to create user from OAuth2User attributes
                    try {
                        User fallbackUser = userService.createOrUpdateUserFromOAuth(
                            microsoftId,
                            email,
                            displayName,
                            jobTitle,
                            true // Microsoft accounts are verified
                        );
                        
                        response.put("success", true);
                        response.put("message", "Account created and signed in successfully (fallback)");
                        response.put("isNewUser", true);
                        
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("id", fallbackUser.getId());
                        userInfo.put("email", fallbackUser.getEmail());
                        userInfo.put("displayName", fallbackUser.getDisplayName());
                        userInfo.put("role", fallbackUser.getRole());
                        userInfo.put("jobTitle", fallbackUser.getJobTitle());
                        userInfo.put("microsoftId", fallbackUser.getMicrosoftId());
                        userInfo.put("emailVerified", fallbackUser.getEmailVerified());
                        userInfo.put("accountCreatedAt", fallbackUser.getAccountCreatedAt());
                        userInfo.put("lastLoginAt", fallbackUser.getLastLoginAt());
                        
                        response.put("user", userInfo);
                        
                        // Include OAuth2 user details
                        Map<String, Object> oauth2Info = new HashMap<>();
                        oauth2Info.put("name", oauth2User.getName());
                        oauth2Info.put("email", email);
                        oauth2Info.put("attributes", oauth2User.getAttributes());
                        response.put("oauth2User", oauth2Info);
                        
                        return ResponseEntity.ok(response);
                    } catch (Exception e) {
                        // If creation fails, return error with details
                        response.put("success", false);
                        response.put("message", "User not found and failed to create from OAuth2 data");
                        response.put("error", e.getMessage());
                    }
                } else {
                    response.put("success", false);
                    response.put("message", "User not found in database and missing required OAuth2 attributes");
                }
                
                // Include OAuth2 user details for debugging
                Map<String, Object> oauth2Info = new HashMap<>();
                oauth2Info.put("name", oauth2User.getName());
                oauth2Info.put("email", email);
                oauth2Info.put("attributes", oauth2User.getAttributes());
                response.put("oauth2User", oauth2Info);
                
                // Include Microsoft Graph profile if available from session or request
                MicrosoftGraphService.UserProfile microsoftProfile = null;
                if (session != null) {
                    microsoftProfile = (MicrosoftGraphService.UserProfile) session.getAttribute("microsoftProfile");
                }
                if (microsoftProfile == null) {
                    microsoftProfile = (MicrosoftGraphService.UserProfile) request.getAttribute("microsoftProfile");
                }
                
                if (microsoftProfile != null) {
                    Map<String, Object> microsoftInfo = new HashMap<>();
                    microsoftInfo.put("id", microsoftProfile.getId());
                    microsoftInfo.put("displayName", microsoftProfile.getDisplayName());
                    microsoftInfo.put("mail", microsoftProfile.getMail());
                    microsoftInfo.put("userPrincipalName", microsoftProfile.getUserPrincipalName());
                    microsoftInfo.put("jobTitle", microsoftProfile.getJobTitle());
                    response.put("microsoftProfile", microsoftInfo);
                }
            }
        } else {
            response.put("success", false);
            response.put("message", "Not authenticated");
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = oauth2User.getAttribute("email");
        if (email == null) {
            email = oauth2User.getAttribute("userPrincipalName");
        }

        Optional<User> user = userService.findByEmail(email);
        
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        User userEntity = user.get();
        Map<String, Object> userInfo = Map.of(
            "id", userEntity.getId(),
            "email", userEntity.getEmail(),
            "displayName", userEntity.getDisplayName(),
            "role", userEntity.getRole(),
            "jobTitle", userEntity.getJobTitle()
        );

        return ResponseEntity.ok(userInfo);
    }
}


