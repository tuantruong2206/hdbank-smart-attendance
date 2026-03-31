package com.hdbank.notification.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

    @CircuitBreaker(name = "email-send", fallbackMethod = "sendFallback")
    @Retry(name = "email-send")
    public void send(String targetEmail, String title, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@hdbank.vn");
        message.setTo(targetEmail != null ? targetEmail + "@hdbank.vn" : "admin@hdbank.vn");
        message.setSubject("[Smart Attendance] " + title);
        message.setText(body);
        mailSender.send(message);
        log.info("Email sent to {}", targetEmail);
    }

    /**
     * Fallback method invoked when the email-send circuit breaker is open or all retries are exhausted.
     */
    @SuppressWarnings("unused")
    private void sendFallback(String targetEmail, String title, String body, Throwable t) {
        log.error("Circuit breaker fallback for email to {}: {} - {}",
                targetEmail, t.getClass().getSimpleName(), t.getMessage());
    }
}
