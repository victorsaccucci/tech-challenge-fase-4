package com.fiap.FeedbackServerlessApp.services;

import com.fiap.FeedbackServerlessApp.entities.Feedback;
import com.fiap.FeedbackServerlessApp.entities.UrgencyLevel;
import com.fiap.FeedbackServerlessApp.repositories.FeedbackRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FeedbackService {

    private final FeedbackRepository repository;

    public FeedbackService(FeedbackRepository repository) {
        this.repository = repository;
    }

    public Feedback create(String description, int score) {

        UrgencyLevel urgency = calculateUrgency(score);

        Feedback feedback = new Feedback();
        feedback.setDescription(description);
        feedback.setScore(score);
        feedback.setUrgencyLevel(urgency);
        feedback.setCreatedAt(LocalDateTime.now());

        return repository.save(feedback);
    }

    private UrgencyLevel calculateUrgency(int score) {
        if (score <= 3) return UrgencyLevel.HIGH;
        if (score <= 6) return UrgencyLevel.MEDIUM;
        return UrgencyLevel.LOW;
    }
}
