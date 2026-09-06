package com.resumeai.service.analysis;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class FormattingAnalyzer {

    private static final Pattern BULLET_GLYPHS = Pattern.compile("[•●▪◦➤➔✔✓★]");
    private static final Pattern EXCESSIVE_UNICODE = Pattern.compile("[\\x{1F300}-\\x{1FAFF}]");

    public record FormattingResult(int score, List<Issue> issues) {}

    public FormattingResult analyze(String normalizedText, int parsingConfidence, boolean likelyImageOnly) {
        List<Issue> issues = new ArrayList<>();
        int score = 100;

        if (likelyImageOnly) {
            score -= 40;
            issues.add(new Issue("formatting", Issue.Severity.CRITICAL,
                    "Document appears image-based",
                    "Very little text could be extracted, suggesting the resume content is embedded as images rather than selectable text.",
                    "Re-export your resume from its source document (Word/Google Docs) as a text-based PDF rather than a scanned image."));
        }

        long emojiMatches = EXCESSIVE_UNICODE.matcher(normalizedText).results().count();
        if (emojiMatches > 3) {
            score -= 10;
            issues.add(new Issue("formatting", Issue.Severity.MEDIUM,
                    "Emoji or decorative symbols detected",
                    "Several emoji or unusual symbols were found in the resume text.",
                    "Remove decorative symbols and emoji — most ATS parsers ignore or mishandle them."));
        }

        long bulletGlyphCount = BULLET_GLYPHS.matcher(normalizedText).results().count();
        long lineCount = normalizedText.lines().count();
        if (lineCount > 0 && bulletGlyphCount > 0 && bulletGlyphCount < lineCount * 0.05) {
            score -= 5;
            issues.add(new Issue("formatting", Issue.Severity.LOW,
                    "Inconsistent bullet usage",
                    "Bullet characters appear on only a small fraction of lines, suggesting inconsistent formatting.",
                    "Use a consistent bullet style for all experience and project entries."));
        }

        if (parsingConfidence < 70) {
            score -= 15;
            issues.add(new Issue("formatting", Issue.Severity.HIGH,
                    "Low parsing confidence",
                    "The document structure made reliable text extraction difficult, which often correlates with tables, columns, or text boxes that ATS systems also struggle with.",
                    "Consider a simpler single-column layout using standard headings and paragraph text instead of tables or text boxes."));
        }

        score = Math.max(0, Math.min(100, score));
        return new FormattingResult(score, issues);
    }
}

