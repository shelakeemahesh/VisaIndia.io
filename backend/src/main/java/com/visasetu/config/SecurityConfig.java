package com.visasetu.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${visasetu.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,http://localhost:8080}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Admin Ingestion & Management
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/content/ingest").hasRole("ADMIN")

                        // 2. Public Tracking & Razorpay Webhook
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/track").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook").permitAll()

                        // 3. User-Scoped Private Endpoints
                        .requestMatchers("/api/v1/trips/my").authenticated()
                        .requestMatchers("/api/v1/users/**").authenticated()

                        // 4. Guest & Public Endpoints (Secured via Cryptographic Token / Purpose-Bound OTP / Allowlist)
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/eligibility/**",
                                "/api/v1/content/**",
                                "/api/v1/categories/**",
                                "/api/v1/requirements/**",
                                "/api/v1/notices/**",
                                "/api/v1/faqs/**",
                                "/api/v1/help/**",
                                "/api/v1/mcp/**",
                                "/api/v1/trips/**",
                                "/api/v1/payments/**",
                                "/api/v1/documents/**",
                                "/api/v1/applications/**",
                                "/llms.txt",
                                "/actuator/health",
                                "/error"
                        ).permitAll()

                        // 5. Catch-all: Deny all unauthenticated requests
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        // Support exact origins and dynamic cloud subdomains (Vercel & Render) with credentials
        config.setAllowedOriginPatterns(new java.util.ArrayList<>(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.vercel.app",
                "https://*.onrender.com"
        )));
        for (String origin : origins) {
            config.addAllowedOriginPattern(origin);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Correlation-ID", "X-Razorpay-Signature", "X-Razorpay-Event-Id", "X-Guest-Access-Token"));
        config.setExposedHeaders(List.of("X-Correlation-ID", "Authorization", "X-Guest-Access-Token"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
