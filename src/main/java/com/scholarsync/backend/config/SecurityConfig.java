package com.scholarsync.backend.config;

import com.scholarsync.backend.security.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

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
                .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll() // OAuth2 endpoints must be permitted
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .authorizationEndpoint(authz -> authz
                    .baseUri("/login/oauth2/authorization"))
                .redirectionEndpoint(redirect -> redirect
                    .baseUri("/login/oauth2/code/*"))
                .successHandler(oAuth2LoginSuccessHandler)
                .defaultSuccessUrl("/api/auth/success", true));

        return http.build();
    }
}


