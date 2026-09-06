package com.resumeai.service.analysis;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class BulletExtractor {

    private static final Pattern BULLET_PREFIX = Pattern.compile("^\\s*[•●▪◦➤➔✔✓★*\\-]\\s+");
    private static final int MAX_BULLETS = 300;

    public List<String> extract(String normalizedText) {
        List<String> bullets = new ArrayList<>();
        for (String line : normalizedText.split("\n")) {
            if (bullets.size() >= MAX_BULLETS) break;
            if (BULLET_PREFIX.matcher(line).find()) {
                String cleaned = BULLET_PREFIX.matcher(line).replaceFirst("").trim();
                if (cleaned.length() > 15) {
                    bullets.add(cleaned);
                }
            }
        }
        return bullets;
    }
}

