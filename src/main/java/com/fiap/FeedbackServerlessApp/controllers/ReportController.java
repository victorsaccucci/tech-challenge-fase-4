package com.fiap.FeedbackServerlessApp.controllers;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/relatorio")
public class ReportController {

    private final AWSLambda lambdaClient;

    @Value("${aws.lambda.weekly-report.function-name:weekly-report-handler}")
    private String lambdaFunctionName;

    public ReportController(AWSLambda lambdaClient) {
        this.lambdaClient = lambdaClient;
    }

    @PostMapping("/semanal")
    public ResponseEntity<?> triggerWeeklyReport() {
        try {
            
            InvokeRequest invokeRequest = new InvokeRequest()
                    .withFunctionName(lambdaFunctionName)
                    .withPayload("{}");
            
            InvokeResult result = lambdaClient.invoke(invokeRequest);
            String response = new String(result.getPayload().array(), StandardCharsets.UTF_8);
            
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Relatório semanal disparado com sucesso");
            responseBody.put("lambdaResponse", response);
            responseBody.put("statusCode", result.getStatusCode());
            
            return ResponseEntity.ok(responseBody);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erro ao disparar relatório");
            error.put("details", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
