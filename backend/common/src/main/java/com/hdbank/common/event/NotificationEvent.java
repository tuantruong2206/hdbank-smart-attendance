package com.hdbank.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private UUID targetEmployeeId;
    private String targetEmployeeCode;
    private String title;
    private String body;
    private String type; // PUSH, EMAIL, SMS
    private String priority; // LOW, NORMAL, HIGH, URGENT
    private Map<String, String> data;
}
