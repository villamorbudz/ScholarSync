package com.scholarsync.backend.config;

import com.scholarsync.backend.security.JwtAuthenticationFilter;
import com.scholarsync.backend.security.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                         JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false))
            .authorizeHttpRequests(auth -> auth
                // Public static resources
                .requestMatchers("/", "/index.html", "/login", "/login.html", "/static/**", "/error").permitAll()
                .requestMatchers("/**/*.js", "/**/*.css").permitAll()
                // OAuth2 endpoints must be permitted
                .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                // Public API endpoints
                .requestMatchers("/api/public/**").permitAll()
                // Auth endpoints - require authentication (except test endpoint for debugging)
                .requestMatchers("/api/auth/test/**").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                // User search endpoint - require authentication
                .requestMatchers("/api/users/search").authenticated()
                // Group endpoints - require authentication
                .requestMatchers("/api/groups/**").authenticated()
                // Course endpoints - require authentication
                .requestMatchers("/api/courses/**").authenticated()
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .authorizationEndpoint(authz -> authz
                    .baseUri("/login/oauth2/authorization"))
                .redirectionEndpoint(redirect -> redirect
                    .baseUri("/login/oauth2/code/*"))
                .successHandler(oAuth2LoginSuccessHandler)
                .defaultSuccessUrl("http://localhost:5173/auth/success", true));

        // Support JWT bearer auth in addition to session-based OAuth login
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}


