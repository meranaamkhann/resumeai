package com.resumeai.service.analysis;

import com.resumeai.dto.AnalysisResponse;
import com.resumeai.dto.RecruiterViewResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecruiterSimulationService {

    public RecruiterViewResponse generate(AnalysisResponse analysis) {
        List<String> strengths = new ArrayList<>();
        List<String> concerns = new ArrayList<>();

        var scores = analysis.categoryScores();

        addIfHigh(strengths, "clear structure and section organization", scores.structure());
        addIfHigh(strengths, "strong ATS parsing compatibility", scores.atsCompatibility());
        addIfHigh(strengths, "well-formatted layout", scores.formatting());
        addIfHigh(strengths, "content that reads as relevant and complete", scores.contentQuality());

        addIfLow(concerns, "measurable outcomes are missing from several bullets", scores.impact());
        addIfLow(concerns, "keyword alignment could be stronger for target roles", scores.keywordAlignment());
        addIfLow(concerns, "experience section may not clearly establish relevance", scores.experienceRelevance());
        addIfLow(concerns, "formatting may cause inconsistent ATS parsing", scores.formatting());

        String firstImpression = analysis.overallScore() >= 80
                ? "Strong overall impression — this resume is well-organized and clearly communicates relevant experience."
                : analysis.overallScore() >= 60
                ? "Reasonable first impression, but a few gaps stand out on a quick scan."
                : "This resume would likely need a second look before a recruiter moves forward — several areas need work.";

        if (strengths.isEmpty()) strengths.add("No standout strengths detected by this heuristic — consider a structural review.");
        if (concerns.isEmpty()) concerns.add("No major concerns detected by this heuristic.");

        return new RecruiterViewResponse(firstImpression, strengths, concerns,
                "This is an AI-assisted heuristic based on your resume analysis, not a prediction of any real recruiter's decision.");
    }

    private void addIfHigh(List<String> list, String text, int score) {
        if (score >= 80) list.add(text);
    }

    private void addIfLow(List<String> list, String text, int score) {
        if (score < 60) list.add(text);
    }
}

