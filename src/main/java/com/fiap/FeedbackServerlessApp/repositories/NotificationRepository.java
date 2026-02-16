package com.fiap.FeedbackServerlessApp.repositories;

import com.fiap.FeedbackServerlessApp.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
