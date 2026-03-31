package com.hdbank.notification.service;

import com.hdbank.notification.model.NotificationLogJpaEntity;
import com.hdbank.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final NtfyPushService pushService;
    private final MailHogEmailService emailService;
    private final NotificationLogRepository notificationLogRepository;

    /**
     * Dispatch a notification via the specified channel type.
     */
    public void dispatch(String type, String targetCode, String title, String body) {
        dispatch(type, targetCode, title, body, "NORMAL", "NOTIFICATION");
    }

    /**
     * Dispatch with priority-based routing.
     * URGENT -> push + email + SMS
     * HIGH -> push + email
     * NORMAL -> push only
     * LOW -> email only
     */
    public void dispatchByPriority(String targetCode, String title, String body,
                                    String priority, String eventType) {
        switch (priority) {
            case "URGENT" -> {
                sendAndLog("PUSH", targetCode, title, body, priority, eventType);
                sendAndLog("EMAIL", targetCode, title, body, priority, eventType);
                sendAndLog("SMS", targetCode, title, body, priority, eventType);
            }
            case "HIGH" -> {
                sendAndLog("PUSH", targetCode, title, body, priority, eventType);
                sendAndLog("EMAIL", targetCode, title, body, priority, eventType);
            }
            case "NORMAL" -> sendAndLog("PUSH", targetCode, title, body, priority, eventType);
            case "LOW" -> sendAndLog("EMAIL", targetCode, title, body, priority, eventType);
            default -> {
                log.warn("Unknown priority '{}', defaulting to PUSH", priority);
                sendAndLog("PUSH", targetCode, title, body, "NORMAL", eventType);
            }
        }
    }

    /**
     * Dispatch with explicit channel, priority, and event type for logging.
     */
    public void dispatch(String type, String targetCode, String title, String body,
                         String priority, String eventType) {
        sendAndLog(type, targetCode, title, body, priority, eventType);
    }

    private void sendAndLog(String channel, String targetCode, String title, String body,
                            String priority, String eventType) {
        String status = "SENT";
        String errorMessage = null;

        try {
            switch (channel) {
                case "PUSH" -> pushService.send(targetCode, title, body);
                case "EMAIL" -> emailService.send(targetCode, title, body);
                case "SMS" -> log.info("[SMS Mock] To: {}, Title: {}, Body: {}", targetCode, title, body);
                default -> {
                    log.warn("Unknown notification channel: {}", channel);
                    status = "FAILED";
                    errorMessage = "Unknown channel: " + channel;
                }
            }
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("Failed to send {} notification to {}: {}", channel, targetCode, e.getMessage());
        }

        // Persist notification log
        try {
            NotificationLogJpaEntity logEntry = NotificationLogJpaEntity.builder()
                    .targetEmployeeCode(targetCode)
                    .channel(channel)
                    .title(title)
                    .body(body)
                    .priority(priority)
                    .eventType(eventType)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();
            notificationLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to persist notification log: {}", e.getMessage());
        }
    }
}
