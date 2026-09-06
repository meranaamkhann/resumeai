package com.resumeai.service.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@ConditionalOnProperty(prefix = "app.payments", name = "provider", havingValue = "stripe")
public class StripePaymentProvider implements PaymentProvider {

    private final String webhookSecret;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StripePaymentProvider(@Value("${app.payments.webhook-secret:}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    public boolean isConfigured() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        if (!isConfigured() || signatureHeader == null) return false;

        String timestamp = null;
        String providedSignature = null;
        for (String part : signatureHeader.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].equals("t")) timestamp = kv[1];
            if (kv[0].equals("v1")) providedSignature = kv[1];
        }
        if (timestamp == null || providedSignature == null) return false;

        try {
            String signedPayload = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String computedHex = bytesToHex(computed);
            return MessageDigest.isEqual(computedHex.getBytes(StandardCharsets.UTF_8), providedSignature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String extractEventId(String payload) {
        try {
            return objectMapper.readTree(payload).path("id").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public WebhookEvent parseEvent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventId = root.path("id").asText(null);
            String type = root.path("type").asText(null);
            JsonNode object = root.path("data").path("object");
            String customerReference = object.path("client_reference_id").asText(null);
            if (customerReference == null) {
                customerReference = object.path("metadata").path("userId").asText(null);
            }

            String newPlan = switch (type == null ? "" : type) {
                case "checkout.session.completed", "customer.subscription.updated" -> {
                    String status = object.path("status").asText("");
                    yield "canceled".equals(status) || "unpaid".equals(status) ? "FREE" : "PRO";
                }
                case "customer.subscription.deleted" -> "FREE";
                default -> null;
            };

            return new WebhookEvent(eventId, type, customerReference, newPlan);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse webhook payload", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}

