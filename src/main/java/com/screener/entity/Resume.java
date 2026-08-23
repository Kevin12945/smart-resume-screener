package com.screener.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String candidateName;

    @Column(nullable = false)
    private String fileName;

    @Lob
    @Column(nullable = false)
    private String rawText;

    // Structured data extracted by the LLM, stored as raw JSON strings.
    // Kept as JSON text (not normalized tables) so the exact LLM output
    // is preserved verbatim for auditability of the extraction step.
    @Lob
    private String extractedSkillsJson;

    @Lob
    private String extractedExperienceJson;

    @Lob
    private String extractedEducationJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
