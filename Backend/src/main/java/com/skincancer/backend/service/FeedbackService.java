package com.skincancer.backend.service;

import com.skincancer.backend.dto.request.CreateFeedbackRequest;
import com.skincancer.backend.dto.response.FeedbackResponse;
import com.skincancer.backend.dto.response.RetrainSampleResponse;
import com.skincancer.backend.entity.Feedback;
import com.skincancer.backend.entity.Prediction;
import com.skincancer.backend.entity.UserEntity;
import com.skincancer.backend.exception.NotFoundException;
import com.skincancer.backend.repository.FeedbackRepository;
import com.skincancer.backend.repository.PredictionRepository;
import com.skincancer.backend.repository.UserRepository;
import com.skincancer.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final PredictionRepository predictionRepository;
    private final UserRepository userRepository;

    @Transactional
    public FeedbackResponse create(UserPrincipal principal, UUID predictionId, CreateFeedbackRequest request) {
        log.info("[FLOW][FEEDBACK] Create feedback userId={} predictionId={}", principal.userId(), predictionId);
        Prediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new NotFoundException("PREDICTION_NOT_FOUND", "Prediction not found"));
        UserEntity user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        Feedback feedback = new Feedback();
        feedback.setPrediction(prediction);
        feedback.setUser(user);
        feedback.setIsCorrect(request.isCorrect());
        feedback.setUserLabel(request.userLabel());
        feedback.setComment(request.comment());
        feedback.setAllowForRetrain(Boolean.TRUE.equals(request.allowForRetrain()));
        feedback.setCreatedAt(LocalDateTime.now());
        feedback = feedbackRepository.save(feedback);
        log.info("[FLOW][FEEDBACK] Create feedback success feedbackId={} allowForRetrain={}", feedback.getFeedbackId(), feedback.isAllowForRetrain());

        return new FeedbackResponse(
                feedback.getFeedbackId(),
                prediction.getPredictionId(),
                user.getUserId(),
                feedback.getIsCorrect(),
                feedback.getUserLabel(),
                feedback.getComment(),
                feedback.isAllowForRetrain(),
                feedback.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<RetrainSampleResponse> retrainSamples(int page, int size) {
        List<RetrainSampleResponse> results = feedbackRepository.findByAllowForRetrainTrueOrderByCreatedAtDesc(PageRequest.of(page, size))
                .stream()
                .map(f -> new RetrainSampleResponse(
                        f.getFeedbackId(),
                        f.getPrediction().getPredictionId(),
                        f.getPrediction().getImage().getImageId(),
                        f.getPrediction().getImage().getFileUri(),
                        f.getPrediction().getPredictedClass(),
                        f.getUserLabel(),
                        f.getIsCorrect(),
                        f.getCreatedAt()
                ))
                .toList();
        log.info("[FLOW][FEEDBACK] Load retrain samples page={} size={} returned={}", page, size, results.size());
        return results;
    }
}
