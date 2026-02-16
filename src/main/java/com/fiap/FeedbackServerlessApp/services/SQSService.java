package com.fiap.FeedbackServerlessApp.services;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fiap.FeedbackServerlessApp.entities.Feedback;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SQSService {

    private final AmazonSQS sqsClient;
    private final Gson gson;
    
    public SQSService() {
        this.sqsClient = AmazonSQSClientBuilder.defaultClient();
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, 
                (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> 
                    context.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .create();
    }
    
    @Value("${aws.sqs.queue-url:}")
    private String queueUrl;

    public void sendCriticalFeedback(Feedback feedback) {
        if (queueUrl == null || queueUrl.isEmpty()) {
            return; // Modo local, não envia para SQS
        }

        try {
            String messageBody = gson.toJson(feedback);
            SendMessageRequest request = new SendMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMessageBody(messageBody);
            
            sqsClient.sendMessage(request);
        } catch (Exception e) {
            System.err.println("Erro ao enviar mensagem para SQS: " + e.getMessage());
        }
    }
}
