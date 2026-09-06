package com.resumeai.service.billing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.payments", name = "provider", havingValue = "none", matchIfMissing = true)
public class NoopPaymentProvider implements PaymentProvider {

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        return false;
    }

    @Override
    public String extractEventId(String payload) {
        throw new UnsupportedOperationException("No payment provider is configured.");
    }

    @Override
    public WebhookEvent parseEvent(String payload) {
        throw new UnsupportedOperationException("No payment provider is configured.");
    }
}

