package com.fiap.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WeeklyReportHandler implements RequestHandler<ScheduledEvent, String> {

    private final AmazonSNS snsClient = AmazonSNSClientBuilder.defaultClient();
    private final String SNS_TOPIC_ARN = System.getenv("SNS_TOPIC_ARN");
    private final String DB_URL = System.getenv("DB_URL");
    private final String DB_USER = System.getenv("DB_USER");
    private final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    @Override
    public String handleRequest(ScheduledEvent event, Context context) {
        try {
            ReportData data = generateReport();
            String emailBody = formatReportEmail(data);

            PublishRequest publishRequest = new PublishRequest()
                .withTopicArn(SNS_TOPIC_ARN)
                .withSubject("Relatorio Semanal de Feedbacks")
                .withMessage(emailBody);

            snsClient.publish(publishRequest);
            context.getLogger().log("Relatório semanal enviado com sucesso");
            
            return "Relatório gerado e enviado";
        } catch (Exception e) {
            context.getLogger().log("Erro ao gerar relatório: " + e.getMessage());
            return "Erro: " + e.getMessage();
        }
    }

    private ReportData generateReport() throws SQLException {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(7);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            ReportData data = new ReportData();
            
            // Média de avaliações
            String avgQuery = "SELECT AVG(score) FROM feedbacks WHERE created_at BETWEEN ? AND ?";
            try (PreparedStatement stmt = conn.prepareStatement(avgQuery)) {
                stmt.setTimestamp(1, Timestamp.valueOf(startDate));
                stmt.setTimestamp(2, Timestamp.valueOf(endDate));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    data.averageScore = rs.getDouble(1);
                }
            }

            // Total de feedbacks
            String countQuery = "SELECT COUNT(*) FROM feedbacks WHERE created_at BETWEEN ? AND ?";
            try (PreparedStatement stmt = conn.prepareStatement(countQuery)) {
                stmt.setTimestamp(1, Timestamp.valueOf(startDate));
                stmt.setTimestamp(2, Timestamp.valueOf(endDate));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    data.totalFeedbacks = rs.getLong(1);
                }
            }

            // Contagem por urgência
            String urgencyQuery = "SELECT urgency_level, COUNT(*) FROM feedbacks WHERE created_at BETWEEN ? AND ? GROUP BY urgency_level";
            try (PreparedStatement stmt = conn.prepareStatement(urgencyQuery)) {
                stmt.setTimestamp(1, Timestamp.valueOf(startDate));
                stmt.setTimestamp(2, Timestamp.valueOf(endDate));
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String level = rs.getString(1);
                    long count = rs.getLong(2);
                    switch (level) {
                        case "HIGH" -> data.highUrgency = count;
                        case "MEDIUM" -> data.mediumUrgency = count;
                        case "LOW" -> data.lowUrgency = count;
                    }
                }
            }

            // Feedbacks por dia
            String dailyQuery = "SELECT DATE(created_at), COUNT(*) FROM feedbacks WHERE created_at BETWEEN ? AND ? GROUP BY DATE(created_at)";
            try (PreparedStatement stmt = conn.prepareStatement(dailyQuery)) {
                stmt.setTimestamp(1, Timestamp.valueOf(startDate));
                stmt.setTimestamp(2, Timestamp.valueOf(endDate));
                ResultSet rs = stmt.executeQuery();
                StringBuilder dailyBreakdown = new StringBuilder();
                while (rs.next()) {
                    dailyBreakdown.append(String.format("%s: %d feedbacks\n", 
                        rs.getDate(1), rs.getLong(2)));
                }
                data.dailyBreakdown = dailyBreakdown.toString();
            }

            return data;
        }
    }

    private String formatReportEmail(ReportData data) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return String.format(
            "RELATORIO SEMANAL DE FEEDBACKS\n" +
            "Gerado em: %s\n\n" +
            "RESUMO GERAL\n" +
            "================================\n" +
            "Total de avaliacoes: %d\n" +
            "Media de avaliacoes: %.2f\n\n" +
            "AVALIACOES POR URGENCIA\n" +
            "================================\n" +
            "Alta: %d\n" +
            "Media: %d\n" +
            "Baixa: %d\n\n" +
            "AVALIACOES POR DIA\n" +
            "================================\n" +
            "%s\n" +
            "================================\n" +
            "Relatorio gerado automaticamente pelo sistema",
            LocalDateTime.now().format(formatter),
            data.totalFeedbacks,
            data.averageScore,
            data.highUrgency,
            data.mediumUrgency,
            data.lowUrgency,
            data.dailyBreakdown.isEmpty() ? "Nenhum feedback registrado neste periodo" : data.dailyBreakdown
        );
    }

    static class ReportData {
        double averageScore;
        long totalFeedbacks;
        long highUrgency;
        long mediumUrgency;
        long lowUrgency;
        String dailyBreakdown;
    }
}
