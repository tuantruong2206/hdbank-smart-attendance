package com.hdbank.attendance.config;

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
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/timesheets/*/approve").hasAnyRole("DEPT_HEAD", "DEPUTY_HEAD", "UNIT_HEAD")
                        .requestMatchers("/api/v1/timesheets/*/lock").hasRole("SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/leaves/pending").hasAnyRole("DEPT_HEAD", "DEPUTY_HEAD", "UNIT_HEAD", "DIVISION_DIRECTOR", "REGION_DIRECTOR", "CEO", "SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/attendance/**").authenticated()
                        .requestMatchers("/api/v1/timesheets/**").authenticated()
                        .requestMatchers("/api/v1/leaves/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new MdcFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new HeaderAuthFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
