package com.hdbank.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that populates MDC (Mapped Diagnostic Context) with
 * traceId, userId, and requestId for structured logging.
 *
 * Should be registered early in the filter chain so that all subsequent
 * log messages include these context values.
 */
public class MdcFilter extends OncePerRequestFilter {

    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Set requestId from header or generate one
            String requestId = request.getHeader("X-Request-Id");
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString().substring(0, 8);
            }
            MDC.put(MDC_REQUEST_ID, requestId);

            // Set traceId from header or generate one
            String traceId = request.getHeader("X-Trace-Id");
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString();
            }
            MDC.put(MDC_TRACE_ID, traceId);

            // Set userId from header (gateway populates this) or from SecurityContext
            String userId = request.getHeader("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                MDC.put(MDC_USER_ID, userId);
            }

            // Propagate traceId in response header
            response.setHeader("X-Trace-Id", traceId);
            response.setHeader("X-Request-Id", requestId);

            filterChain.doFilter(request, response);

            // After auth filter, try to fill userId from SecurityContext if not set
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (MDC.get(MDC_USER_ID) == null && auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                MDC.put(MDC_USER_ID, principal.getId().toString());
            }

        } finally {
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_REQUEST_ID);
        }
    }
}
