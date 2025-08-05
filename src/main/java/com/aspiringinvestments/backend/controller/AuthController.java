package com.aspiringinvestments.backend.controller;

import com.aspiringinvestments.backend.dto.ApiResponse;
import com.aspiringinvestments.backend.dto.LoginRequest;
import com.aspiringinvestments.backend.dto.RegisterRequest;
import com.aspiringinvestments.backend.entity.User;
import com.aspiringinvestments.backend.security.JwtTokenProvider;
import com.aspiringinvestments.backend.service.UserService;
import com.aspiringinvestments.backend.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            boolean authenticated = userService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());
            
            if (!authenticated) {
                return ResponseEntity.ok(ApiResponse.error("Invalid credentials"));
            }

            Optional<User> userOpt = userService.findByEmail(loginRequest.getEmail());
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("User not found"));
            }

            User user = userOpt.get();
            String token = jwtTokenProvider.createToken(user.getId().toString(), user.getEmail());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail()
            ));

            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            if (userService.userExists(registerRequest.getEmail())) {
                return ResponseEntity.ok(ApiResponse.error("User already exists"));
            }

            User user = userService.createUser(registerRequest.getEmail(), registerRequest.getPassword());
            String token = jwtTokenProvider.createToken(user.getId().toString(), user.getEmail());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail()
            ));

            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            
            if (!userService.userExists(email)) {
                return ResponseEntity.ok(ApiResponse.error("User not found"));
            }

            String dongle = userService.generatePasswordResetDongle(email);
            String resetLink = "http://localhost:8080/reset-password?email=" + email + "&token=" + dongle;
            
            String message = String.format(
                "Please click the following link to reset your password:\n%s",
                resetLink
            );

            emailService.sendEmail(email, "Password Reset", message);

            return ResponseEntity.ok(ApiResponse.success("Password reset email sent"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to send reset email: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String token = request.get("token");
            String newPassword = request.get("password");

            if (!userService.validatePasswordResetDongle(email, token)) {
                return ResponseEntity.ok(ApiResponse.error("Invalid reset link"));
            }

            userService.resetPassword(email, token, newPassword);

            return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Password reset failed: " + e.getMessage()));
        }
    }
}