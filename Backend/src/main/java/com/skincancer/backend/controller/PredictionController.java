package com.skincancer.backend.controller;

import com.skincancer.backend.dto.response.PredictionBatchResponse;
import com.skincancer.backend.dto.response.PredictionItemResponse;
import com.skincancer.backend.security.UserPrincipal;
import com.skincancer.backend.service.PredictionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @PostMapping(value = "/check", consumes = "multipart/form-data")
    public PredictionBatchResponse check(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(name = "top_k", required = false) Integer topK,
            @RequestParam(name = "client_app", required = false) String clientApp,
            HttpServletRequest request
    ) {
        return predictionService.predict(principal, files, topK, clientApp, request.getRemoteAddr());
    }

    @GetMapping("/history")
    public List<PredictionItemResponse> history(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return predictionService.history(principal, page, size);
    }
}
