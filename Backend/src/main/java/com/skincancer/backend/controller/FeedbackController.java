package com.skincancer.backend.controller;

import com.skincancer.backend.dto.request.CreateFeedbackRequest;
import com.skincancer.backend.dto.response.FeedbackResponse;
import com.skincancer.backend.dto.response.RetrainSampleResponse;
import com.skincancer.backend.security.UserPrincipal;
import com.skincancer.backend.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/{predictionId}/feedback")
    public FeedbackResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID predictionId,
            @Valid @RequestBody CreateFeedbackRequest request
    ) {
        return feedbackService.create(principal, predictionId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/retrain-samples")
    public List<RetrainSampleResponse> retrainSamples(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return feedbackService.retrainSamples(page, size);
    }
}
