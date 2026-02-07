package com.fiap.FeedbackServerlessApp.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double averageScore;

    @Column(nullable = false)
    private Long totalFeedbacks;

    @Column(nullable = false)
    private Long highUrgencyCount;

    @Column(nullable = false)
    private Long mediumUrgencyCount;

    @Column(nullable = false)
    private Long lowUrgencyCount;

    @Column(nullable = false)
    private LocalDateTime generatedAt;
}
