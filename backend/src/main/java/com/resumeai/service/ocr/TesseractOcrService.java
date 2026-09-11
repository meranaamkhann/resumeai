package com.resumeai.service.ocr;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@ConditionalOnProperty(prefix = "app.ocr", name = "provider", havingValue = "tesseract")
public class TesseractOcrService implements OcrService {

    private static final int MAX_PAGES_TO_OCR = 5;
    private static final float RENDER_DPI = 200f;

    private final String tessdataPath;

    public TesseractOcrService(@Value("${app.ocr.tessdata-path:}") String tessdataPath) {
        this.tessdataPath = tessdataPath;
    }

    @Override
    public boolean isAvailable() {
        return tessdataPath != null && !tessdataPath.isBlank() && Files.isDirectory(Path.of(tessdataPath));
    }

    @Override
    public String extractText(byte[] imageOrPdfBytes) {
        if (!isAvailable()) {
            throw new IllegalStateException(
                    "Tesseract OCR is enabled (app.ocr.provider=tesseract) but OCR_TESSDATA_PATH is not set "
                    + "or does not point to a valid tessdata directory.");
        }

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);

        StringBuilder combinedText = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(imageOrPdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pagesToRender = Math.min(document.getNumberOfPages(), MAX_PAGES_TO_OCR);

            for (int pageIndex = 0; pageIndex < pagesToRender; pageIndex++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, RENDER_DPI);
                combinedText.append(tesseract.doOCR(pageImage)).append("\n");
            }
        } catch (IOException | TesseractException e) {
            throw new IllegalStateException("OCR processing failed for this document.", e);
        }

        return combinedText.toString();
    }
}

