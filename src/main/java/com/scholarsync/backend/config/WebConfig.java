package com.scholarsync.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Only handle /static/** as static resources
        // OAuth2 endpoints (/login/oauth2/**) are handled by Spring Security filters
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}

