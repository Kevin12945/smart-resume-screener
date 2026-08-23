package com.screener.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/**
 * Uses the LLM to convert unstructured resume text into structured JSON:
 * skills, experience entries, and education entries.
 */
@Service
public class ResumeExtractionService {

    private static final String SYSTEM_PROMPT = """
            You are a precise resume-parsing engine. Given raw resume text, extract
            structured information and return ONLY a single valid JSON object -
            no markdown, no commentary, no explanation before or after it.

            The JSON object must have exactly this shape:
            {
              "candidateName": string,
              "skills": [string, ...],
              "experience": [
                { "title": string, "company": string, "duration": string, "description": string }
              ],
              "education": [
                { "degree": string, "institution": string, "year": string }
              ]
            }

            Rules:
            - "skills" should include technical skills, tools, languages, and frameworks
              explicitly mentioned or clearly implied by project/work descriptions.
            - If a field is unknown, use an empty string "" (for strings) or an empty
              array [] (for lists). Never omit a key.
            - "candidateName" should be the applicant's full name if it appears in the
              resume text, otherwise "".
            - Do not invent information that is not present in the resume text.
            """;

    private final LlmService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumeExtractionService(LlmService llmService) {
        this.llmService = llmService;
    }

    public record ExtractionResult(
            String candidateName,
            String skillsJson,
            String experienceJson,
            String educationJson
    ) {}

    public ExtractionResult extract(String resumeText) {
        String userPrompt = "Resume text:\n---\n" + resumeText + "\n---\n" +
                "Return the JSON object described in the system prompt.";

        String rawJson = llmService.complete(SYSTEM_PROMPT, userPrompt);

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String candidateName = root.path("candidateName").asText("");
            String skillsJson = root.path("skills").toString();
            String experienceJson = root.path("experience").toString();
            String educationJson = root.path("education").toString();
            return new ExtractionResult(candidateName, skillsJson, experienceJson, educationJson);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse LLM extraction output as JSON. Raw output: " + rawJson, e);
        }
    }
}
