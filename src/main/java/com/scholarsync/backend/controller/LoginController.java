package com.scholarsync.backend.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
public class LoginController {

    @GetMapping("/login")
    public ResponseEntity<String> login() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/login.html");
        
        try (InputStream inputStream = resource.getInputStream()) {
            String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(html);
        }
    }

}

