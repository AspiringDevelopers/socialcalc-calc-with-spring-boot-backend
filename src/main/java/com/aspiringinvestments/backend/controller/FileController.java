package com.aspiringinvestments.backend.controller;

import com.aspiringinvestments.backend.dto.ApiResponse;
import com.aspiringinvestments.backend.dto.SaveFileRequest;
import com.aspiringinvestments.backend.entity.FileData;
import com.aspiringinvestments.backend.security.CurrentUser;
import com.aspiringinvestments.backend.service.FileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileService fileService;

    @GetMapping("/save")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listFiles(@CurrentUser UUID userId) {
        try {
            List<FileData> files = fileService.listFiles(userId, "home/");
            
            List<Map<String, Object>> fileList = files.stream()
                .map(file -> Map.of(
                    "fname", extractFileName(file.getPath()),
                    "createdAt", file.getCreatedAt(),
                    "updatedAt", file.getUpdatedAt()
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(fileList));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to list files: " + e.getMessage()));
        }
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<String>> saveFile(
            @CurrentUser UUID userId,
            @Valid @RequestBody SaveFileRequest request) {
        try {
            String filePath = "home/" + request.getFname();
            fileService.saveFile(userId, filePath, request.getData());
            
            return ResponseEntity.ok(ApiResponse.success("Done"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to save file: " + e.getMessage()));
        }
    }

    @PostMapping("/insert")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFile(
            @CurrentUser UUID userId,
            @RequestBody Map<String, String> request) {
        try {
            String filename = request.get("filename");
            String filePath = "home/" + filename;
            
            Optional<FileData> fileOpt = fileService.getFile(userId, filePath);
            if (fileOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("File not found"));
            }

            FileData file = fileOpt.get();
            Map<String, Object> responseData = Map.of(
                "data", file.getContent(),
                "result", "ok"
            );

            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to get file: " + e.getMessage()));
        }
    }

    @PostMapping("/usersheet")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleUserSheet(
            @CurrentUser UUID userId,
            @RequestBody Map<String, String> request) {
        try {
            String pagename = request.get("pagename");
            String delete = request.get("delete");
            
            if ("yes".equals(delete)) {
                String filePath = "home/" + pagename;
                fileService.deleteFile(userId, filePath);
                return ResponseEntity.ok(ApiResponse.success(Map.of("deleted", true)));
            }

            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "User sheet handled")));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to handle user sheet: " + e.getMessage()));
        }
    }

    @GetMapping("/runas")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runApp(
            @CurrentUser UUID userId,
            @RequestParam String file,
            @RequestParam(required = false) String sheets) {
        try {
            String filePath = "home/" + file;
            Optional<FileData> fileOpt = fileService.getFile(userId, filePath);
            
            if (fileOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("File not found"));
            }

            FileData fileData = fileOpt.get();
            List<String> sheetsList = sheets != null ? 
                List.of(sheets.split(",")) : 
                List.of();

            Map<String, Object> responseData = Map.of(
                "fname", file,
                "sheetstr", fileData.getContent(),
                "sheets", sheetsList
            );

            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to run app: " + e.getMessage()));
        }
    }

    private String extractFileName(String path) {
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "";
    }
}