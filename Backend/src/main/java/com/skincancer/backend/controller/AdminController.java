package com.skincancer.backend.controller;

import com.skincancer.backend.dto.request.AdminFeedbackUpdateRequest;
import com.skincancer.backend.dto.response.AdminDashboardResponse;
import com.skincancer.backend.dto.response.AdminUserPredictionResponse;
import com.skincancer.backend.dto.response.AdminUserSummaryResponse;
import com.skincancer.backend.dto.response.FeedbackResponse;
import com.skincancer.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/users")
    public List<AdminUserSummaryResponse> users() {
        return adminService.getUsers();
    }

    @GetMapping("/users/{userId}/predictions")
    public List<AdminUserPredictionResponse> userPredictions(@PathVariable UUID userId) {
        return adminService.getUserPredictions(userId);
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<InputStreamResource> image(@PathVariable UUID imageId) throws IOException {
        Path imagePath = adminService.getImagePath(imageId);
        MediaType mediaType = resolveMediaType(imagePath);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new InputStreamResource(Files.newInputStream(imagePath)));
    }

    @PatchMapping("/predictions/{predictionId}/feedback")
    public FeedbackResponse updateLatestFeedback(
            @PathVariable UUID predictionId,
            @RequestBody AdminFeedbackUpdateRequest request
    ) {
        return adminService.updateLatestFeedback(predictionId, request.allowForRetrain());
    }

    private MediaType resolveMediaType(Path path) throws IOException {
        String contentType = Files.probeContentType(path);
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }
}
