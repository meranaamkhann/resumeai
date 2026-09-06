package com.resumeai.service;

import com.resumeai.dto.ResumeVersionRequest;
import com.resumeai.dto.ResumeVersionResponse;
import com.resumeai.entity.ResumeDocument;
import com.resumeai.entity.ResumeVersion;
import com.resumeai.exception.ResourceNotFoundException;
import com.resumeai.repository.ResumeVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ResumeVersionService {

    private final ResumeVersionRepository resumeVersionRepository;
    private final ResumeUploadService resumeUploadService;

    public ResumeVersionService(ResumeVersionRepository resumeVersionRepository, ResumeUploadService resumeUploadService) {
        this.resumeVersionRepository = resumeVersionRepository;
        this.resumeUploadService = resumeUploadService;
    }

    @Transactional
    public ResumeVersionResponse create(UUID userId, ResumeVersionRequest request) {
        ResumeDocument document = resumeUploadService.getOwnedDocument(request.documentId(), userId);

        ResumeVersion version = ResumeVersion.builder()
                .resumeId(document.getResumeId())
                .documentId(document.getId())
                .userId(userId)
                .targetRole(request.targetRole())
                .targetCompany(request.targetCompany())
                .jobDescriptionId(request.jobDescriptionId())
                .build();
        resumeVersionRepository.save(version);
        return toResponse(version);
    }

    public List<ResumeVersionResponse> list(UUID userId) {
        return resumeVersionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ResumeVersionResponse get(UUID userId, UUID versionId) {
        return toResponse(resumeVersionRepository.findByIdAndUserId(versionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume version")));
    }

    private ResumeVersionResponse toResponse(ResumeVersion v) {
        return new ResumeVersionResponse(v.getId(), v.getResumeId(), v.getDocumentId(),
                v.getTargetRole(), v.getTargetCompany(), v.getJobDescriptionId(), v.getCreatedAt(), v.getUpdatedAt());
    }
}

