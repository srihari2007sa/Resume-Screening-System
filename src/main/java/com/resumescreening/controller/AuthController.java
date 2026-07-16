package com.resumescreening.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if ("admin".equals(username) && "admin123".equals(password)) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "token", "kit-ai-session-token-jwt-placeholder",
                "role", "Recruiter Admin",
                "username", username
            ));
        } else {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Invalid username or password. Use 'admin' and 'admin123'."
            ));
        }
    }
}
