package com.hdbank.admin.config;

import com.hdbank.common.security.HeaderAuthFilter;
import com.hdbank.common.security.MdcFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasAnyRole("SYSTEM_ADMIN", "CEO")
                .anyRequest().authenticated())
            .addFilterBefore(new MdcFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new HeaderAuthFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
