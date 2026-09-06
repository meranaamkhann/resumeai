package com.resumeai.service.analysis;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class SectionDetector {

    private static final Map<String, List<String>> SECTION_ALIASES = Map.of(
            "experience", List.of("experience", "work experience", "professional experience", "employment history"),
            "education", List.of("education", "academic background", "academic qualifications"),
            "skills", List.of("skills", "technical skills", "core competencies", "technical expertise"),
            "projects", List.of("projects", "selected projects", "academic projects", "personal projects"),
            "certifications", List.of("certifications", "certifications & courses", "certifications and courses"),
            "summary", List.of("summary", "professional summary", "objective", "about"),
            "achievements", List.of("achievements", "accomplishments"),
            "publications", List.of("publications"),
            "languages", List.of("languages")
    );

    public record DetectionResult(List<String> detectedSections, List<String> missingImportant) {}

    public DetectionResult detect(String normalizedText) {
        String lower = normalizedText.toLowerCase();
        LinkedHashMap<String, Boolean> found = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : SECTION_ALIASES.entrySet()) {
            boolean present = entry.getValue().stream().anyMatch(alias ->
                    Pattern.compile("(?m)^\\s*" + Pattern.quote(alias) + "\\s*$", Pattern.CASE_INSENSITIVE)
                            .matcher(normalizedText).find()
                    || lower.contains("\n" + alias + "\n")
            );
            found.put(entry.getKey(), present);
        }

        List<String> detected = found.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();

        List<String> importantOnes = List.of("experience", "education", "skills");
        List<String> missing = importantOnes.stream().filter(s -> !detected.contains(s)).toList();

        return new DetectionResult(detected, missing);
    }
}

