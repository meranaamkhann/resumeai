package com.resumeai.service.analysis;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScoringEngine {

    private final SectionDetector sectionDetector;
    private final FormattingAnalyzer formattingAnalyzer;
    private final BulletExtractor bulletExtractor;
    private final BulletQualityAnalyzer bulletQualityAnalyzer;
    private final ScoringWeights weights;

    public ScoringEngine(SectionDetector sectionDetector,
                          FormattingAnalyzer formattingAnalyzer,
                          BulletExtractor bulletExtractor,
                          BulletQualityAnalyzer bulletQualityAnalyzer,
                          ScoringWeights weights) {
        this.sectionDetector = sectionDetector;
        this.formattingAnalyzer = formattingAnalyzer;
        this.bulletExtractor = bulletExtractor;
        this.bulletQualityAnalyzer = bulletQualityAnalyzer;
        this.weights = weights;
    }

    public record ScoreBreakdown(
            int overallScore,
            int atsParsingScore,
            int keywordAlignmentScore,
            int structureScore,
            int contentQualityScore,
            int experienceRelevanceScore,
            int impactScore,
            int formattingScore,
            List<Issue> issues,
            List<String> detectedSections
    ) {}

    public ScoreBreakdown score(String normalizedText, int parsingConfidence,
                                 List<String> parsingIssuesRaw, boolean likelyImageOnly) {
        List<Issue> allIssues = new ArrayList<>();

        for (String parsingIssue : parsingIssuesRaw) {
            allIssues.add(new Issue("parsing", Issue.Severity.MEDIUM,
                    "Parsing concern detected", parsingIssue,
                    "Review the affected section and ensure it uses plain, selectable text."));
        }

        int atsParsingScore = parsingConfidence;
        if (atsParsingScore < 70) {
            allIssues.add(new Issue("parsing", Issue.Severity.HIGH,
                    "Low parsing confidence",
                    "Your resume could not be reliably parsed. Some ATS-related recommendations may be inaccurate.",
                    "Simplify the layout and re-upload for a more reliable analysis."));
        }

        SectionDetector.DetectionResult sections = sectionDetector.detect(normalizedText);
        int structureScore = computeStructureScore(sections, allIssues);

        FormattingAnalyzer.FormattingResult formattingResult =
                formattingAnalyzer.analyze(normalizedText, parsingConfidence, likelyImageOnly);
        allIssues.addAll(formattingResult.issues());

        List<String> bullets = bulletExtractor.extract(normalizedText);
        BulletQualityAnalyzer.BulletAnalysisResult bulletResult = bulletQualityAnalyzer.analyze(bullets);
        int impactScore = computeImpactScore(bullets, bulletResult, allIssues);
        int contentQualityScore = computeContentQualityScore(normalizedText, bulletResult, allIssues);

        int keywordAlignmentScore = computeBaselineKeywordScore(normalizedText, sections, allIssues);
        int experienceRelevanceScore = sections.detectedSections().contains("experience") ? 70 : 30;

        if (!sections.detectedSections().contains("experience")) {
            allIssues.add(new Issue("experience", Issue.Severity.MEDIUM,
                    "No experience section detected",
                    "We could not clearly identify a work experience or internship section.",
                    "If you have relevant experience or internships, add a clearly labeled Experience section."));
        }

        int overall = weightedAverage(
                atsParsingScore, weights.getAtsParsing(),
                keywordAlignmentScore, weights.getKeywordAlignment(),
                structureScore, weights.getStructure(),
                contentQualityScore, weights.getContentQuality(),
                experienceRelevanceScore, weights.getExperienceRelevance(),
                impactScore, weights.getImpactQuality(),
                formattingResult.score(), weights.getFormatting()
        );

        return new ScoreBreakdown(
                overall, atsParsingScore, keywordAlignmentScore, structureScore,
                contentQualityScore, experienceRelevanceScore, impactScore, formattingResult.score(),
                allIssues, sections.detectedSections()
        );
    }

    private int computeStructureScore(SectionDetector.DetectionResult sections, List<Issue> issues) {
        int score = 100;
        for (String missing : sections.missingImportant()) {
            score -= 20;
            issues.add(new Issue("structure", Issue.Severity.HIGH,
                    "Missing " + missing + " section",
                    "A clearly labeled " + missing + " section could not be detected.",
                    "Add a section titled \"" + capitalize(missing) + "\" so ATS parsers and recruiters can find it easily."));
        }
        return Math.max(0, score);
    }

    private int computeImpactScore(List<String> bullets, BulletQualityAnalyzer.BulletAnalysisResult result, List<Issue> issues) {
        if (bullets.isEmpty()) {
            issues.add(new Issue("impact", Issue.Severity.MEDIUM,
                    "No bulleted achievements detected",
                    "We couldn't find bullet-pointed lines under your experience or projects.",
                    "Use bullet points to describe your responsibilities and achievements — they're easier for both ATS and recruiters to scan."));
            return 40;
        }

        int score = (int) Math.round((result.metricCoveragePct() * 0.6) + (result.strongVerbPct() * 0.4));

        if (result.metricCoveragePct() < 30) {
            issues.add(new Issue("impact", Issue.Severity.HIGH,
                    "Few measurable outcomes in bullets",
                    "Most of your experience bullets describe responsibilities but contain very few measurable outcomes.",
                    "Add measurable results where truthful, such as latency reduction, users served, revenue impact, performance improvements, scale, or time saved. Never invent numbers you can't back up."));
        }

        if (result.strongVerbPct() < 40) {
            issues.add(new Issue("impact", Issue.Severity.MEDIUM,
                    "Weak or passive bullet openers",
                    "Several bullets open with phrases like \"worked on\" or \"responsible for\" rather than a strong action verb.",
                    "Start bullets with strong action verbs such as Developed, Implemented, Led, Optimized, or Automated."));
        }

        return Math.max(0, Math.min(100, score));
    }

    private int computeContentQualityScore(String normalizedText, BulletQualityAnalyzer.BulletAnalysisResult bulletResult, List<Issue> issues) {
        int wordCount = normalizedText.split("\\s+").length;
        int score = 100;

        if (wordCount < 150) {
            score -= 30;
            issues.add(new Issue("content", Issue.Severity.HIGH,
                    "Resume content is very short",
                    "The extracted resume text is unusually short (" + wordCount + " words), which may limit how much a recruiter or ATS can evaluate.",
                    "Expand on your experience, projects, and skills with concrete detail."));
        } else if (wordCount > 1200) {
            score -= 10;
            issues.add(new Issue("content", Issue.Severity.LOW,
                    "Resume content is very long",
                    "The extracted resume text is unusually long (" + wordCount + " words) for a typical resume.",
                    "Consider trimming to the most relevant and recent experience — most resumes read best at 1-2 pages."));
        }

        if (bulletResult.findings().isEmpty()) {
            score -= 10;
        }

        return Math.max(0, Math.min(100, score));
    }

    private int computeBaselineKeywordScore(String normalizedText, SectionDetector.DetectionResult sections, List<Issue> issues) {
        boolean hasSkillsSection = sections.detectedSections().contains("skills");
        int score = hasSkillsSection ? 75 : 45;

        if (!hasSkillsSection) {
            issues.add(new Issue("keywords", Issue.Severity.MEDIUM,
                    "No dedicated skills section",
                    "Without a clearly labeled skills section, keyword matching against job descriptions will be less reliable.",
                    "Add a Skills section listing your actual technologies and tools, grouped logically (e.g. Languages, Backend, Frontend, Databases)."));
        }

        issues.add(new Issue("keywords", Issue.Severity.GOOD,
                "Job-specific keyword matching not yet run",
                "This is your general resume score. Paste a job description to get a job-specific match score with required/preferred keyword analysis.",
                "Use the Job Match feature to compare this resume against a specific job description."));

        return score;
    }

    private int weightedAverage(int... scoreWeightPairs) {
        int weightedSum = 0;
        int totalWeight = 0;
        for (int i = 0; i < scoreWeightPairs.length; i += 2) {
            int scoreVal = scoreWeightPairs[i];
            int weight = scoreWeightPairs[i + 1];
            weightedSum += scoreVal * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? 0 : Math.round((float) weightedSum / totalWeight);
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

