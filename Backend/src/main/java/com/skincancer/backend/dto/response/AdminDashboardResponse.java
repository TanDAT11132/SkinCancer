package com.skincancer.backend.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AdminDashboardResponse(
        Summary summary,
        List<DailyCount> monthlyUsers,
        List<NamedCount> genderBreakdown,
        List<NamedCount> diagnosisBreakdown,
        FeedbackEffectiveness feedbackEffectiveness
) {
    public record Summary(
            long totalUsers,
            long newUsersThisMonth,
            long totalPredictions,
            long predictionsThisMonth,
            long totalFeedbacks,
            double feedbackAccuracyRate
    ) {
    }

    public record DailyCount(LocalDate date, long count) {
    }

    public record NamedCount(String label, long count) {
    }

    public record FeedbackEffectiveness(
            long totalFeedbacks,
            long correctFeedbacks,
            long incorrectFeedbacks,
            long unansweredFeedbacks,
            long retrainReadyFeedbacks,
            double accuracyRate
    ) {
    }
}
