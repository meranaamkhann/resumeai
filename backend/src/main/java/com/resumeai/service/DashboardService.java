package com.resumeai.service;

import com.resumeai.dto.DashboardResponse;
import com.resumeai.entity.Resume;
import com.resumeai.entity.ResumeAnalysis;
import com.resumeai.repository.ResumeAnalysisRepository;
import com.resumeai.repository.ResumeRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository analysisRepository;

    public DashboardService(ResumeRepository resumeRepository, ResumeAnalysisRepository analysisRepository) {
        this.resumeRepository = resumeRepository;
        this.analysisRepository = analysisRepository;
    }

    public DashboardResponse getDashboard(UUID userId) {
        List<Resume> activeResumes = resumeRepository.findByUserIdAndDeletedAtIsNull(userId);
        Set<UUID> activeResumeIds = activeResumes.stream().map(Resume::getId).collect(Collectors.toSet());

        List<ResumeAnalysis> analyses = analysisRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(a -> activeResumeIds.contains(a.getResumeId()))
                .toList();

        Integer latestScore = analyses.isEmpty() ? null : analyses.get(0).getOverallScore();

        List<String> topImprovementAreas = analyses.isEmpty()
                ? List.of()
                : lowestScoringCategories(analyses.get(0));

        List<DashboardResponse.RecentAnalysisSummary> recent = analyses.stream()
                .limit(5)
                .map(a -> new DashboardResponse.RecentAnalysisSummary(
                        a.getId(), a.getResumeId(), a.getDocumentId(), a.getOverallScore(), a.getCreatedAt()))
                .toList();

        return new DashboardResponse(latestScore, activeResumes.size(), analyses.size(), topImprovementAreas, recent);
    }

    private List<String> lowestScoringCategories(ResumeAnalysis a) {
        Map<String, Integer> categories = Map.of(
                "ATS parsing compatibility", a.getAtsParsingScore(),
                "Keyword alignment", a.getKeywordAlignmentScore(),
                "Resume structure", a.getStructureScore(),
                "Content quality", a.getContentQualityScore(),
                "Experience relevance", a.getExperienceRelevanceScore(),
                "Measurable impact", a.getImpactScore(),
                "Formatting", a.getFormattingScore()
        );

        return categories.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}

