package com.resumeai.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.email", name = "provider", havingValue = "none", matchIfMissing = true)
public class ConsoleEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public void send(String toEmail, String subject, String bodyText) {
        log.warn("EMAIL_PROVIDER=none: no real email was sent. This is what would have gone to {}: [{}] {}",
                toEmail, subject, bodyText);
    }
}

