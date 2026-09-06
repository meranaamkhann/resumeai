package com.resumeai.service.ai;

import com.resumeai.util.SkillDictionary;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class FactualConsistencyGuard {

    private static final Pattern METRIC_PATTERN = Pattern.compile("\\d+%|\\$\\d+[\\d,.]*[kKmMbB]?|\\d+[kKmM]\\b|\\d+x\\b|\\d+\\+");

    public record GuardResult(boolean passed, String reason) {}

    public GuardResult check(String original, String rewritten) {
        Set<String> originalMetrics = extractMetrics(original);
        Set<String> rewrittenMetrics = extractMetrics(rewritten);
        rewrittenMetrics.removeAll(originalMetrics);
        if (!rewrittenMetrics.isEmpty()) {
            return new GuardResult(false,
                    "The rewrite introduced a number or metric that wasn't in the original bullet: " + rewrittenMetrics);
        }

        Set<String> originalSkills = extractSkills(original);
        Set<String> rewrittenSkills = extractSkills(rewritten);
        rewrittenSkills.removeAll(originalSkills);
        if (!rewrittenSkills.isEmpty()) {
            return new GuardResult(false,
                    "The rewrite introduced a technology that wasn't in the original bullet: " + rewrittenSkills);
        }

        if (rewritten.length() > original.length() * 3) {
            return new GuardResult(false, "The rewrite is suspiciously longer than the original and may contain invented detail.");
        }

        return new GuardResult(true, null);
    }

    private Set<String> extractMetrics(String text) {
        Set<String> found = new HashSet<>();
        var matcher = METRIC_PATTERN.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }

    private Set<String> extractSkills(String text) {
        Set<String> found = new HashSet<>();
        String lower = text.toLowerCase();
        for (String skill : SkillDictionary.KNOWN_SKILLS) {
            if (Pattern.compile("\\b" + Pattern.quote(skill) + "\\b").matcher(lower).find()) {
                found.add(skill);
            }
        }
        return found;
    }
}

