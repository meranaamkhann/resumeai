package com.resumeai.service;

import com.resumeai.entity.Resume;
import com.resumeai.entity.ResumeDocument;
import com.resumeai.exception.ApiException;
import com.resumeai.exception.ResourceNotFoundException;
import com.resumeai.repository.ResumeDocumentRepository;
import com.resumeai.repository.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ResumeDeletionService {

    private final ResumeRepository resumeRepository;
    private final ResumeDocumentRepository resumeDocumentRepository;
    private final StorageService storageService;
    private final AdvisoryLockService advisoryLockService;

    public ResumeDeletionService(ResumeRepository resumeRepository,
                                  ResumeDocumentRepository resumeDocumentRepository,
                                  StorageService storageService,
                                  AdvisoryLockService advisoryLockService) {
        this.resumeRepository = resumeRepository;
        this.resumeDocumentRepository = resumeDocumentRepository;
        this.storageService = storageService;
        this.advisoryLockService = advisoryLockService;
    }

    @Transactional
    public void deleteResume(UUID userId, UUID resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .filter(r -> r.getUserId().equals(userId))
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Resume"));

        List<ResumeDocument> documents = resumeDocumentRepository.findByResumeIdAndUserId(resumeId, userId);

        for (ResumeDocument document : documents) {
            if (!advisoryLockService.tryAcquireForTransaction(document.getId())) {
                throw new ApiException(HttpStatus.CONFLICT, "PROCESSING_IN_PROGRESS",
                        "This resume is currently being analyzed. Please try deleting it again in a moment.");
            }
        }

        for (ResumeDocument document : documents) {
            if (document.getDeletedAt() == null) {
                storageService.delete(document.getStorageKey());
                document.setDeletedAt(Instant.now());
                resumeDocumentRepository.save(document);
            }
        }

        resume.setDeletedAt(Instant.now());
        resumeRepository.save(resume);
    }
}

