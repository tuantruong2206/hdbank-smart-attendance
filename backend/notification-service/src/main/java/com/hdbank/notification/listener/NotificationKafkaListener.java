package com.hdbank.notification.listener;

import com.hdbank.notification.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaListener {

    private final NotificationDispatcher dispatcher;

    @KafkaListener(topics = "attendance.notification", groupId = "notification-service")
    public void handleNotification(Map<String, Object> event) {
        log.info("Received notification event: {}", event);
        try {
            String type = getString(event, "type", "PUSH");
            String targetCode = getString(event, "targetEmployeeCode", null);
            String title = getString(event, "title", "Thông báo");
            String body = getString(event, "body", "");
            String priority = getString(event, "priority", "NORMAL");

            dispatcher.dispatchByPriority(targetCode, title, body, priority, "NOTIFICATION");
        } catch (Exception e) {
            log.error("Error handling notification event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "attendance.escalation", groupId = "notification-service")
    public void handleEscalation(Map<String, Object> event) {
        log.info("Received escalation event: {}", event);
        try {
            String escalatedToCode = getString(event, "escalatedToCode", null);
            String employeeCode = getString(event, "employeeCode", "unknown");
            String triggerType = getString(event, "triggerType", "ABSENT");
            int level = getInt(event, "escalationLevel", 1);
            String reason = getString(event, "reason",
                    "Cảnh báo leo thang cấp " + level + " cho nhân viên " + employeeCode);

            String title = "Cảnh báo leo thang cấp " + level;
            String priority = level >= 3 ? "URGENT" : (level >= 2 ? "HIGH" : "NORMAL");

            dispatcher.dispatchByPriority(escalatedToCode, title, reason, priority, "ESCALATION");
        } catch (Exception e) {
            log.error("Error handling escalation event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "attendance.anomaly", groupId = "notification-service")
    public void handleAnomaly(Map<String, Object> event) {
        log.info("Received anomaly event: {}", event);
        try {
            int riskScore = getInt(event, "riskScore", 0);
            String anomalyType = getString(event, "anomalyType", "unknown");
            String description = getString(event, "description", "");
            String employeeId = getString(event, "employeeId", null);

            String title = "Phát hiện bất thường: " + anomalyType;
            String body = "Điểm rủi ro: " + riskScore + ". " + description;
            String priority = riskScore >= 90 ? "URGENT" : (riskScore >= 70 ? "HIGH" : "NORMAL");

            // Anomaly alerts go to system admins (targetCode null triggers admin fallback)
            dispatcher.dispatchByPriority(null, title, body, priority, "ANOMALY");
        } catch (Exception e) {
            log.error("Error handling anomaly event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "attendance.checkin", groupId = "notification-service-checkin")
    public void handleCheckIn(Map<String, Object> event) {
        log.debug("Received check-in event for notification: {}", event.get("employeeCode"));
        try {
            String status = getString(event, "status", "VALID");
            String employeeCode = getString(event, "employeeCode", null);
            int fraudScore = getInt(event, "fraudScore", 0);

            // Only notify on suspicious check-ins
            if ("SUSPICIOUS".equals(status) || fraudScore >= 70) {
                String title = "Chấm công đáng ngờ";
                String body = "Nhân viên " + employeeCode + " có chấm công bất thường (điểm rủi ro: "
                        + fraudScore + ")";
                dispatcher.dispatchByPriority(null, title, body, "HIGH", "CHECK_IN");
            }
        } catch (Exception e) {
            log.error("Error handling check-in event: {}", e.getMessage(), e);
        }
    }

    private String getString(Map<String, Object> event, String key, String defaultValue) {
        Object value = event.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> event, String key, int defaultValue) {
        Object value = event.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
