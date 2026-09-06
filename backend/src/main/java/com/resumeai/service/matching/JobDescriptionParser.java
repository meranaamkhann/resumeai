package com.resumeai.service.matching;

import com.resumeai.util.SkillDictionary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JobDescriptionParser {

    private static final Pattern PREFERRED_HEADING = Pattern.compile(
            "(?im)^(preferred|nice to have|bonus|good to have)[:\\s].*$");
    private static final Pattern REQUIRED_HEADING = Pattern.compile(
            "(?im)^(required|requirements|must have|qualifications)[:\\s].*$");
    private static final Pattern YEARS_PATTERN = Pattern.compile(
            "(\\d{1,2})\\+?\\s*(?:-\\s*\\d{1,2}\\s*)?years?");
    private static final Pattern SENIORITY_PATTERN = Pattern.compile(
            "(?i)\\b(intern|junior|entry.level|mid.level|senior|staff|principal|lead)\\b");

    public record ParsedJd(String jobTitle, String seniority, Integer minYearsExperience,
                            List<String> requiredSkills, List<String> preferredSkills) {}

    public ParsedJd parse(String rawText) {
        String text = rawText == null ? "" : rawText;

        int preferredIdx = firstMatchIndex(PREFERRED_HEADING, text);
        int requiredIdx = firstMatchIndex(REQUIRED_HEADING, text);

        String requiredSection;
        String preferredSection;

        if (preferredIdx >= 0) {
            requiredSection = text.substring(0, preferredIdx);
            preferredSection = text.substring(preferredIdx);
        } else {
            requiredSection = text;
            preferredSection = "";
        }

        Set<String> required = extractSkills(requiredIdx >= 0 ? text.substring(requiredIdx) : requiredSection);
        Set<String> preferred = extractSkills(preferredSection);
        preferred.removeAll(required);

        String jobTitle = extractJobTitle(text);
        String seniority = extractSeniority(text);
        Integer minYears = extractMinYears(text);

        return new ParsedJd(jobTitle, seniority, minYears, new ArrayList<>(required), new ArrayList<>(preferred));
    }

    private Set<String> extractSkills(String text) {
        Set<String> found = new LinkedHashSet<>();
        String lower = text.toLowerCase();
        for (String skill : SkillDictionary.KNOWN_SKILLS) {
            String pattern = "\\b" + Pattern.quote(skill) + "\\b";
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(lower).find()) {
                found.add(skill);
            }
        }
        return found;
    }

    private String extractJobTitle(String text) {
        String firstLine = text.lines().filter(l -> !l.isBlank()).findFirst().orElse("").trim();
        if (firstLine.length() > 3 && firstLine.length() < 100 && !firstLine.endsWith(".")) {
            return firstLine;
        }
        return null;
    }

    private String extractSeniority(String text) {
        Matcher m = SENIORITY_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).toLowerCase().replace(".", "-");
        }
        return null;
    }

    private Integer extractMinYears(String text) {
        Matcher m = YEARS_PATTERN.matcher(text);
        Integer max = null;
        while (m.find()) {
            int years = Integer.parseInt(m.group(1));
            if (years > 0 && years <= 20 && (max == null || years > max)) {
                max = years;
            }
        }
        return max;
    }

    private int firstMatchIndex(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.start() : -1;
    }
}

