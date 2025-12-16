package com.scholarsync.backend.security;

import com.scholarsync.backend.model.User;
import com.scholarsync.backend.service.MicrosoftGraphService;
import com.scholarsync.backend.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final MicrosoftGraphService microsoftGraphService;
    private final UserService userService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jwt.expiration:1800000}")
    private long jwtExpirationMillis;

    public OAuth2LoginSuccessHandler(OAuth2AuthorizedClientService authorizedClientService,
                                    MicrosoftGraphService microsoftGraphService,
                                    UserService userService,
                                    JwtService jwtService) {
        this.authorizedClientService = authorizedClientService;
        this.microsoftGraphService = microsoftGraphService;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauth2Token.getPrincipal();
        String registrationId = oauth2Token.getAuthorizedClientRegistrationId();

        // Get the authorized client to access the access token
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                registrationId,
                oauth2User.getName()
        );

        if (authorizedClient == null) {
            log.error("Authorized client not found for user: {}", oauth2User.getName());
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Authentication failed", "Authorized client not found", null, oauth2User);
            return;
        }

        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        try {
            // Fetch full user profile from Microsoft Graph API including jobTitle
            MicrosoftGraphService.UserProfile userProfile = microsoftGraphService
                    .getUserProfile(accessToken)
                    .block(); // Blocking call - in production, consider async handling

            if (userProfile == null) {
                log.error("Failed to fetch user profile from Microsoft Graph API");
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Failed to fetch user profile", "User profile is null from Microsoft Graph API", null, oauth2User);
                return;
            }

            // Extract user information
            String microsoftId = userProfile.getId();
            String email = userProfile.getMail() != null ? userProfile.getMail() : userProfile.getUserPrincipalName();

            // Enforce domain restriction: only @cit.edu users may authenticate
            if (email == null || !email.toLowerCase().endsWith("@cit.edu")) {
                log.warn("Blocked login attempt for non-cit.edu email: {}", email);
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        "Access denied",
                        "Only @cit.edu accounts are allowed to access this system.",
                        userProfile, oauth2User);
                return;
            }
            String displayName = userProfile.getDisplayName();
            String institutionalId = userProfile.getJobTitle(); // Microsoft stores institutional ID in jobTitle field

            log.info("Processing OAuth authentication for user: {} (Microsoft ID: {})", email, microsoftId);

            // Unified sign up/login: Check if user exists first to determine if it's registration or login
            boolean isNewUser = !userService.findByMicrosoftId(microsoftId).isPresent();
            
            // Create new user (auto-register) or update existing user (login)
            User user = userService.createOrUpdateUserFromOAuth(
                    microsoftId,
                    email,
                    displayName,
                    institutionalId
            );

            if (isNewUser) {
                log.info("New user registered and signed in: {} (Role: {})", email, user.getRole());
            } else {
                log.info("Existing user signed in: {} (Role: {})", email, user.getRole());
            }

            // Issue JWT for stateless API access
            String jwt = jwtService.generateToken(user);
            ResponseCookie tokenCookie = ResponseCookie.from("SESSION_TOKEN", jwt)
                    .httpOnly(true)
                    .path("/")
                    .maxAge(Duration.ofMillis(jwtExpirationMillis))
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, tokenCookie.toString());

            // Store Microsoft Graph profile info and user status in session for the success endpoint
            // Using session because request attributes don't persist across redirects
            jakarta.servlet.http.HttpSession session = request.getSession();
            session.setAttribute("microsoftProfile", userProfile);
            session.setAttribute("savedUser", user);
            session.setAttribute("isNewUser", isNewUser);

            // Redirect to success endpoint or frontend
            super.onAuthenticationSuccess(request, response, authentication);

        } catch (WebClientResponseException e) {
            log.error("Error calling Microsoft Graph API: {} (Status: {})", e.getMessage(), e.getStatusCode(), e);
            log.warn("Falling back to OAuth2User attributes for user creation");
            
            // Fallback: Create user from OAuth2User attributes when Graph API fails
            try {
                MicrosoftGraphService.UserProfile partialProfile = extractPartialProfile(oauth2User);
                String microsoftId = partialProfile.getId();
                String email = partialProfile.getMail() != null ? partialProfile.getMail() : 
                              (oauth2User.getAttribute("email") != null ? oauth2User.getAttribute("email") : 
                               oauth2User.getAttribute("userPrincipalName"));
                String displayName = partialProfile.getDisplayName();
                String institutionalId = partialProfile.getJobTitle(); // Microsoft stores institutional ID in jobTitle field
                
                if (microsoftId != null && email != null) {
                    boolean isNewUser = !userService.findByMicrosoftId(microsoftId).isPresent();
                    
                    User user = userService.createOrUpdateUserFromOAuth(
                            microsoftId,
                            email,
                            displayName,
                            institutionalId
                    );
                    
                    if (isNewUser) {
                        log.info("New user registered via fallback: {} (Role: {})", email, user.getRole());
                    } else {
                        log.info("Existing user signed in via fallback: {} (Role: {})", email, user.getRole());
                    }
                    
                    // Store in session for success endpoint
                    jakarta.servlet.http.HttpSession session = request.getSession();
                    session.setAttribute("microsoftProfile", partialProfile);
                    session.setAttribute("savedUser", user);
                    session.setAttribute("isNewUser", isNewUser);
                    
                    // Redirect to success endpoint
                    super.onAuthenticationSuccess(request, response, authentication);
                    return;
                }
            } catch (Exception fallbackException) {
                log.error("Fallback user creation also failed: {}", fallbackException.getMessage(), fallbackException);
            }
            
            // If fallback also fails, send error response
            MicrosoftGraphService.UserProfile partialProfile = extractPartialProfile(oauth2User);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Failed to fetch user information", 
                "Microsoft Graph API error: " + e.getMessage() + " (Status: " + e.getStatusCode() + ")",
                partialProfile, oauth2User);
        } catch (Exception e) {
            log.error("Unexpected error during OAuth login: {}", e.getMessage(), e);
            
            // Try fallback before sending error
            try {
                MicrosoftGraphService.UserProfile partialProfile = extractPartialProfile(oauth2User);
                String microsoftId = partialProfile.getId();
                String email = partialProfile.getMail() != null ? partialProfile.getMail() : 
                              (oauth2User.getAttribute("email") != null ? oauth2User.getAttribute("email") : 
                               oauth2User.getAttribute("userPrincipalName"));
                String displayName = partialProfile.getDisplayName();
                String institutionalId = partialProfile.getJobTitle(); // Microsoft stores institutional ID in jobTitle field
                
                if (microsoftId != null && email != null) {
                    boolean isNewUser = !userService.findByMicrosoftId(microsoftId).isPresent();
                    
                    User user = userService.createOrUpdateUserFromOAuth(
                            microsoftId,
                            email,
                            displayName,
                            institutionalId
                    );
                    
                    jakarta.servlet.http.HttpSession session = request.getSession();
                    session.setAttribute("microsoftProfile", partialProfile);
                    session.setAttribute("savedUser", user);
                    session.setAttribute("isNewUser", isNewUser);
                    
                    super.onAuthenticationSuccess(request, response, authentication);
                    return;
                }
            } catch (Exception fallbackException) {
                log.error("Fallback user creation failed: {}", fallbackException.getMessage(), fallbackException);
            }
            
            MicrosoftGraphService.UserProfile partialProfile = extractPartialProfile(oauth2User);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Authentication failed", 
                "Unexpected error: " + e.getMessage(),
                partialProfile, oauth2User);
        }
    }

    /**
     * Sends a JSON error response with Microsoft Graph API details if available
     */
    private void sendErrorResponse(
            HttpServletResponse response,
            int statusCode,
            String message,
            String details,
            MicrosoftGraphService.UserProfile microsoftProfile,
            OAuth2User oauth2User) throws IOException {
        
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("details", details);
        errorResponse.put("statusCode", statusCode);

        // Add Microsoft Graph profile if available
        if (microsoftProfile != null) {
            Map<String, Object> profileInfo = new HashMap<>();
            profileInfo.put("id", microsoftProfile.getId());
            profileInfo.put("displayName", microsoftProfile.getDisplayName());
            profileInfo.put("mail", microsoftProfile.getMail());
            profileInfo.put("userPrincipalName", microsoftProfile.getUserPrincipalName());
            profileInfo.put("jobTitle", microsoftProfile.getJobTitle());
            errorResponse.put("microsoftProfile", profileInfo);
        }

        // Add OAuth2User attributes if available (fallback info)
        if (oauth2User != null && microsoftProfile == null) {
            Map<String, Object> oauthInfo = new HashMap<>();
            oauthInfo.put("name", oauth2User.getName());
            oauthInfo.put("attributes", oauth2User.getAttributes());
            errorResponse.put("oauth2User", oauthInfo);
        }

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    /**
     * Extracts partial profile information from OAuth2User when Graph API call fails
     */
    private MicrosoftGraphService.UserProfile extractPartialProfile(OAuth2User oauth2User) {
        if (oauth2User == null) {
            return null;
        }

        MicrosoftGraphService.UserProfile partialProfile = new MicrosoftGraphService.UserProfile();
        partialProfile.setId(oauth2User.getAttribute("oid")); // Microsoft Object ID
        partialProfile.setDisplayName(oauth2User.getAttribute("name"));
        partialProfile.setMail(oauth2User.getAttribute("email"));
        partialProfile.setUserPrincipalName(oauth2User.getAttribute("userPrincipalName"));
        
        // Extract institutional ID from given_name (e.g., "22-0369-330 Giles Anthony" -> "22-0369-330")
        // OAuth2User doesn't have jobTitle, but institutional ID is in given_name
        String givenName = oauth2User.getAttribute("given_name");
        String institutionalId = null;
        if (givenName != null && !givenName.isEmpty()) {
            // Extract the institutional ID pattern from given_name
            // Pattern: "22-0369-330 Giles Anthony" or "2010-12345 Name" or "1643 Name"
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "^(\\d{2}-\\d{4}-\\d{3}|\\d{4}-\\d{5}|\\d{1,4})\\b"
            );
            java.util.regex.Matcher matcher = pattern.matcher(givenName);
            if (matcher.find()) {
                institutionalId = matcher.group(1);
            }
        }
        
        // Fallback: try jobTitle attribute if given_name extraction failed
        if (institutionalId == null) {
            institutionalId = oauth2User.getAttribute("jobTitle");
        }
        
        partialProfile.setJobTitle(institutionalId); // Microsoft Graph API uses jobTitle field

        return partialProfile;
    }
}

