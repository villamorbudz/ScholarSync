package com.scholarsync.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class MicrosoftGraphService {

    private final WebClient webClient;
    private static final String GRAPH_API_BASE_URL = "https://graph.microsoft.com/v1.0";

    public MicrosoftGraphService() {
        this.webClient = WebClient.builder()
                .baseUrl(GRAPH_API_BASE_URL)
                .build();
    }

    /**
     * Fetches user profile from Microsoft Graph API including jobTitle
     * 
     * Note: Microsoft stores institutional IDs in the jobTitle field.
     * This is mapped to institutionalId in our User entity.
     * 
     * @param accessToken OAuth access token
     * @return UserProfile with id, displayName, mail, userPrincipalName, and jobTitle (institutional ID)
     */
    public Mono<UserProfile> getUserProfile(String accessToken) {
        return webClient.get()
                .uri("/me?$select=id,displayName,mail,userPrincipalName,jobTitle")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(UserProfile.class);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserProfile {
        @JsonProperty("id")
        private String id;

        @JsonProperty("displayName")
        private String displayName;

        @JsonProperty("mail")
        private String mail;

        @JsonProperty("userPrincipalName")
        private String userPrincipalName;

        /**
         * Microsoft Graph API field name: "jobTitle"
         * Content: Institutional ID (e.g., "22-1234-567", "1-1234")
         * Mapped to: User.institutionalId in our database
         */
        @JsonProperty("jobTitle")
        private String jobTitle;
    }
}

