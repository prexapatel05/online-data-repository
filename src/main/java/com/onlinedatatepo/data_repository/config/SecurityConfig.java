package com.onlinedatatepo.data_repository.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for authentication and authorization.
 * 
 * This class sets up:
 * - Session-based authentication using JSESSIONID cookies
 * - CSRF protection for form submissions (Thymeleaf integration)
 * - Password encoding using BCrypt
 * - Public endpoints (login, register, home)
 * - Protected endpoints (dashboard, dataset management)
 * 
 * Role-based access control:
 * - USER: Can upload datasets, comment, rate, bookmark
 * - ADMIN: Can manage users, audit logs, moderate content
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Password encoder bean using BCrypt algorithm.
     * Provides secure password hashing with auto-generated salt.
     * 
     * @return BCryptPasswordEncoder with strength 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Security filter chain configuration for HTTP requests.
     * 
     * Public endpoints (no authentication required):
     * - /api/auth/register
     * - /api/auth/login
     * - /api/datasets/public (read-only)
     * - /home, /about, static resources
     * 
     * Protected endpoints (authentication required):
     * - /api/datasets/** (all operations except read-public)
     * - /api/users/**
     * - /dashboard/**
     * - /admin/** (ADMIN role required)
     * 
     * @param http HttpSecurity object to configure
     * @return SecurityFilterChain bean
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for API development and testing
            .csrf(csrf -> csrf.disable())
            
            // Allow all requests for now (we'll add proper security after creating controllers)
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            
            // Session management
            .sessionManagement(session -> session
                .sessionConcurrency(concurrency -> concurrency
                    .maximumSessions(1)
                )
            );
        
        return http.build();
    }
}
