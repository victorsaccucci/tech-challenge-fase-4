package com.fiap.FeedbackServerlessApp.services;

import com.fiap.FeedbackServerlessApp.entities.UrgencyLevel;
import com.fiap.FeedbackServerlessApp.entities.WeeklyReport;
import com.fiap.FeedbackServerlessApp.repositories.FeedbackRepository;
import com.fiap.FeedbackServerlessApp.repositories.WeeklyReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WeeklyReportService {

    private final FeedbackRepository feedbackRepository;
    private final WeeklyReportRepository weeklyReportRepository;

    public WeeklyReportService(FeedbackRepository feedbackRepository, 
                               WeeklyReportRepository weeklyReportRepository) {
        this.feedbackRepository = feedbackRepository;
        this.weeklyReportRepository = weeklyReportRepository;
    }

    public WeeklyReport generateWeeklyReport() {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(7);

        Double averageScore = feedbackRepository.calculateAverageScore(startDate, endDate);
        long totalFeedbacks = feedbackRepository.findByCreatedAtBetween(startDate, endDate).size();
        long highUrgency = feedbackRepository.countByUrgencyLevelAndCreatedAtBetween(UrgencyLevel.HIGH, startDate, endDate);
        long mediumUrgency = feedbackRepository.countByUrgencyLevelAndCreatedAtBetween(UrgencyLevel.MEDIUM, startDate, endDate);
        long lowUrgency = feedbackRepository.countByUrgencyLevelAndCreatedAtBetween(UrgencyLevel.LOW, startDate, endDate);

        WeeklyReport report = WeeklyReport.builder()
                .averageScore(averageScore != null ? averageScore : 0.0)
                .totalFeedbacks(totalFeedbacks)
                .highUrgencyCount(highUrgency)
                .mediumUrgencyCount(mediumUrgency)
                .lowUrgencyCount(lowUrgency)
                .generatedAt(LocalDateTime.now())
                .build();

        return weeklyReportRepository.save(report);
    }
}
