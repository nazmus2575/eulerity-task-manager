package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.model.Task;
import com.eulerity.taskmanager.model.TaskStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.model:gemini-2.0-flash}") String model
    ) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.apiKey = apiKey;
        this.model = model;
    }

    public Task suggestTask(String plainLanguageDescription) {
        if (plainLanguageDescription == null || plainLanguageDescription.isBlank()) {
            throw new IllegalArgumentException("Task description is required");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        Map<String, Object> requestBody = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", buildSystemPrompt()))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", plainLanguageDescription))
                )),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.1
                )
        );

        Map<?, ?> response = restTemplate.postForObject(
                GEMINI_API_URL.formatted(model),
                new HttpEntity<>(requestBody, headers),
                Map.class
        );

        String json = extractGeneratedText(response);
        Task suggestedTask = parseTask(json);
        suggestedTask.setId(null);
        suggestedTask.setStatus(TaskStatus.TODO);

        return suggestedTask;
    }

    private String buildSystemPrompt() {
        return """
                You are a task parsing assistant for a personal task manager API.
                Convert the user's plain language request into one valid JSON object.

                Return JSON only. Do not include markdown, comments, explanations, or extra text.

                The JSON object must match this structure exactly:
                {
                  "id": null,
                  "title": "short required task title",
                  "description": "optional longer description or null",
                  "dueDate": "ISO-8601 date in yyyy-MM-dd format or null",
                  "priority": "LOW | MEDIUM | HIGH",
                  "status": "TODO"
                }

                Rules:
                - title is required and must not be blank.
                - If no priority is implied, use "MEDIUM".
                - New task suggestions must always use "TODO" for status.
                - If no due date is provided or confidently inferable, use null.
                - Use only these enum values: LOW, MEDIUM, HIGH, TODO.
                """;
    }

    private String extractGeneratedText(Map<?, ?> response) {
        try {
            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);

            return firstPart.get("text").toString();
        } catch (RuntimeException exception) {
            throw new RestClientException("Unable to read Gemini response", exception);
        }
    }

    private Task parseTask(String json) {
        try {
            return objectMapper.readValue(stripCodeFences(json), Task.class);
        } catch (JsonProcessingException exception) {
            throw new RestClientException("Gemini returned invalid task JSON", exception);
        }
    }

    private String stripCodeFences(String text) {
        return text
                .replaceFirst("^```json\\s*", "")
                .replaceFirst("^```\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
    }
}
