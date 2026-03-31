package com.hdbank.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class NtfyPushService {

    private final String ntfyUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public NtfyPushService(@Value("${ntfy.url:http://localhost:8095}") String ntfyUrl) {
        this.ntfyUrl = ntfyUrl;
    }

    public void send(String employeeCode, String title, String body) {
        if (employeeCode == null) {
            log.warn("No employee code for push notification, skipping");
            return;
        }
        try {
            String topic = "sa-" + employeeCode;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Title", title);
            headers.set("Priority", "default");
            headers.set("Tags", "bell");
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(ntfyUrl + "/" + topic, request, String.class);
            log.info("Push sent to {} via ntfy", employeeCode);
        } catch (Exception e) {
            log.error("Failed to send push to {}: {}", employeeCode, e.getMessage());
        }
    }
}
