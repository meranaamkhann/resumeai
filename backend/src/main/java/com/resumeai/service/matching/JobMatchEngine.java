package com.resumeai.service.matching;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class JobMatchEngine {

    private static final Map<String, Set<String>> SEMANTIC_EQUIVALENTS = Map.of(
            "rest", Set.of("restful api", "http api", "web api"),
            "restful api", Set.of("rest", "http api"),
            "postgresql", Set.of("postgres", "sql database"),
            "javascript", Set.of("js"),
            "typescript", Set.of("ts"),
            "kubernetes", Set.of("k8s"),
            "ci/cd", Set.of("continuous integration", "continuous deployment")
    );

    public record SkillMatchResult(int matchedCount, int totalCount,
                                    List<String> matched, List<String> missing) {}

    public record MatchOutcome(int matchScore, SkillMatchResult required, SkillMatchResult preferred,
                                String experienceMatchLabel) {}

    public MatchOutcome match(String resumeText, List<String> requiredSkills, List<String> preferredSkills,
                               Integer jdMinYears, Integer estimatedResumeYears) {
        String lower = resumeText.toLowerCase();

        SkillMatchResult requiredResult = evaluate(lower, requiredSkills);
        SkillMatchResult preferredResult = evaluate(lower, preferredSkills);

        double requiredRatio = requiredResult.totalCount() == 0 ? 1.0
                : (double) requiredResult.matchedCount() / requiredResult.totalCount();
        double preferredRatio = preferredResult.totalCount() == 0 ? 1.0
                : (double) preferredResult.matchedCount() / preferredResult.totalCount();

        String experienceLabel;
        double experienceRatio;
        if (jdMinYears == null || jdMinYears == 0) {
            experienceLabel = "Not specified";
            experienceRatio = 1.0;
        } else if (estimatedResumeYears == null) {
            experienceLabel = "Unable to estimate";
            experienceRatio = 0.6;
        } else if (estimatedResumeYears >= jdMinYears) {
            experienceLabel = "Strong";
            experienceRatio = 1.0;
        } else if (estimatedResumeYears >= jdMinYears - 1) {
            experienceLabel = "Moderate";
            experienceRatio = 0.7;
        } else {
            experienceLabel = "Below requirement";
            experienceRatio = 0.4;
        }

        int score = (int) Math.round((requiredRatio * 60) + (preferredRatio * 25) + (experienceRatio * 15));
        score = Math.max(0, Math.min(100, score));

        return new MatchOutcome(score, requiredResult, preferredResult, experienceLabel);
    }

    private SkillMatchResult evaluate(String resumeLower, List<String> jdSkills) {
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String skill : jdSkills) {
            if (containsSkill(resumeLower, skill)) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }
        return new SkillMatchResult(matched.size(), jdSkills.size(), matched, missing);
    }

    private boolean containsSkill(String resumeLower, String skill) {
        if (Pattern.compile("\\b" + Pattern.quote(skill) + "\\b").matcher(resumeLower).find()) {
            return true;
        }
        Set<String> equivalents = SEMANTIC_EQUIVALENTS.getOrDefault(skill, Set.of());
        for (String equivalent : equivalents) {
            if (Pattern.compile("\\b" + Pattern.quote(equivalent) + "\\b").matcher(resumeLower).find()) {
                return true;
            }
        }
        return false;
    }

    public Integer estimateYearsOfExperience(String resumeText) {
        Set<Integer> years = new TreeSet<>();
        var matcher = Pattern.compile("\\b(19[8-9]\\d|20[0-4]\\d)\\b").matcher(resumeText);
        while (matcher.find()) {
            years.add(Integer.parseInt(matcher.group(1)));
        }
        if (years.size() < 2) return null;
        int span = years.stream().mapToInt(Integer::intValue).max().getAsInt()
                - years.stream().mapToInt(Integer::intValue).min().getAsInt();
        return Math.min(span, 25);
    }
}

