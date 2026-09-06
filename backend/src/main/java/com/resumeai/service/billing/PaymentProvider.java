package com.resumeai.service.billing;

public interface PaymentProvider {

    boolean isConfigured();

    boolean verifyWebhookSignature(String payload, String signatureHeader);

    String extractEventId(String payload);

    WebhookEvent parseEvent(String payload);

    record WebhookEvent(String eventId, String type, String customerReference, String newPlan) {}
}

