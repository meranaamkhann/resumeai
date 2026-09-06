package com.resumeai.service;

import com.resumeai.dto.ApplicationRequest;
import com.resumeai.dto.ApplicationResponse;
import com.resumeai.entity.Application;
import com.resumeai.exception.ApiException;
import com.resumeai.exception.ResourceNotFoundException;
import com.resumeai.repository.ApplicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ApplicationTrackerService {

    private final ApplicationRepository applicationRepository;

    public ApplicationTrackerService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public ApplicationResponse create(UUID userId, ApplicationRequest request) {
        Application application = Application.builder()
                .userId(userId)
                .company(request.company().trim())
                .role(request.role().trim())
                .jobUrl(request.jobUrl())
                .location(request.location())
                .applicationDate(request.applicationDate())
                .resumeVersionId(request.resumeVersionId())
                .status(parseStatus(request.status()))
                .notes(request.notes())
                .build();
        applicationRepository.save(application);
        return toResponse(application);
    }

    @Transactional
    public ApplicationResponse update(UUID userId, UUID applicationId, ApplicationRequest request) {
        Application application = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application"));

        application.setCompany(request.company().trim());
        application.setRole(request.role().trim());
        application.setJobUrl(request.jobUrl());
        application.setLocation(request.location());
        application.setApplicationDate(request.applicationDate());
        application.setResumeVersionId(request.resumeVersionId());
        application.setStatus(parseStatus(request.status()));
        application.setNotes(request.notes());
        applicationRepository.save(application);
        return toResponse(application);
    }

    public List<ApplicationResponse> list(UUID userId) {
        return applicationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID userId, UUID applicationId) {
        Application application = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application"));
        applicationRepository.delete(application);
    }

    private Application.Status parseStatus(String status) {
        if (status == null || status.isBlank()) return Application.Status.SAVED;
        try {
            return Application.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Unknown application status: " + status);
        }
    }

    private ApplicationResponse toResponse(Application a) {
        return new ApplicationResponse(a.getId(), a.getCompany(), a.getRole(), a.getJobUrl(), a.getLocation(),
                a.getApplicationDate(), a.getResumeVersionId(), a.getStatus().name(), a.getNotes(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}

