package com.screener.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ShortlistResponse {
    private Long resumeId;
    private String candidateName;
    private Integer score;
    private String justification;
}
