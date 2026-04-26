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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(org.springframework.security.config.Customizer.withDefaults()) // Enable CORS
            .csrf(csrf -> csrf.disable()) // Disable CSRF for development
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/customers/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_COMPANY_OWNER")
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_SUPER_ADMIN")
                .requestMatchers("/api/owners/**").hasAuthority("ROLE_COMPANY_OWNER")
                .requestMatchers("/api/managers/**").hasAnyAuthority("ROLE_SERVICE_MANAGER", "ROLE_COMPANY_OWNER")
                .requestMatchers("/api/service-centers/**").hasAnyAuthority("ROLE_COMPANY_OWNER", "ROLE_SERVICE_MANAGER")
                .requestMatchers("/api/service-packages/**").hasAnyAuthority("ROLE_COMPANY_OWNER", "ROLE_SERVICE_MANAGER")
                .requestMatchers("/api/analytics/**").hasAuthority("ROLE_COMPANY_OWNER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(java.util.List.of("http://localhost:3000"));
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
