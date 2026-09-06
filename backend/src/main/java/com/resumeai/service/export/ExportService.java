package com.resumeai.service.export;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExportService {

    private static final float FONT_SIZE = 11f;
    private static final float LEADING = 14f;
    private static final float MARGIN = 50f;

    public byte[] toAtsSafePdf(String extractedText) {
        try (PDDocument document = new PDDocument()) {
            List<String> lines = wrapLines(extractedText, 95);
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            stream.setFont(font, FONT_SIZE);
            stream.beginText();
            stream.newLineAtOffset(MARGIN, page.getMediaBox().getHeight() - MARGIN);

            float y = page.getMediaBox().getHeight() - MARGIN;
            for (String line : lines) {
                if (y < MARGIN) {
                    stream.endText();
                    stream.close();
                    page = new PDPage(PDRectangle.LETTER);
                    document.addPage(page);
                    stream = new PDPageContentStream(document, page);
                    stream.setFont(font, FONT_SIZE);
                    stream.beginText();
                    y = page.getMediaBox().getHeight() - MARGIN;
                    stream.newLineAtOffset(MARGIN, y);
                }
                stream.showText(sanitizeForPdf(line));
                stream.newLineAtOffset(0, -LEADING);
                y -= LEADING;
            }
            stream.endText();
            stream.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate PDF export", e);
        }
    }

    public byte[] toAtsSafeDocx(String extractedText) {
        try (XWPFDocument document = new XWPFDocument()) {
            for (String line : extractedText.split("\n")) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setFontFamily("Calibri");
                run.setFontSize(11);
                run.setText(line);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate DOCX export", e);
        }
    }

    private List<String> wrapLines(String text, int maxCharsPerLine) {
        List<String> result = new ArrayList<>();
        for (String rawLine : text.split("\n")) {
            if (rawLine.isBlank()) {
                result.add("");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (String word : rawLine.split(" ")) {
                if (current.length() + word.length() + 1 > maxCharsPerLine) {
                    result.add(current.toString());
                    current = new StringBuilder();
                }
                if (!current.isEmpty()) current.append(' ');
                current.append(word);
            }
            if (!current.isEmpty()) result.add(current.toString());
        }
        return result;
    }

    private String sanitizeForPdf(String line) {
        return line.replaceAll("[^\\x00-\\x7F]", "?");
    }
}

