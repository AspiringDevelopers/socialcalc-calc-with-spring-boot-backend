package com.aspiringinvestments.backend.service;

import com.aspiringinvestments.backend.entity.User;
import com.aspiringinvestments.backend.repository.UserRepository;
import com.aspiringinvestments.backend.util.RandomStringGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RandomStringGenerator randomStringGenerator;

    public User createUser(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User already exists");
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(email, hashedPassword);
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean authenticate(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    public void updatePassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);
    }

    public void setUserDongle(UUID userId, String dongle) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setDongle(dongle);
        userRepository.save(user);
    }

    public String generatePasswordResetDongle(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String dongle = randomStringGenerator.generate(20);
        user.setDongle(dongle);
        userRepository.save(user);
        
        return dongle;
    }

    public boolean validatePasswordResetDongle(String email, String dongle) {
        Optional<User> userOpt = userRepository.findByEmailAndDongle(email, dongle);
        return userOpt.isPresent();
    }

    public void resetPassword(String email, String dongle, String newPassword) {
        User user = userRepository.findByEmailAndDongle(email, dongle)
                .orElseThrow(() -> new RuntimeException("Invalid reset link"));
        
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashedPassword);
        user.setDongle(null); // Clear the dongle after use
        userRepository.save(user);
    }

    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }
}