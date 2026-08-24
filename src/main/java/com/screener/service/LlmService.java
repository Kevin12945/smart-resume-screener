package com.screener.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Thin wrapper around the Groq API (OpenAI-compatible chat completions endpoint,
 * https://api.groq.com/openai/v1/chat/completions).
 *
 * Chosen because Groq offers a genuine free tier (no credit card, no billing
 * setup) - get a key at https://console.groq.com/keys.
 *
 * Requires the GROQ_API_KEY environment variable to be set at runtime.
 * The model name is configurable via `llm.groq.model` in application.properties.
 */
@Service
public class LlmService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.groq.api-key}")
    private String apiKey;

    @Value("${llm.groq.base-url}")
    private String baseUrl;

    @Value("${llm.groq.model}")
    private String model;

    public LlmService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Sends a single-turn prompt to Groq and returns the raw text of the response.
     *
     * @param systemPrompt instructions describing the task and required output format
     * @param userPrompt   the task-specific content (resume text, job description, etc.)
     * @return the model's raw text reply
     */
    public String complete(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GROQ_API_KEY is not set. Export it before starting the application, e.g.\n" +
                            "  export GROQ_API_KEY=gsk_...\n" +
                            "Get a free key (no credit card) at https://console.groq.com/keys");
        }

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);

        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

        JsonNode response = restTemplate.exchange(baseUrl, HttpMethod.POST, request, JsonNode.class).getBody();

        if (response == null || !response.has("choices") || response.get("choices").isEmpty()) {
            throw new IllegalStateException("Empty response from LLM provider: " + response);
        }

        String text = response.get("choices").get(0).path("message").path("content").asText("");

        if (text.isBlank()) {
            throw new IllegalStateException("LLM response contained no text content: " + response);
        }

        return stripJsonFences(text);
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
