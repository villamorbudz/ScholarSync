package com.scholarsync.backend.model;

/**
 * Tracks the authentication method used for LOGIN in the current session.
 * 
 * This enum will be used in JWT tokens/session to track how the user authenticated.
 * It is NOT stored in the database - it's a session-level attribute.
 * 
 * - OAUTH_ONLY: User logged in via Microsoft OAuth (most common)
 * - LOCAL_ONLY: User logged in via local password (fallback when MS OAuth is down)
 * - HYBRID: Reserved for future use if needed
 * 
 * Note: Registration is ONLY possible via Microsoft OAuth.
 * Local password is generated during OAuth registration for emergency fallback.
 * 
 * TODO: Include this in JWT token claims when JWT session handling is implemented.
 */
public enum AuthenticationMethod {
    OAUTH_ONLY,    // User logged in via OAuth (stored in JWT/session)
    LOCAL_ONLY,    // User logged in via local password (stored in JWT/session)
    HYBRID         // Reserved for future use
}


