package com.resumeai.service;

import com.resumeai.exception.InvalidFileException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSecurityServiceTest {

    private final FileSecurityService service = new FileSecurityService(10);

    @Test
    @DisplayName("accepts a real PDF with correct magic bytes and extension")
    void acceptsValidPdf() {
        byte[] pdfBytes = minimalPdfBytes();
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfBytes);

        FileSecurityService.ValidatedFile result = service.validate(file);

        assertThat(result.extension()).isEqualTo("pdf");
        assertThat(result.detectedMimeType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("accepts a real DOCX with correct magic bytes and extension")
    void acceptsValidDocx() throws Exception {
        byte[] docxBytes = minimalDocxBytes();
        MockMultipartFile file = new MockMultipartFile("file", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes);

        FileSecurityService.ValidatedFile result = service.validate(file);

        assertThat(result.extension()).isEqualTo("docx");
    }

    @Test
    @DisplayName("rejects a file whose extension does not match its actual content")
    void rejectsExtensionMimeMismatch() {
        byte[] pdfBytes = minimalPdfBytes();
        MockMultipartFile disguisedFile = new MockMultipartFile("file", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", pdfBytes);

        assertThatThrownBy(() -> service.validate(disguisedFile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    @DisplayName("rejects an executable disguised with a .pdf extension")
    void rejectsDisguisedExecutable() {
        byte[] exeMagicBytes = new byte[]{0x4D, 0x5A, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", exeMagicBytes);

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    @DisplayName("rejects unsupported file extensions")
    void rejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.exe", "application/octet-stream", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    @DisplayName("rejects a file that exceeds the configured maximum size")
    void rejectsOversizedFile() {
        FileSecurityService smallLimitService = new FileSecurityService(1);
        byte[] oversized = new byte[2 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", oversized);

        assertThatThrownBy(() -> smallLimitService.validate(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("maximum allowed size");
    }

    @Test
    @DisplayName("rejects an empty file")
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("accepts plain text resumes")
    void acceptsPlainText() {
        byte[] textBytes = "John Doe\nSoftware Engineer\nExperience\nSkills\nJava, Python".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", textBytes);

        FileSecurityService.ValidatedFile result = service.validate(file);

        assertThat(result.extension()).isEqualTo("txt");
    }

    @Test
    @DisplayName("computes a stable content hash for duplicate detection")
    void computesConsistentContentHash() {
        byte[] textBytes = "same content".getBytes();
        MockMultipartFile first = new MockMultipartFile("file", "a.txt", "text/plain", textBytes);
        MockMultipartFile second = new MockMultipartFile("file", "b.txt", "text/plain", textBytes);

        String hashOne = service.validate(first).contentHashSha256();
        String hashTwo = service.validate(second).contentHashSha256();

        assertThat(hashOne).isEqualTo(hashTwo);
    }

    @Test
    @DisplayName("sanitizes a filename containing path traversal characters")
    void sanitizesPathTraversalFilename() {
        byte[] textBytes = "content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "../../etc/passwd.txt", "text/plain", textBytes);

        FileSecurityService.ValidatedFile result = service.validate(file);

        assertThat(result.sanitizedDisplayName()).doesNotContain("/").doesNotContain("\\");
    }

    @Test
    @DisplayName("rejects a DOCX-shaped zip with an absurd compression ratio (zip bomb)")
    void rejectsZipBombDisguisedAsDocx() throws Exception {
        byte[] bombBytes = buildZipBombShapedAsOoxml();
        MockMultipartFile file = new MockMultipartFile("file", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bombBytes);

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidFileException.class);
    }

    private byte[] buildZipBombShapedAsOoxml() throws Exception {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(byteOut)) {
            String contentTypesXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Override PartName=\"/word/document.xml\" "
                    + "ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                    + "</Types>";
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(contentTypesXml.getBytes());
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("word/document.xml"));
            byte[] repeatedChunk = "A".repeat(1_000_000).getBytes();
            for (int i = 0; i < 50; i++) {
                zip.write(repeatedChunk);
            }
            zip.closeEntry();
        }
        return byteOut.toByteArray();
    }

    private byte[] minimalPdfBytes() {
        String pdf = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
                + "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"
                + "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] >>\nendobj\n"
                + "xref\n0 4\ntrailer\n<< /Root 1 0 R /Size 4 >>\nstartxref\n0\n%%EOF";
        return pdf.getBytes();
    }

    private byte[] minimalDocxBytes() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText("Test resume content for magic byte validation");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }
}

