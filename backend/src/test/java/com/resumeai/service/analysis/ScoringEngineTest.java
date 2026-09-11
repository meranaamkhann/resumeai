package com.resumeai.service.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringEngineTest {

    private final ScoringEngine scoringEngine = buildEngine();

    @Test
    @DisplayName("a well-structured resume with metrics scores substantially higher than a bare one")
    void wellStructuredResumeScoresHigherThanMinimalOne() {
        String goodResume = """
                John Doe
                john.doe@email.com
                +1 555 123 4567

                Summary
                Backend engineer with experience building scalable services.

                Experience
                Backend Developer, Acme Corp
                - Developed a payment processing service using Spring Boot and PostgreSQL, reducing latency by 35%
                - Implemented a caching layer that improved throughput by 40% under peak load
                - Led migration of legacy REST APIs to a microservices architecture

                Education
                B.Tech in Computer Science, XYZ University

                Skills
                Java, Spring Boot, PostgreSQL, Docker, AWS
                """;

        String barebonesText = "Asad. Did some coding stuff at a company once.";

        ScoringEngine.ScoreBreakdown goodBreakdown = scoringEngine.score(goodResume, 95, List.of(), false);
        ScoringEngine.ScoreBreakdown poorBreakdown = scoringEngine.score(barebonesText, 60, List.of(), false);

        assertThat(goodBreakdown.overallScore()).isGreaterThan(poorBreakdown.overallScore());
        assertThat(goodBreakdown.overallScore()).isGreaterThan(60);
    }

    @Test
    @DisplayName("flags missing experience, education, and skills sections as structural issues")
    void flagsMissingCoreSections() {
        String textWithoutSections = "Asad Khan asad@email.com +91 9999999999 I like coding and building things.";

        ScoringEngine.ScoreBreakdown breakdown = scoringEngine.score(textWithoutSections, 80, List.of(), false);

        boolean hasStructureIssue = breakdown.issues().stream()
                .anyMatch(issue -> issue.category().equals("structure"));

        assertThat(hasStructureIssue).isTrue();
        assertThat(breakdown.structureScore()).isLessThan(100);
    }

    @Test
    @DisplayName("low parsing confidence produces a high-severity parsing issue")
    void lowParsingConfidenceProducesIssue() {
        ScoringEngine.ScoreBreakdown breakdown = scoringEngine.score("Some sparse text", 30, List.of(), false);

        boolean hasLowConfidenceIssue = breakdown.issues().stream()
                .anyMatch(issue -> issue.severity() == Issue.Severity.HIGH && issue.category().equals("parsing"));

        assertThat(hasLowConfidenceIssue).isTrue();
    }

    @Test
    @DisplayName("bullets with measurable outcomes score higher on impact than bullets without")
    void bulletsWithMetricsScoreHigherImpact() {
        String withMetrics = """
                Experience
                - Developed a caching service, reducing latency by 45%
                - Automated deployment pipeline, cutting release time by 60%
                Education
                Skills
                Java
                """;

        String withoutMetrics = """
                Experience
                - Worked on a caching service
                - Helped with deployment pipeline
                Education
                Skills
                Java
                """;

        ScoringEngine.ScoreBreakdown withMetricsBreakdown = scoringEngine.score(withMetrics, 90, List.of(), false);
        ScoringEngine.ScoreBreakdown withoutMetricsBreakdown = scoringEngine.score(withoutMetrics, 90, List.of(), false);

        assertThat(withMetricsBreakdown.impactScore()).isGreaterThan(withoutMetricsBreakdown.impactScore());
    }

    @Test
    @DisplayName("all category scores stay within 0-100 bounds")
    void allScoresStayWithinBounds() {
        ScoringEngine.ScoreBreakdown breakdown = scoringEngine.score("", 0, List.of(), true);

        assertThat(breakdown.overallScore()).isBetween(0, 100);
        assertThat(breakdown.atsParsingScore()).isBetween(0, 100);
        assertThat(breakdown.structureScore()).isBetween(0, 100);
        assertThat(breakdown.contentQualityScore()).isBetween(0, 100);
        assertThat(breakdown.impactScore()).isBetween(0, 100);
        assertThat(breakdown.formattingScore()).isBetween(0, 100);
    }

    private ScoringEngine buildEngine() {
        ScoringWeights weights = new ScoringWeights();
        weights.setAtsParsing(20);
        weights.setKeywordAlignment(20);
        weights.setStructure(15);
        weights.setContentQuality(15);
        weights.setExperienceRelevance(10);
        weights.setImpactQuality(10);
        weights.setFormatting(10);

        return new ScoringEngine(
                new SectionDetector(),
                new FormattingAnalyzer(),
                new BulletExtractor(),
                new BulletQualityAnalyzer(),
                weights
        );
    }
}

