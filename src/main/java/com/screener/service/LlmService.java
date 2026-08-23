package com.screener.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Thin wrapper around the Google Gemini API
 * (https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent).
 *
 * Chosen because Google AI Studio offers a genuine free tier (no credit card,
 * no billing setup) - see https://aistudio.google.com to generate a key.
 *
 * Requires the GEMINI_API_KEY environment variable to be set at runtime.
 * The model name is configurable via `llm.gemini.model` in application.properties.
 */
@Service
public class LlmService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.gemini.api-key}")
    private String apiKey;

    @Value("${llm.gemini.base-url}")
    private String baseUrl;

    @Value("${llm.gemini.model}")
    private String model;

    public LlmService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Sends a single-turn prompt to Gemini and returns the raw text of the response.
     *
     * @param systemPrompt instructions describing the task and required output format
     * @param userPrompt   the task-specific content (resume text, job description, etc.)
     * @return the model's raw text reply
     */
    public String complete(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not set. Export it before starting the application, e.g.\n" +
                            "  export GEMINI_API_KEY=AIza...\n" +
                            "Get a free key (no credit card) at https://aistudio.google.com/apikey");
        }

        String url = baseUrl + "/" + model + ":generateContent";

        ObjectNode requestBody = objectMapper.createObjectNode();

        ObjectNode systemInstruction = requestBody.putObject("system_instruction");
        systemInstruction.putArray("parts").addObject().put("text", systemPrompt);

        ObjectNode userContent = requestBody.putArray("contents").addObject();
        userContent.putArray("parts").addObject().put("text", userPrompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

        JsonNode response = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class).getBody();

        if (response == null || !response.has("candidates") || response.get("candidates").isEmpty()) {
            throw new IllegalStateException("Empty response from LLM provider: " + response);
        }

        JsonNode parts = response.get("candidates").get(0).path("content").path("parts");
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.has("text")) {
                text.append(part.get("text").asText());
            }
        }

        if (text.isEmpty()) {
            throw new IllegalStateException("LLM response contained no text content: " + response);
        }

        return stripJsonFences(text.toString());
    }

    /** Removes ```json / ``` markdown fences if the model wraps its JSON output in them. */
    private String stripJsonFences(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }
}
