package com.hdbank.report.config;

import com.hdbank.common.security.HeaderAuthFilter;
import com.hdbank.common.security.MdcFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/v1/reports/**").authenticated()
                .requestMatchers("/api/v1/dashboard/**").authenticated()
                .anyRequest().authenticated())
            .addFilterBefore(new MdcFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new HeaderAuthFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
