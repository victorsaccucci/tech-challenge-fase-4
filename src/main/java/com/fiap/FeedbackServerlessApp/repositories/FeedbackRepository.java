package com.fiap.FeedbackServerlessApp.repositories;

import com.fiap.FeedbackServerlessApp.entities.Feedback;
import com.fiap.FeedbackServerlessApp.entities.UrgencyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    
    List<Feedback> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    long countByUrgencyLevelAndCreatedAtBetween(UrgencyLevel urgencyLevel, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT AVG(f.score) FROM Feedback f WHERE f.createdAt BETWEEN :start AND :end")
    Double calculateAverageScore(LocalDateTime start, LocalDateTime end);
}
