package com.fiap.FeedbackServerlessApp.services;

import com.fiap.FeedbackServerlessApp.entities.Feedback;
import com.fiap.FeedbackServerlessApp.entities.UrgencyLevel;
import com.fiap.FeedbackServerlessApp.repositories.FeedbackRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FeedbackService {

    private final FeedbackRepository repository;
    private final NotificationService notificationService;
    private final SQSService sqsService;

    public FeedbackService(FeedbackRepository repository, 
                          NotificationService notificationService,
                          SQSService sqsService) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.sqsService = sqsService;
    }

    public Feedback create(String description, int score) {

        UrgencyLevel urgency = calculateUrgency(score);

        Feedback feedback = new Feedback();
        feedback.setDescription(description);
        feedback.setScore(score);
        feedback.setUrgencyLevel(urgency);
        feedback.setCreatedAt(LocalDateTime.now());

        Feedback savedFeedback = repository.save(feedback);
        
        // Envia notificação se for crítico
        notificationService.sendCriticalNotification(savedFeedback);
        
        // Envia para SQS se for crítico (AWS)
        if (urgency == UrgencyLevel.HIGH) {
            sqsService.sendCriticalFeedback(savedFeedback);
        }
        
        return savedFeedback;
    }

    private UrgencyLevel calculateUrgency(int score) {
        if (score <= 3) return UrgencyLevel.HIGH;
        if (score <= 6) return UrgencyLevel.MEDIUM;
        return UrgencyLevel.LOW;
    }
}
