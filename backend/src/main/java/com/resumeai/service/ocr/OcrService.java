package com.resumeai.service.ocr;

public interface OcrService {

    boolean isAvailable();

    String extractText(byte[] imageOrPdfBytes);
}

