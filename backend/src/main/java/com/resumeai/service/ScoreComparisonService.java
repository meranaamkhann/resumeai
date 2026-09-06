package com.resumeai.service;

import com.resumeai.dto.ScoreComparisonResponse;
import com.resumeai.entity.ResumeAnalysis;
import com.resumeai.exception.ResourceNotFoundException;
import com.resumeai.repository.ResumeAnalysisRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ScoreComparisonService {

    private final ResumeAnalysisRepository analysisRepository;

    public ScoreComparisonService(ResumeAnalysisRepository analysisRepository) {
        this.analysisRepository = analysisRepository;
    }

    public ScoreComparisonResponse compare(UUID userId, UUID previousAnalysisId, UUID newAnalysisId) {
        ResumeAnalysis previous = analysisRepository.findByIdAndUserId(previousAnalysisId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis"));
        ResumeAnalysis current = analysisRepository.findByIdAndUserId(newAnalysisId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis"));

        return new ScoreComparisonResponse(
                previous.getOverallScore(), current.getOverallScore(),
                current.getOverallScore() - previous.getOverallScore(),
                delta(previous.getAtsParsingScore(), current.getAtsParsingScore()),
                delta(previous.getKeywordAlignmentScore(), current.getKeywordAlignmentScore()),
                delta(previous.getStructureScore(), current.getStructureScore()),
                delta(previous.getContentQualityScore(), current.getContentQualityScore()),
                delta(previous.getExperienceRelevanceScore(), current.getExperienceRelevanceScore()),
                delta(previous.getImpactScore(), current.getImpactScore()),
                delta(previous.getFormattingScore(), current.getFormattingScore())
        );
    }

    private ScoreComparisonResponse.CategoryDelta delta(int previous, int current) {
        return new ScoreComparisonResponse.CategoryDelta(previous, current, current - previous);
    }
}

