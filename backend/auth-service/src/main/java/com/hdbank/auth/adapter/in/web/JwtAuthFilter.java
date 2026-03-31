package com.hdbank.auth.adapter.in.web;

import com.hdbank.common.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * JWT authentication filter for auth-service.
 * Extracts and validates JWT from the Authorization header,
 * then sets SecurityContextHolder with UserPrincipal.
 * Skips public endpoints (/api/v1/auth/**, /actuator/**).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final SecretKey secretKey;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
        this.secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/verify-2fa")
                || path.startsWith("/api/v1/auth/refresh")
                || path.startsWith("/api/v1/auth/request-otp")
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String tokenType = claims.get("type", String.class);
                if (!"access".equals(tokenType)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"status\":401,\"message\":\"Invalid token type\"}");
                    return;
                }

                UUID userId = UUID.fromString(claims.getSubject());
                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);
                String fullName = claims.get("fullName", String.class);

                UserPrincipal principal = UserPrincipal.builder()
                        .id(userId)
                        .email(email)
                        .fullName(fullName)
                        .role(role)
                        .permissions(List.of())
                        .build();

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":401,\"message\":\"Invalid or expired token\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
