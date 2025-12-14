package com.scholarsync.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final String professorKey;

    public AuthController(@Value("${app.professor.key:}") String professorKey) {
        this.professorKey = professorKey == null ? "" : professorKey;
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping(path = "/api/auth/professor/check")
    public ResponseEntity<?> checkProfessor(@RequestHeader(value = "X-Professor-Key", required = false) String key) {
        if (professorKey.isEmpty()) {
            // disabled on server
            return ResponseEntity.ok().body("ok");
        }
        if (key != null && key.equals(professorKey)) {
            return ResponseEntity.ok().body("ok");
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
    }
}
