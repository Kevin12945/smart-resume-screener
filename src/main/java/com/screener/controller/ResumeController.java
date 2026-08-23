package com.screener.controller;

import com.screener.entity.Resume;
import com.screener.exception.ResourceNotFoundException;
import com.screener.repository.ResumeRepository;
import com.screener.service.PdfTextExtractionService;
import com.screener.service.ResumeExtractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeRepository resumeRepository;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final ResumeExtractionService resumeExtractionService;

    public ResumeController(ResumeRepository resumeRepository,
                             PdfTextExtractionService pdfTextExtractionService,
                             ResumeExtractionService resumeExtractionService) {
        this.resumeRepository = resumeRepository;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.resumeExtractionService = resumeExtractionService;
    }

    /**
     * Uploads a resume (.pdf or .txt), extracts raw text, then calls the LLM
     * to derive structured skills/experience/education.
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<Resume> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String rawText = pdfTextExtractionService.extractText(file);

        ResumeExtractionService.ExtractionResult extraction = resumeExtractionService.extract(rawText);

        Resume resume = new Resume();
        resume.setFileName(file.getOriginalFilename());
        resume.setRawText(rawText);
        resume.setCandidateName(
                extraction.candidateName().isBlank() ? file.getOriginalFilename() : extraction.candidateName());
        resume.setExtractedSkillsJson(extraction.skillsJson());
        resume.setExtractedExperienceJson(extraction.experienceJson());
        resume.setExtractedEducationJson(extraction.educationJson());

        return ResponseEntity.ok(resumeRepository.save(resume));
    }

    @GetMapping
    public List<Resume> list() {
        return resumeRepository.findAll();
    }

    @GetMapping("/{id}")
    public Resume get(@PathVariable Long id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found: " + id));
    }
}
