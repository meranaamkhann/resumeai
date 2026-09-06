package com.resumeai.service.parsing;

import com.resumeai.exception.InvalidFileException;
import com.resumeai.service.ocr.OcrService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ResumeParsingService {

    private static final int MAX_TEXT_LENGTH = 50_000;
    private static final int MAX_ANALYZED_PDF_PAGES = 15;

    private final PdfTextExtractor pdfTextExtractor;
    private final DocxTextExtractor docxTextExtractor;
    private final TxtTextExtractor txtTextExtractor;
    private final TextNormalizer textNormalizer;
    private final ParsingConfidenceCalculator confidenceCalculator;
    private final ParsingTimeoutGuard timeoutGuard;
    private final OcrService ocrService;

    public ResumeParsingService(PdfTextExtractor pdfTextExtractor,
                                 DocxTextExtractor docxTextExtractor,
                                 TxtTextExtractor txtTextExtractor,
                                 TextNormalizer textNormalizer,
                                 ParsingConfidenceCalculator confidenceCalculator,
                                 ParsingTimeoutGuard timeoutGuard,
                                 OcrService ocrService) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.docxTextExtractor = docxTextExtractor;
        this.txtTextExtractor = txtTextExtractor;
        this.textNormalizer = textNormalizer;
        this.confidenceCalculator = confidenceCalculator;
        this.timeoutGuard = timeoutGuard;
        this.ocrService = ocrService;
    }

    public record ParsedResume(String normalizedText, int parsingConfidence, List<String> parsingIssues, boolean truncated) {}

    public ParsedResume parse(byte[] bytes, String extension) {
        String rawText;
        boolean likelyImageOnly = false;
        List<String> structuralIssues = new ArrayList<>();

        switch (extension) {
            case "pdf" -> {
                PdfTextExtractor.ExtractionResult result = timeoutGuard.runWithTimeout(() -> pdfTextExtractor.extract(bytes));
                rawText = result.text();
                likelyImageOnly = result.likelyImageOnly();
                if (result.pageCount() > MAX_ANALYZED_PDF_PAGES) {
                    structuralIssues.add("this resume has " + result.pageCount() + " pages; only the first "
                            + MAX_ANALYZED_PDF_PAGES + " were analyzed");
                }
                if (likelyImageOnly) {
                    if (ocrService.isAvailable()) {
                        try {
                            rawText = ocrService.extractText(bytes);
                            likelyImageOnly = rawText == null || rawText.trim().length() < 40;
                        } catch (RuntimeException ocrFailure) {
                            structuralIssues.add("this PDF appears to be image-based and OCR could not recover readable text");
                        }
                    } else {
                        structuralIssues.add("this PDF appears to be image-based; OCR is not enabled on this deployment, so image-only text could not be recovered");
                    }
                }
            }
            case "docx" -> rawText = timeoutGuard.runWithTimeout(() -> docxTextExtractor.extract(bytes));
            case "txt" -> rawText = timeoutGuard.runWithTimeout(() -> txtTextExtractor.extract(bytes));
            default -> throw new InvalidFileException("Unsupported file type: " + extension);
        }

        String normalized = textNormalizer.normalize(rawText);

        boolean truncated = normalized.length() > MAX_TEXT_LENGTH;
        if (truncated) {
            normalized = normalized.substring(0, MAX_TEXT_LENGTH);
            structuralIssues.add("the extracted text was unusually long and was truncated before analysis");
        }

        var confidence = confidenceCalculator.calculate(normalized, likelyImageOnly);

        List<String> allIssues = new ArrayList<>(structuralIssues);
        allIssues.addAll(confidence.issues());

        return new ParsedResume(normalized, confidence.score(), allIssues, truncated);
    }
}

