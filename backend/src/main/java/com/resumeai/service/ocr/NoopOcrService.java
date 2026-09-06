package com.resumeai.service.ocr;

import org.springframework.stereotype.Service;

@Service
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

