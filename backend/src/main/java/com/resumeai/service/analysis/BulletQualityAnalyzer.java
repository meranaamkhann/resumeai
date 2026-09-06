package com.resumeai.service.analysis;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class BulletQualityAnalyzer {

    private static final Set<String> STRONG_ACTION_VERBS = Set.of(
            "developed", "built", "designed", "implemented", "led", "architected", "optimized",
            "reduced", "increased", "automated", "launched", "migrated", "refactored", "deployed",
            "created", "engineered", "improved", "delivered", "spearheaded", "streamlined"
    );

    private static final Set<String> WEAK_OPENERS = Set.of(
            "worked on", "responsible for", "helped with", "involved in", "participated in", "assisted"
    );

    private static final Pattern METRIC_PATTERN = Pattern.compile("\\d+%|\\$\\d+|\\d+[kKmM]\\b|\\d+x\\b|\\d+\\+");

    public record BulletFinding(String bulletText, boolean hasMetric, boolean hasStrongVerb, boolean isPassiveOrWeak) {}

    public record BulletAnalysisResult(List<BulletFinding> findings, double metricCoveragePct, double strongVerbPct) {}

    public BulletAnalysisResult analyze(List<String> bullets) {
        List<BulletFinding> findings = new ArrayList<>();
        if (bullets.isEmpty()) {
            return new BulletAnalysisResult(findings, 0, 0);
        }

        int withMetric = 0;
        int withStrongVerb = 0;

        for (String bullet : bullets) {
            String trimmed = bullet.trim();
            String lower = trimmed.toLowerCase();
            boolean hasMetric = METRIC_PATTERN.matcher(trimmed).find();
            boolean hasStrongVerb = STRONG_ACTION_VERBS.stream().anyMatch(lower::startsWith);
            boolean isWeak = WEAK_OPENERS.stream().anyMatch(lower::startsWith);

            if (hasMetric) withMetric++;
            if (hasStrongVerb) withStrongVerb++;

            findings.add(new BulletFinding(trimmed, hasMetric, hasStrongVerb, isWeak));
        }

        double metricPct = (withMetric * 100.0) / bullets.size();
        double verbPct = (withStrongVerb * 100.0) / bullets.size();
        return new BulletAnalysisResult(findings, metricPct, verbPct);
    }
}

