package com.skincancer.backend.service;

import com.skincancer.backend.dto.response.AdminDashboardResponse;
import com.skincancer.backend.dto.response.AdminUserPredictionResponse;
import com.skincancer.backend.dto.response.AdminUserSummaryResponse;
import com.skincancer.backend.dto.response.FeedbackResponse;
import com.skincancer.backend.entity.Feedback;
import com.skincancer.backend.entity.Prediction;
import com.skincancer.backend.entity.UserEntity;
import com.skincancer.backend.exception.NotFoundException;
import com.skincancer.backend.repository.FeedbackRepository;
import com.skincancer.backend.repository.ImageUploadRepository;
import com.skincancer.backend.repository.PredictionRepository;
import com.skincancer.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PredictionRepository predictionRepository;
    private final FeedbackRepository feedbackRepository;
    private final ImageUploadRepository imageUploadRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime nextMonthStart = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<UserEntity> users = userRepository.findAll();
        List<Prediction> predictions = predictionRepository.findAll();
        List<Feedback> feedbacks = feedbackRepository.findAll();

        long newUsersThisMonth = users.stream()
                .filter(user -> isWithinMonth(user.getCreatedAt(), monthStart, nextMonthStart))
                .count();
        long predictionsThisMonth = predictions.stream()
                .filter(prediction -> isWithinMonth(prediction.getRequestedAt(), monthStart, nextMonthStart))
                .count();

        AdminDashboardResponse.Summary summary = new AdminDashboardResponse.Summary(
                users.size(),
                newUsersThisMonth,
                predictions.size(),
                predictionsThisMonth,
                feedbacks.size(),
                roundRate(calculateAccuracyRate(feedbacks))
        );

        List<AdminDashboardResponse.DailyCount> monthlyUsers = buildMonthlyUsers(users, currentMonth);
        List<AdminDashboardResponse.NamedCount> genderBreakdown = buildNamedCounts(
                users,
                user -> normalizeLabel(user.getGender(), "Unknown")
        );
        List<AdminDashboardResponse.NamedCount> diagnosisBreakdown = buildNamedCounts(
                predictions,
                prediction -> normalizeLabel(prediction.getPredictedClass(), "Unknown")
        );
        AdminDashboardResponse.FeedbackEffectiveness feedbackEffectiveness = buildFeedbackEffectiveness(feedbacks);

        log.info("[FLOW][ADMIN] Dashboard loaded users={} predictions={} feedbacks={}",
                users.size(), predictions.size(), feedbacks.size());
        return new AdminDashboardResponse(
                summary,
                monthlyUsers,
                genderBreakdown,
                diagnosisBreakdown,
                feedbackEffectiveness
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserSummaryResponse> getUsers() {
        List<UserEntity> users = userRepository.findAll();
        List<Prediction> predictions = predictionRepository.findAll();
        List<Feedback> feedbacks = feedbackRepository.findAll();

        Map<UUID, Long> predictionCounts = predictions.stream()
                .collect(Collectors.groupingBy(
                        prediction -> prediction.getImage().getUser().getUserId(),
                        Collectors.counting()
                ));
        Map<UUID, Long> feedbackCounts = feedbacks.stream()
                .collect(Collectors.groupingBy(
                        feedback -> feedback.getUser().getUserId(),
                        Collectors.counting()
                ));

        return users.stream()
                .sorted(Comparator.comparing(UserEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(user -> new AdminUserSummaryResponse(
                        user.getUserId(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getGender(),
                        user.getAge(),
                        user.getRole(),
                        user.getCreatedAt(),
                        predictionCounts.getOrDefault(user.getUserId(), 0L),
                        feedbackCounts.getOrDefault(user.getUserId(), 0L)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminUserPredictionResponse> getUserPredictions(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        List<Prediction> predictions = predictionRepository.findByImageUserUserIdOrderByRequestedAtDesc(user.getUserId());
        List<UUID> predictionIds = predictions.stream()
                .map(Prediction::getPredictionId)
                .toList();

        Map<UUID, Feedback> latestFeedbackByPredictionId = predictionIds.isEmpty()
                ? new HashMap<>()
                : feedbackRepository.findByPredictionPredictionIdIn(predictionIds)
                .stream()
                .collect(Collectors.toMap(
                        feedback -> feedback.getPrediction().getPredictionId(),
                        Function.identity(),
                        (left, right) -> left.getCreatedAt().isAfter(right.getCreatedAt()) ? left : right
                ));

        return predictions.stream()
                .map(prediction -> {
                    Feedback feedback = latestFeedbackByPredictionId.get(prediction.getPredictionId());
                    return new AdminUserPredictionResponse(
                            prediction.getPredictionId(),
                            prediction.getImage().getImageId(),
                            "/api/admin/images/" + prediction.getImage().getImageId(),
                            prediction.getPredictedClass(),
                            prediction.getProbability(),
                            prediction.getTopKJson(),
                            prediction.getRawResponseJson(),
                            prediction.getModelName(),
                            prediction.getModelVersion(),
                            prediction.getRequestedAt(),
                            feedback == null ? null : feedback.getIsCorrect(),
                            feedback == null ? null : feedback.getUserLabel(),
                            feedback == null ? null : feedback.getComment(),
                            feedback != null && feedback.isAllowForRetrain(),
                            feedback == null ? null : feedback.getCreatedAt()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Path getImagePath(UUID imageId) {
        Path path = imageUploadRepository.findById(imageId)
                .map(image -> Paths.get(image.getFileUri()))
                .orElseThrow(() -> new NotFoundException("IMAGE_NOT_FOUND", "Image not found"));
        if (!Files.exists(path)) {
            throw new NotFoundException("IMAGE_FILE_NOT_FOUND", "Image file not found");
        }
        return path;
    }

    @Transactional
    public FeedbackResponse updateLatestFeedback(UUID predictionId, Boolean allowForRetrain) {
        Feedback feedback = feedbackRepository.findFirstByPredictionPredictionIdOrderByCreatedAtDesc(predictionId)
                .orElseThrow(() -> new NotFoundException("FEEDBACK_NOT_FOUND", "Feedback not found for prediction"));

        feedback.setAllowForRetrain(Boolean.TRUE.equals(allowForRetrain));
        feedback = feedbackRepository.save(feedback);

        return new FeedbackResponse(
                feedback.getFeedbackId(),
                feedback.getPrediction().getPredictionId(),
                feedback.getUser().getUserId(),
                feedback.getIsCorrect(),
                feedback.getUserLabel(),
                feedback.getComment(),
                feedback.isAllowForRetrain(),
                feedback.getCreatedAt()
        );
    }

    private List<AdminDashboardResponse.DailyCount> buildMonthlyUsers(List<UserEntity> users, YearMonth currentMonth) {
        Map<LocalDate, Long> byDay = users.stream()
                .filter(user -> user.getCreatedAt() != null)
                .filter(user -> YearMonth.from(user.getCreatedAt().toLocalDate()).equals(currentMonth))
                .collect(Collectors.groupingBy(
                        user -> user.getCreatedAt().toLocalDate(),
                        TreeMap::new,
                        Collectors.counting()
                ));

        List<AdminDashboardResponse.DailyCount> items = new ArrayList<>();
        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate date = currentMonth.atDay(day);
            items.add(new AdminDashboardResponse.DailyCount(date, byDay.getOrDefault(date, 0L)));
        }
        return items;
    }

    private <T> List<AdminDashboardResponse.NamedCount> buildNamedCounts(List<T> items, Function<T, String> classifier) {
        return items.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new AdminDashboardResponse.NamedCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private AdminDashboardResponse.FeedbackEffectiveness buildFeedbackEffectiveness(List<Feedback> feedbacks) {
        long correct = feedbacks.stream()
                .filter(feedback -> Boolean.TRUE.equals(feedback.getIsCorrect()))
                .count();
        long incorrect = feedbacks.stream()
                .filter(feedback -> Boolean.FALSE.equals(feedback.getIsCorrect()))
                .count();
        long unanswered = feedbacks.stream()
                .filter(feedback -> feedback.getIsCorrect() == null)
                .count();
        long retrainReady = feedbacks.stream()
                .filter(Feedback::isAllowForRetrain)
                .count();

        return new AdminDashboardResponse.FeedbackEffectiveness(
                feedbacks.size(),
                correct,
                incorrect,
                unanswered,
                retrainReady,
                roundRate(calculateAccuracyRate(feedbacks))
        );
    }

    private boolean isWithinMonth(LocalDateTime value, LocalDateTime start, LocalDateTime endExclusive) {
        return value != null && !value.isBefore(start) && value.isBefore(endExclusive);
    }

    private double calculateAccuracyRate(List<Feedback> feedbacks) {
        long answered = feedbacks.stream()
                .filter(feedback -> feedback.getIsCorrect() != null)
                .count();
        if (answered == 0) {
            return 0.0;
        }
        long correct = feedbacks.stream()
                .filter(feedback -> Boolean.TRUE.equals(feedback.getIsCorrect()))
                .count();
        return (correct * 100.0) / answered;
    }

    private double roundRate(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String normalizeLabel(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "male" -> "Male";
            case "female" -> "Female";
            case "other" -> "Other";
            default -> raw.trim();
        };
    }
}
