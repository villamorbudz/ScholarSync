package com.scholarsync.backend.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        Map<String, Object> errorResponse = new HashMap<>();
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            errorResponse.put("status", statusCode);
            errorResponse.put("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
            
            if (message != null) {
                errorResponse.put("message", message.toString());
            }
            
            if (exception != null) {
                errorResponse.put("exception", exception.getClass().getName());
            }
            
            // Check if it's an OAuth2 endpoint issue
            String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
            if (requestUri != null && requestUri.contains("/oauth2/")) {
                errorResponse.put("hint", "OAuth2 endpoint should be handled by Spring Security. Ensure the URL is correct: /login/oauth2/authorization/microsoft");
            }
            
            return ResponseEntity.status(statusCode).body(errorResponse);
        }
        
        errorResponse.put("status", 500);
        errorResponse.put("error", "Internal Server Error");
        return ResponseEntity.status(500).body(errorResponse);
    }
}

