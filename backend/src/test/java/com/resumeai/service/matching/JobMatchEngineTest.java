package com.resumeai.service.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobMatchEngineTest {

    private final JobMatchEngine engine = new JobMatchEngine();

    @Test
    @DisplayName("scores a resume with all required skills higher than one missing them")
    void resumeWithAllRequiredSkillsScoresHigher() {
        List<String> requiredSkills = List.of("java", "spring boot", "postgresql");
        List<String> preferredSkills = List.of("docker", "aws");

        String strongResume = "Experienced with Java, Spring Boot, PostgreSQL, Docker, and AWS.";
        String weakResume = "Experienced with Python and MongoDB.";

        JobMatchEngine.MatchOutcome strongOutcome = engine.match(strongResume, requiredSkills, preferredSkills, null, null);
        JobMatchEngine.MatchOutcome weakOutcome = engine.match(weakResume, requiredSkills, preferredSkills, null, null);

        assertThat(strongOutcome.matchScore()).isGreaterThan(weakOutcome.matchScore());
        assertThat(strongOutcome.required().matchedCount()).isEqualTo(3);
        assertThat(weakOutcome.required().matchedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("recognizes semantic equivalents like REST and RESTful API")
    void recognizesSemanticEquivalents() {
        List<String> requiredSkills = List.of("restful api");
        String resume = "Designed HTTP APIs following REST principles using Spring Boot.";

        JobMatchEngine.MatchOutcome outcome = engine.match(resume, requiredSkills, List.of(), null, null);

        assertThat(outcome.required().matchedCount()).isEqualTo(1);
        assertThat(outcome.required().missing()).isEmpty();
    }

    @Test
    @DisplayName("lists genuinely missing required skills by name")
    void listsMissingRequiredSkills() {
        List<String> requiredSkills = List.of("java", "kubernetes");
        String resume = "Experienced Java developer, no container orchestration experience.";

        JobMatchEngine.MatchOutcome outcome = engine.match(resume, requiredSkills, List.of(), null, null);

        assertThat(outcome.required().missing()).containsExactly("kubernetes");
    }

    @Test
    @DisplayName("treats unspecified experience requirements as fully satisfied")
    void unspecifiedExperienceRequirementIsFullyMatched() {
        JobMatchEngine.MatchOutcome outcome = engine.match("Some resume text", List.of(), List.of(), null, null);

        assertThat(outcome.experienceMatchLabel()).isEqualTo("Not specified");
    }

    @Test
    @DisplayName("labels experience below the requirement as such")
    void experienceBelowRequirementIsFlagged() {
        JobMatchEngine.MatchOutcome outcome = engine.match("Some resume text", List.of(), List.of(), 8, 2);

        assertThat(outcome.experienceMatchLabel()).isEqualTo("Below requirement");
    }

    @Test
    @DisplayName("match score stays within 0-100 bounds even with no required or preferred skills")
    void matchScoreStaysWithinBounds() {
        JobMatchEngine.MatchOutcome outcome = engine.match("", List.of(), List.of(), null, null);

        assertThat(outcome.matchScore()).isBetween(0, 100);
    }

    @Test
    @DisplayName("estimates years of experience from a spread of years mentioned in the resume")
    void estimatesYearsOfExperienceFromDateSpread() {
        String resume = "Worked at Acme from 2018 to 2022, then at Beta from 2022 to the present (2025).";

        Integer years = engine.estimateYearsOfExperience(resume);

        assertThat(years).isNotNull();
        assertThat(years).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("returns null years estimate when fewer than two distinct years are mentioned")
    void returnsNullWhenInsufficientYearData() {
        Integer years = engine.estimateYearsOfExperience("Started working in 2023.");

        assertThat(years).isNull();
    }
}

