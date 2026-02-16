package com.fiap.FeedbackServerlessApp.repositories;

import com.fiap.FeedbackServerlessApp.entities.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
}
