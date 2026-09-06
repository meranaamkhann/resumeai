package com.resumeai.service.parsing;

import com.resumeai.exception.InvalidFileException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
public class DocxTextExtractor {

    public String extract(byte[] bytes) {
        ZipSecureFile.setMinInflateRatio(0.01);
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            if (text == null || text.trim().isEmpty()) {
                throw new InvalidFileException("This DOCX file appears to contain no readable text.");
            }
            return text;
        } catch (InvalidFileException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidFileException(
                    "We couldn't extract readable text from this DOCX file. It may be corrupted or malformed.");
        }
    }
}

