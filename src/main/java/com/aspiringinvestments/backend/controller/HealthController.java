package com.aspiringinvestments.backend.controller;

import com.aspiringinvestments.backend.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> healthData = Map.of(
            "status", "UP",
            "service", "Aspiring Investments Backend",
            "version", "1.0.0",
            "timestamp", LocalDateTime.now()
        );
        
        return ResponseEntity.ok(ApiResponse.success(healthData));
    }

    @GetMapping("/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> home() {
        Map<String, Object> homeData = Map.of(
            "message", "Aspiring Investments API",
            "version", "1.0.0",
            "status", "running"
        );
        
        return ResponseEntity.ok(ApiResponse.success(homeData));
    }
}