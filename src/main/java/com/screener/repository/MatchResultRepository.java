package com.screener.repository;

import com.screener.entity.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    List<MatchResult> findByJobDescriptionIdOrderByScoreDesc(Long jobDescriptionId);

    Optional<MatchResult> findByResumeIdAndJobDescriptionId(Long resumeId, Long jobDescriptionId);
}
