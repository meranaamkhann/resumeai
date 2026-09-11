package com.resumeai.service.email;

public interface EmailSender {

    boolean isConfigured();

    void send(String toEmail, String subject, String bodyText);
}

