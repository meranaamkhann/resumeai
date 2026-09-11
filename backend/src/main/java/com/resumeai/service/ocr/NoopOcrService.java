package com.resumeai.service.ocr;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.ocr", name = "provider", havingValue = "none", matchIfMissing = true)
public class NoopOcrService implements OcrService {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String extractText(byte[] imageOrPdfBytes) {
        throw new UnsupportedOperationException(
                "OCR is not available on this deployment. To enable it, install Tesseract OCR " +
                "(or wire up a hosted OCR API) and implement OcrService accordingly — this integration " +
                "point exists specifically so that swap doesn't touch calling code.");
    }
}

