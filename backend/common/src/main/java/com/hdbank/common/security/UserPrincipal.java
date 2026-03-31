package com.hdbank.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPrincipal {
    private UUID id;
    private String email;
    private String fullName;
    private String role;
    private UUID organizationId;
    private List<String> permissions;
}
