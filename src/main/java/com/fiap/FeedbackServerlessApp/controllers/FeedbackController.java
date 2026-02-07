package com.fiap.FeedbackServerlessApp.controllers;

import com.fiap.FeedbackServerlessApp.dtos.CreateFeedbackRequest;
import com.fiap.FeedbackServerlessApp.entities.Feedback;
import com.fiap.FeedbackServerlessApp.services.FeedbackService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/avaliacao")
public class FeedbackController {

    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody CreateFeedbackRequest request) {

        if (request.score < 0 || request.score > 10) {
            return ResponseEntity.badRequest()
                    .body("Score deve estar entre 0 e 10");
        }

        Feedback feedback = service.create(
                request.description,
                request.score
        );

        return ResponseEntity.status(201).body(feedback);
    }
}

