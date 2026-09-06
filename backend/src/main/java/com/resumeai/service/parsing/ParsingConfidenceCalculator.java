package com.resumeai.service.parsing;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ParsingConfidenceCalculator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\+?\\d[\\d\\s().-]{7,}\\d)");

    public record ConfidenceResult(int score, List<String> issues) {}

    public ConfidenceResult calculate(String normalizedText, boolean likelyImageOnly) {
        List<String> issues = new ArrayList<>();
        int score = 100;

        int length = normalizedText.length();
        if (length < 200) {
            score -= 40;
            issues.add("text extraction appears incomplete");
        } else if (length < 500) {
            score -= 15;
            issues.add("extracted text is shorter than expected for a typical resume");
        }

        if (likelyImageOnly) {
            score -= 50;
            issues.add("some text may exist inside images");
        }

        if (!EMAIL_PATTERN.matcher(normalizedText).find()) {
            score -= 15;
            issues.add("contact email could not be detected");
        }

        if (!PHONE_PATTERN.matcher(normalizedText).find()) {
            score -= 10;
            issues.add("phone number could not be detected");
        }

        long lineBreaks = normalizedText.chars().filter(c -> c == '\n').count();
        double avgLineLength = lineBreaks == 0 ? length : (double) length / lineBreaks;
        if (avgLineLength < 8) {
            score -= 15;
            issues.add("two-column or fragmented reading order may be unreliable");
        }

        score = Math.max(0, Math.min(100, score));
        return new ConfidenceResult(score, issues);
    }
}

