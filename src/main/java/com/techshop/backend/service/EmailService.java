package com.techshop.backend.service;

public interface EmailService {
    void sendWelcomeEmail(String recipientEmail, String recipientName);
}
