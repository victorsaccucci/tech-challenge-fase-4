package com.fiap.FeedbackServerlessApp.repositories;

import com.fiap.FeedbackServerlessApp.entities.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
