package com.hdbank.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailHogEmailService {

    private final JavaMailSender mailSender;

    public void send(String targetEmail, String title, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@hdbank.vn");
            message.setTo(targetEmail != null ? targetEmail + "@hdbank.vn" : "admin@hdbank.vn");
            message.setSubject("[Smart Attendance] " + title);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}", targetEmail);
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
        }
    }
}
