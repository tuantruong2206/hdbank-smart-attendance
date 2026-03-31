package com.hdbank.admin.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigChangeRequest {
    private String entityType;
    private String entityId;
    private String changeType;
    private String oldValue;
    private String newValue;
    private UUID requestedBy;
    private String requesterName;
}
