package com.screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.screener.dto.MatchResponse;
import com.screener.dto.ShortlistResponse;
import com.screener.entity.JobDescription;
import com.screener.entity.MatchResult;
import com.screener.entity.Resume;
import com.screener.exception.ResourceNotFoundException;
import com.screener.repository.JobDescriptionRepository;
import com.screener.repository.MatchResultRepository;
import com.screener.repository.ResumeRepository;
import com.screener.service.MatchingService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchingService matchingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MatchController(ResumeRepository resumeRepository,
                            JobDescriptionRepository jobDescriptionRepository,
                            MatchResultRepository matchResultRepository,
                            MatchingService matchingService) {
        this.resumeRepository = resumeRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.matchResultRepository = matchResultRepository;
        this.matchingService = matchingService;
    }

    /** Triggers an LLM-based match score between one resume and one job description. */
    @PostMapping("/{resumeId}/{jobDescriptionId}")
    public MatchResponse match(@PathVariable Long resumeId, @PathVariable Long jobDescriptionId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found: " + resumeId));
        JobDescription jd = jobDescriptionRepository.findById(jobDescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Job description not found: " + jobDescriptionId));

        MatchResult result = matchingService.computeMatch(resume, jd);
        return toMatchResponse(result);
    }

    /** Runs matches for every resume in the system against one job description. */
    @PostMapping("/job-descriptions/{jobDescriptionId}/run-all")
    public List<MatchResponse> matchAll(@PathVariable Long jobDescriptionId) {
        JobDescription jd = jobDescriptionRepository.findById(jobDescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Job description not found: " + jobDescriptionId));

        return resumeRepository.findAll().stream()
                .map(resume -> toMatchResponse(matchingService.computeMatch(resume, jd)))
                .toList();
    }

    /** Returns candidates for a job description, sorted by score, above an optional threshold. */
    @GetMapping("/shortlist")
    public List<ShortlistResponse> shortlist(
            @RequestParam Long jobDescriptionId,
            @RequestParam(defaultValue = "0") Integer minScore) {

        List<MatchResult> results = matchResultRepository
                .findByJobDescriptionIdOrderByScoreDesc(jobDescriptionId);

        return results.stream()
                .filter(r -> r.getScore() != null && r.getScore() >= minScore)
                .map(r -> {
                    String candidateName = resumeRepository.findById(r.getResumeId())
                            .map(Resume::getCandidateName)
                            .orElse("Unknown candidate");
                    return new ShortlistResponse(r.getResumeId(), candidateName, r.getScore(), r.getJustification());
                })
                .toList();
    }

    private MatchResponse toMatchResponse(MatchResult result) {
        return new MatchResponse(
                result.getResumeId(),
                result.getJobDescriptionId(),
                result.getScore(),
                result.getJustification(),
                parseStringArray(result.getMatchedSkillsJson()),
                parseStringArray(result.getMissingSkillsJson())
        );
    }

    private List<String> parseStringArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return Arrays.asList(objectMapper.readValue(json, String[].class));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
