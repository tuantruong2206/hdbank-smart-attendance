package com.hdbank.auth.adapter.in.web;

import com.hdbank.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate limiter for the login endpoint using Redis.
 * - 5 attempts per IP per minute
 * - 10 attempts per email per 5 minutes
 * Returns HTTP 429 Too Many Requests when exceeded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimiter extends OncePerRequestFilter {

    private static final String IP_KEY_PREFIX = "login-attempts:ip:";
    private static final String EMAIL_KEY_PREFIX = "login-attempts:email:";
    private static final int MAX_ATTEMPTS_PER_IP = 5;
    private static final Duration IP_WINDOW = Duration.ofMinutes(1);
    private static final int MAX_ATTEMPTS_PER_EMAIL = 10;
    private static final Duration EMAIL_WINDOW = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().equals("/api/v1/auth/login")
                || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);

        // Check IP rate limit
        String ipKey = IP_KEY_PREFIX + clientIp;
        if (isRateLimited(ipKey, MAX_ATTEMPTS_PER_IP, IP_WINDOW)) {
            log.warn("Login rate limit exceeded for IP: {}", clientIp);
            writeRateLimitResponse(response, "Too many login attempts from this IP. Please try again later.");
            return;
        }

        // We cannot easily extract email from the request body without consuming it,
        // so the email-based rate limiting is done post-filter in AuthController.
        // Here we only enforce IP-based limiting and increment the IP counter.
        incrementCounter(ipKey, IP_WINDOW);

        filterChain.doFilter(request, response);
    }

    /**
     * Check and increment email-based rate limit. Called from AuthController after parsing the request.
     * @return true if rate limited (should block)
     */
    public boolean checkEmailRateLimit(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String emailKey = EMAIL_KEY_PREFIX + email.toLowerCase();
        if (isRateLimited(emailKey, MAX_ATTEMPTS_PER_EMAIL, EMAIL_WINDOW)) {
            log.warn("Login rate limit exceeded for email: {}", email);
            return true;
        }
        incrementCounter(emailKey, EMAIL_WINDOW);
        return false;
    }

    private boolean isRateLimited(String key, int maxAttempts, Duration window) {
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            int attempts = Integer.parseInt(value);
            return attempts >= maxAttempts;
        }
        return false;
    }

    private void incrementCounter(String key, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }
    }

    private void writeRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> apiResponse = ApiResponse.error(429, message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
