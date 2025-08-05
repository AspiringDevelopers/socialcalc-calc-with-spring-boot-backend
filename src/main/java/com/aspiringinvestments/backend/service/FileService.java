package com.aspiringinvestments.backend.service;

import com.aspiringinvestments.backend.entity.FileData;
import com.aspiringinvestments.backend.entity.User;
import com.aspiringinvestments.backend.repository.FileDataRepository;
import com.aspiringinvestments.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {

    @Autowired
    private FileDataRepository fileDataRepository;

    @Autowired
    private UserRepository userRepository;

    public FileData createFile(UUID userId, String path, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FileData fileData = new FileData(user, path, content);
        return fileDataRepository.save(fileData);
    }

    public Optional<FileData> getFile(UUID userId, String path) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return fileDataRepository.findByUserAndPath(user, path);
    }

    @Transactional
    public FileData saveFile(UUID userId, String path, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<FileData> existingFile = fileDataRepository.findByUserAndPath(user, path);
        
        if (existingFile.isPresent()) {
            FileData file = existingFile.get();
            file.setContent(content);
            return fileDataRepository.save(file);
        } else {
            FileData newFile = new FileData(user, path, content);
            return fileDataRepository.save(newFile);
        }
    }

    @Transactional
    public void deleteFile(UUID userId, String path) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        fileDataRepository.deleteByUserAndPath(user, path);
    }

    public List<FileData> listFiles(UUID userId, String pathPrefix) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return fileDataRepository.findByUserAndPathStartingWith(user, pathPrefix);
    }

    public List<FileData> listAllUserFiles(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return fileDataRepository.findByUserOrderByUpdatedAtDesc(user);
    }

    public boolean fileExists(UUID userId, String path) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return fileDataRepository.existsByUserAndPath(user, path);
    }

    public String getFilePath(String... pathComponents) {
        return String.join("/", pathComponents);
    }

    public String getSecureStorePath(String appName, String fileName) {
        return getFilePath("home", "securestore", appName, fileName);
    }

    public String getSecureStoreDirPath(String appName) {
        return getFilePath("home", "securestore", appName) + "/";
    }
}