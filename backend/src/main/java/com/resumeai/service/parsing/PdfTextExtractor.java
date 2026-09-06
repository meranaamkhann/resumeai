package com.resumeai.service.parsing;

import com.resumeai.exception.InvalidFileException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfTextExtractor {

    private static final int MAX_PAGES = 15;

    public ExtractionResult extract(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new InvalidFileException(
                        "This PDF is password-protected. Please upload an unlocked file.");
            }
            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                throw new InvalidFileException("This PDF has no pages.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(pageCount, MAX_PAGES));
            String text = stripper.getText(document);
            boolean likelyImageOnly = text == null || text.trim().length() < 40;
            return new ExtractionResult(text == null ? "" : text, pageCount, likelyImageOnly);
        } catch (InvalidFileException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidFileException(
                    "We couldn't extract readable text from this PDF. It may be corrupted, scanned, or image-based. Try uploading a text-based PDF or DOCX.");
        }
    }

    public record ExtractionResult(String text, int pageCount, boolean likelyImageOnly) {}
}

