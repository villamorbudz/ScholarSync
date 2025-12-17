package com.scholarsync.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // Disable CSRF so POST/PUT/DELETE work in demo
            .csrf(csrf -> csrf.disable())

            // Allow H2 Console iframe
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )

            // Allow ALL API endpoints for now
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/**",
                    "/h2-console/**",
                    "/adviser-test.html"
                ).permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
