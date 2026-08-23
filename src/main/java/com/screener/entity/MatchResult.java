package com.screener.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_results")
@Getter
@Setter
@NoArgsConstructor
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long resumeId;

    @Column(nullable = false)
    private Long jobDescriptionId;

    @Column(nullable = false)
    private Integer score; // 1-10

    @Lob
    @Column(nullable = false)
    private String justification;

    @Lob
    private String matchedSkillsJson;

    @Lob
    private String missingSkillsJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime evaluatedAt = LocalDateTime.now();
}
