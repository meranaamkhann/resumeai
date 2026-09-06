package com.resumeai.service.billing;

import com.resumeai.entity.ProcessedWebhookEvent;
import com.resumeai.entity.User;
import com.resumeai.exception.ApiException;
import com.resumeai.repository.ProcessedWebhookEventRepository;
import com.resumeai.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BillingService {

    private final PaymentProvider paymentProvider;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final UserRepository userRepository;

    public BillingService(PaymentProvider paymentProvider,
                           ProcessedWebhookEventRepository processedWebhookEventRepository,
                           UserRepository userRepository) {
        this.paymentProvider = paymentProvider;
        this.processedWebhookEventRepository = processedWebhookEventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        if (!paymentProvider.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENTS_NOT_CONFIGURED",
                    "No payment provider is configured on this deployment.");
        }

        if (!paymentProvider.verifyWebhookSignature(payload, signatureHeader)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SIGNATURE", "Webhook signature verification failed.");
        }

        PaymentProvider.WebhookEvent event = paymentProvider.parseEvent(payload);
        if (event.eventId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EVENT", "Webhook payload did not contain an event id.");
        }

        try {
            processedWebhookEventRepository.save(ProcessedWebhookEvent.builder()
                    .provider("stripe")
                    .eventId(event.eventId())
                    .build());
        } catch (DataIntegrityViolationException duplicate) {
            return;
        }

        if (event.customerReference() != null && event.newPlan() != null) {
            try {
                UUID userId = UUID.fromString(event.customerReference());
                userRepository.findById(userId).ifPresent(user -> {
                    user.setPlan(User.Plan.valueOf(event.newPlan()));
                    userRepository.save(user);
                });
            } catch (IllegalArgumentException ignoredMalformedReference) {
                return;
            }
        }
    }
}

