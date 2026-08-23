package com.screener.controller;

import com.screener.dto.JobDescriptionRequest;
import com.screener.entity.JobDescription;
import com.screener.exception.ResourceNotFoundException;
import com.screener.repository.JobDescriptionRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-descriptions")
public class JobDescriptionController {

    private final JobDescriptionRepository jobDescriptionRepository;

    public JobDescriptionController(JobDescriptionRepository jobDescriptionRepository) {
        this.jobDescriptionRepository = jobDescriptionRepository;
    }

    @PostMapping
    public ResponseEntity<JobDescription> create(@Valid @RequestBody JobDescriptionRequest request) {
        JobDescription jd = new JobDescription();
        jd.setTitle(request.getTitle());
        jd.setDescription(request.getDescription());
        return ResponseEntity.ok(jobDescriptionRepository.save(jd));
    }

    @GetMapping
    public List<JobDescription> list() {
        return jobDescriptionRepository.findAll();
    }

    @GetMapping("/{id}")
    public JobDescription get(@PathVariable Long id) {
        return jobDescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job description not found: " + id));
    }
}
