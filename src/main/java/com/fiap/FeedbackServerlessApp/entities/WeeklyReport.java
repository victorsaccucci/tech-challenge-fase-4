package com.fiap.FeedbackServerlessApp.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

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

    //element collection para persistir coleção de tipos simples (teste)
    @ElementCollection
    @CollectionTable(
            name = "weekly_report_feedbacks_by_day",
            joinColumns = @JoinColumn(name = "weekly_report_id")
    )
    @MapKeyColumn(name = "day")
    @Column(name = "count")
    private Map<String, Integer> feedbackCountByDay;

    //mesma coisa do atributo de cima
    @ElementCollection
    @CollectionTable(
            name = "weekly_report_feedbacks_by_urgency",
            joinColumns = @JoinColumn(name = "weekly_report_id")
    )
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "urgency_level")
    @Column(name = "count")
    private Map<UrgencyLevel, Integer> feedbackCountByUrgency;

    @Column(nullable = false)
    private LocalDateTime generatedAt;
}
