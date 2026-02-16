package com.fiap.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.google.gson.Gson;

import java.util.Map;

public class CriticalNotificationHandler implements RequestHandler<SQSEvent, String> {

    private final AmazonSNS snsClient = AmazonSNSClientBuilder.defaultClient();
    private final Gson gson = new Gson();
    private final String SNS_TOPIC_ARN = System.getenv("SNS_TOPIC_ARN");

    @Override
    public String handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                Map<String, Object> feedback = gson.fromJson(message.getBody(), Map.class);
                
                String description = (String) feedback.get("description");
                String urgency = (String) feedback.get("urgencyLevel");
                String createdAt = (String) feedback.get("createdAt");

                if ("HIGH".equals(urgency)) {
                    String emailBody = String.format(
                        "ALERTA DE FEEDBACK CRÍTICO\n\n" +
                        "Descrição: %s\n" +
                        "Urgência: %s\n" +
                        "Data de envio: %s\n\n" +
                        "Ação imediata necessária!",
                        description, urgency, createdAt
                    );

                    PublishRequest publishRequest = new PublishRequest()
                        .withTopicArn(SNS_TOPIC_ARN)
                        .withSubject("Feedback Critico Recebido")
                        .withMessage(emailBody);

                    snsClient.publish(publishRequest);
                    context.getLogger().log("Notificação crítica enviada: " + description);
                }
            } catch (Exception e) {
                context.getLogger().log("Erro ao processar mensagem: " + e.getMessage());
            }
        }
        return "Processamento concluído";
    }
}
