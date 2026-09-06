package com.resumeai.controller;

import com.resumeai.entity.ResumeDocument;
import com.resumeai.security.CurrentUser;
import com.resumeai.service.AnalyticsService;
import com.resumeai.service.RateLimiterService;
import com.resumeai.service.ResumeUploadService;
import com.resumeai.service.export.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ResumeUploadService resumeUploadService;
    private final ExportService exportService;
    private final RateLimiterService rateLimiterService;
    private final AnalyticsService analyticsService;

    public ExportController(ResumeUploadService resumeUploadService, ExportService exportService,
                             RateLimiterService rateLimiterService, AnalyticsService analyticsService) {
        this.resumeUploadService = resumeUploadService;
        this.exportService = exportService;
        this.rateLimiterService = rateLimiterService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{documentId}.pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID documentId) {
        var user = CurrentUser.get();
        rateLimiterService.checkAnalysis(user.userId().toString());
        ResumeDocument document = resumeUploadService.getOwnedDocument(documentId, user.userId());
        byte[] pdf = exportService.toAtsSafePdf(document.getExtractedText());
        analyticsService.record(user.userId(), "resume_exported_pdf");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume-ats-safe.pdf\"")
                .body(pdf);
    }

    @GetMapping("/{documentId}.docx")
    public ResponseEntity<byte[]> exportDocx(@PathVariable UUID documentId) {
        var user = CurrentUser.get();
        rateLimiterService.checkAnalysis(user.userId().toString());
        ResumeDocument document = resumeUploadService.getOwnedDocument(documentId, user.userId());
        byte[] docx = exportService.toAtsSafeDocx(document.getExtractedText());
        analyticsService.record(user.userId(), "resume_exported_docx");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume-ats-safe.docx\"")
                .body(docx);
    }
}
