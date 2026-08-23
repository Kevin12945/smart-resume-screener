package com.screener.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class MatchResponse {
    private Long resumeId;
    private Long jobDescriptionId;
    private Integer score;
    private String justification;
    private List<String> matchedSkills;
    private List<String> missingSkills;
}
