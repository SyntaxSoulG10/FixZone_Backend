package com.fixzone.fixzon_backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the main security pipeline for the application.
     * We use a stateless JWT-based approach to support scaling across multiple backend instances
     * and to avoid the overhead of server-side sessions.
     * 
     * Security Rationale:
     * 1. Statelessness: By using JWTs, the server does not need to store session state,
     *    enabling horizontal scalability in cloud environments.
     * 2. Granular Access Control: Uses RBAC (Role-Based Access Control) to restrict
     *    API endpoints based on assigned user authorities.
     * 3. CORS Policy: Explicitly defined to allow communication only from verified
     *    frontend origins, preventing unauthorized cross-origin requests.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(org.springframework.security.config.Customizer.withDefaults()) // Enable CORS
                .csrf(csrf -> csrf.disable()) // Disable CSRF for development
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/customers/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_COMPANY_OWNER", "CUSTOMER")
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/analytics/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_COMPANY_OWNER")
                        .requestMatchers("/api/owners/**").hasAnyAuthority("ROLE_COMPANY_OWNER", "OWNER")
                        .requestMatchers("/api/managers/**").hasAnyAuthority("ROLE_SERVICE_MANAGER", "ROLE_COMPANY_OWNER", "MANAGER")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Defines the Cross-Origin Resource Sharing policy for the entire API.
     * We explicitly whitelist local development origins to allow the Next.js frontend
     * to communicate with this Spring Boot server without being blocked by browser security.
     */
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        // Allow common local development origins
        configuration.setAllowedOrigins(java.util.List.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "*"));
        configuration.setExposedHeaders(java.util.List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1 hour cache for preflight requests

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
