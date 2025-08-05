package com.aspiringinvestments.backend.controller;

import com.aspiringinvestments.backend.dto.ApiResponse;
import com.aspiringinvestments.backend.dto.WebAppRequest;
import com.aspiringinvestments.backend.entity.FileData;
import com.aspiringinvestments.backend.entity.InAppPurchase;
import com.aspiringinvestments.backend.security.CurrentUser;
import com.aspiringinvestments.backend.service.FileService;
import com.aspiringinvestments.backend.service.InAppPurchaseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/webapp")
@CrossOrigin(origins = "*")
public class WebAppController {

    @Autowired
    private FileService fileService;

    @Autowired
    private InAppPurchaseService inAppPurchaseService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleWebAppGet(
            @CurrentUser UUID userId,
            @RequestParam String action,
            @RequestParam(required = false) String appname) {
        try {
            switch (action) {
                case "login":
                    return ResponseEntity.ok(ApiResponse.success(Map.of("status", "ok")));
                    
                case "getInapp":
                    if (appname == null || appname.isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.error("App name is required"));
                    }
                    
                    InAppPurchase purchase = inAppPurchaseService.getOrCreatePurchase(userId, appname);
                    Map<String, Object> inAppData = Map.of(
                        "owned", purchase.getOwned(),
                        "consumed", purchase.getConsumed()
                    );
                    return ResponseEntity.ok(ApiResponse.success(inAppData));
                    
                default:
                    return ResponseEntity.ok(ApiResponse.error("Unknown action"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to handle webapp request: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleWebAppPost(
            @CurrentUser UUID userId,
            @Valid @RequestBody WebAppRequest request) {
        try {
            switch (request.getAction()) {
                case "savefile":
                    return handleSaveFile(userId, request);
                case "savecurrentfile":
                    return handleSaveCurrentFile(userId, request);
                case "getfile":
                    return handleGetFile(userId, request);
                case "deletefile":
                    return handleDeleteFile(userId, request);
                case "listdir":
                    return handleListDir(userId, request);
                case "update":
                    return handleUpdate(userId, request);
                case "purchase":
                    return handlePurchase(userId, request);
                default:
                    return ResponseEntity.ok(ApiResponse.error("Unknown action"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to handle webapp request: " + e.getMessage()));
        }
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> handleSaveFile(UUID userId, WebAppRequest request) {
        String appName = request.getAppname();
        String fname = request.getFname();
        String data = request.getData();

        if (!inAppPurchaseService.hasRemainingUses(userId, appName)) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("result", "buy")));
        }

        String filePath = fileService.getSecureStorePath(appName, fname);
        fileService.saveFile(userId, filePath, data);

        return ResponseEntity.ok(ApiResponse.success(Map.of("result", "ok")));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> handleSaveCurrentFile(UUID userId, WebAppRequest request) {
        String appName = request.getAppname();
        String fname = request.getFname();
        String data = request.getData();
        String filePath = fileService.getSecureStorePath(appName, fname);

        Optional<FileData> fileOpt = fileService.getFile(userId, filePath);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("File not found"));
        }

        fileService.saveFile(userId, filePath, data);
        return ResponseEntity.ok(ApiResponse.success(Map.of("result", "ok")));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> handleGetFile(UUID userId, WebAppRequest request) {
        String appName = request.getAppname();
        String fname = request.getFname();
        String filePath = fileService.getSecureStorePath(appName, fname);

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
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> handleDeleteFile(UUID userId, WebAppRequest request) {
        String appName = request.getAppname();
        String fname = request.getFname();
        String filePath = fileService.getSecureStorePath(appName, fname);

        fileService.deleteFile(userId, filePath);
        return ResponseEntity.ok(ApiResponse.success(Map.of("result", "ok")));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> handleListDir(UUID userId, WebAppRequest request) {
        String appName = request.getAppname();
        String dirPath = fileService.getSecureStoreDirPath(appName);

        List<FileData> files = fileService.listFiles(userId, dirPath);
        List<String> fileNames = files.stream()
            .map(file -> extractFileName(file.getPath()))
            .collect(Collectors.toList());

        Map<String, Object> responseData = Map.of(
            "data", fileNames,
            "result", "ok"
        );

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> handleUpdate(UUID userId, WebAppRequest request) {
        String appName = request.getAppname();
        inAppPurchaseService.consumeOne(userId, appName);
        return ResponseEntity.ok(ApiResponse.success(Map.of("result", "ok")));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> handlePurchase(UUID userId, WebAppRequest request) {
        String appName = request.getAppname();
        inAppPurchaseService.addPurchase(userId, appName, 10);
        return ResponseEntity.ok(ApiResponse.success(Map.of("result", "ok")));
    }

    private String extractFileName(String path) {
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "";
    }
}