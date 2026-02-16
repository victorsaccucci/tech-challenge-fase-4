package com.fiap.FeedbackServerlessApp.config;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AWSConfig {

    @Bean
    public AWSLambda awsLambdaClient() {
        return AWSLambdaClientBuilder.standard()
                .withCredentials(InstanceProfileCredentialsProvider.getInstance())
                .withRegion("us-east-2")
                .build();
    }
}
