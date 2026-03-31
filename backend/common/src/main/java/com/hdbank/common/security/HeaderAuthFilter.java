package com.hdbank.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filter for downstream services (not auth-service) that reads user info
 * from X-User-Id and X-User-Role headers set by the gateway.
 * The gateway already validates the JWT; these services trust the headers.
 */
public class HeaderAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_USER_EMAIL = "X-User-Email";
    private static final String HEADER_ORG_ID = "X-Organization-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(HEADER_USER_ID);
        String userRole = request.getHeader(HEADER_USER_ROLE);

        if (userId != null && userRole != null) {
            String email = request.getHeader(HEADER_USER_EMAIL);
            String orgId = request.getHeader(HEADER_ORG_ID);

            UserPrincipal principal = UserPrincipal.builder()
                    .id(UUID.fromString(userId))
                    .email(email)
                    .role(userRole)
                    .organizationId(orgId != null ? UUID.fromString(orgId) : null)
                    .permissions(List.of())
                    .build();

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + userRole)
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
