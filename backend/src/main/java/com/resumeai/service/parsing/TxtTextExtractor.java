package com.resumeai.service.parsing;

import com.resumeai.exception.InvalidFileException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class TxtTextExtractor {

    public String extract(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.trim().isEmpty()) {
            throw new InvalidFileException("This text file appears to be empty.");
        }
        return text;
    }
}

