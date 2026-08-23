package com.screener.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.screener.entity.JobDescription;
import com.screener.entity.MatchResult;
import com.screener.entity.Resume;
import com.screener.repository.MatchResultRepository;
import org.springframework.stereotype.Service;

/**
 * Uses the LLM to semantically compare a resume against a job description
 * and produce a 1-10 fit score with a written justification.
 */
@Service
public class MatchingService {

    private static final String SYSTEM_PROMPT = """
            You are an expert technical recruiter. Compare the following resume with
            the given job description and rate the candidate's fit on a scale of 1-10
            (10 = ideal match, 1 = not a match at all), with a clear justification.

            Base your score on: relevant skills overlap, years/level of experience,
            domain relevance, and education where relevant. Be honest and critical -
            do not inflate scores out of politeness.

            Return ONLY a single valid JSON object - no markdown, no commentary -
            with exactly this shape:
            {
              "score": integer from 1 to 10,
              "justification": string (2-4 sentences explaining the score),
              "matchedSkills": [string, ...],
              "missingSkills": [string, ...]
            }
            """;

    private final LlmService llmService;
    private final MatchResultRepository matchResultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MatchingService(LlmService llmService, MatchResultRepository matchResultRepository) {
        this.llmService = llmService;
        this.matchResultRepository = matchResultRepository;
    }

    public MatchResult computeMatch(Resume resume, JobDescription jobDescription) {
        String userPrompt = """
                Job Description:
                ---
                Title: %s
                %s
                ---

                Candidate Resume (extracted skills: %s):
                ---
                %s
                ---

                Compare the resume with this job description and rate fit on 1-10 with justification.
                """.formatted(
                jobDescription.getTitle(),
                jobDescription.getDescription(),
                nullToEmpty(resume.getExtractedSkillsJson()),
                resume.getRawText()
        );

        String rawJson = llmService.complete(SYSTEM_PROMPT, userPrompt);

        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse LLM match output as JSON. Raw output: " + rawJson, e);
        }

        MatchResult result = new MatchResult();
        result.setResumeId(resume.getId());
        result.setJobDescriptionId(jobDescription.getId());
        result.setScore(root.path("score").asInt(0));
        result.setJustification(root.path("justification").asText(""));
        result.setMatchedSkillsJson(root.path("matchedSkills").toString());
        result.setMissingSkillsJson(root.path("missingSkills").toString());

        return matchResultRepository.save(result);
    }

    private String nullToEmpty(String s) {
        return s == null ? "[]" : s;
    }
}
