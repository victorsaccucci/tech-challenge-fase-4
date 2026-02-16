package com.fiap.FeedbackServerlessApp.services;

import com.fiap.FeedbackServerlessApp.entities.Feedback;
import com.fiap.FeedbackServerlessApp.entities.Notification;
import com.fiap.FeedbackServerlessApp.entities.UrgencyLevel;
import com.fiap.FeedbackServerlessApp.repositories.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification sendCriticalNotification(Feedback feedback) {
        if (feedback.getUrgencyLevel() != UrgencyLevel.HIGH) {
            return null;
        }

        Notification notification = Notification.builder()
                .description(feedback.getDescription())
                .urgencyLevel(feedback.getUrgencyLevel())
                .sentAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }
}
