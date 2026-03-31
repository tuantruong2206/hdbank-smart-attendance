package com.hdbank.notification.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

    @CircuitBreaker(name = "ntfy-push", fallbackMethod = "sendFallback")
    @Retry(name = "ntfy-push")
    public void send(String employeeCode, String title, String body) {
        if (employeeCode == null) {
            log.warn("No employee code for push notification, skipping");
            return;
        }
        String topic = "sa-" + employeeCode;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Title", title);
        headers.set("Priority", "default");
        headers.set("Tags", "bell");
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(ntfyUrl + "/" + topic, request, String.class);
        log.info("Push sent to {} via ntfy", employeeCode);
    }

    /**
     * Fallback method invoked when the ntfy-push circuit breaker is open or all retries are exhausted.
     */
    @SuppressWarnings("unused")
    private void sendFallback(String employeeCode, String title, String body, Throwable t) {
        log.error("Circuit breaker fallback for ntfy push to {}: {} - {}",
                employeeCode, t.getClass().getSimpleName(), t.getMessage());
    }
}
