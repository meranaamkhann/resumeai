package com.resumeai.service.parsing;

import org.springframework.stereotype.Component;

@Component
public class TextNormalizer {

    public String normalize(String raw) {
        String text = raw.replace("\r\n", "\n").replace("\r", "\n");
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("\\n{3,}", "\n\n");
        text = text.replaceAll("\\u00A0", " ");
        return text.trim();
    }
}

