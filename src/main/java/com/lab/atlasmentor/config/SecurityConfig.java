package com.lab.atlasmentor.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ✅ Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ CORS Configuration (FIXED)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowCredentials(true);

        configuration.setAllowedOrigins(Arrays.asList(
                "http://65.0.103.210",
                "http://localhost:4200",
                "http://localhost:4500",
                "https://app.atlasmentor.com"
        ));

        configuration.setAllowedHeaders(Arrays.asList("*"));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ✅ Security Filter Chain
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF is disabled intentionally: this API is fully stateless.
                // Authentication is carried exclusively in the Authorization: Bearer <JWT>
                // request header — no session cookies are issued or read anywhere in the
                // application. Because browsers never automatically attach the JWT header
                // on cross-origin requests (unlike cookies), CSRF attacks cannot succeed
                // and the synchroniser-token pattern would add cost with zero security gain.
                // If cookie-based auth is ever introduced, re-enable CSRF protection.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .headers(headers -> {
                        headers.contentTypeOptions(Customizer.withDefaults());
                        headers.frameOptions(frame -> frame.deny());
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        );
                        headers.contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'none'; frame-ancestors 'none'")
                        );
                        headers.referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)
                        );
                        headers.addHeaderWriter(new StaticHeadersWriter(
                                "Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=()"
                        ));
                        headers.cacheControl(Customizer.withDefaults());
                })
                .authorizeHttpRequests(authz -> authz

                        // Public APIs
                        .requestMatchers("/api/auth/login", "/api/auth/register",
                                         "/api/auth/verify-email", "/api/auth/resend-verification",
                                         "/api/auth/forgot-password", "/api/auth/reset-password",
                                         "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers("/api/mobile-country-codes/**").permitAll()
                        .requestMatchers("/api/students/register").permitAll()
                        .requestMatchers("/api/countries").permitAll()
                        .requestMatchers("/api/universities/country/**").permitAll()

                        // Tamper-evident financial audit log — ADMIN only
                        .requestMatchers("/api/audit/**").hasRole("ADMIN")

                        // Role-based APIs
                        .requestMatchers("/api/admin/dashboard/**")
                        .hasAnyRole("ADMIN", "MANAGER", "BRANCH_PARTNER", "ADMINISTRATIVE_ASSISTANT",
                                "SENIOR_COUNSELLOR", "JUNIOR_COUNSELLOR")

                        // Employee Tree (EmployeeTreeController) - kept in sync with its
                        // @PreAuthorize; must be listed before the generic /api/admin/** rule
                        // below or MANAGER/BRANCH_PARTNER/ADMINISTRATIVE_ASSISTANT get a 403
                        // at the filter-chain level before the controller is even reached.
                        .requestMatchers("/api/admin/employee-tree", "/api/admin/employees/**")
                        .hasAnyRole("ADMIN", "ADMINISTRATIVE_ASSISTANT", "MANAGER", "BRANCH_PARTNER")

                        .requestMatchers("/api/admin/**")
                        .hasAnyRole("ADMIN", "SENIOR_COUNSELLOR")

                        .requestMatchers("/api/manager/**")
                        .hasAnyRole("ADMIN", "MANAGER", "BRANCH_PARTNER", "ADMINISTRATIVE_ASSISTANT")

                        .requestMatchers("/api/employee/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE", "BRANCH_PARTNER", "ADMINISTRATIVE_ASSISTANT")

                        .requestMatchers("/api/company/**")
                        .hasAnyRole("ADMIN", "MANAGER", "COMPANY", "BRANCH_PARTNER", "ADMINISTRATIVE_ASSISTANT")

                        .requestMatchers("/api/referral/**")
                        .hasAnyRole("ADMIN", "MANAGER", "BRANCH_PARTNER", "ADMINISTRATIVE_ASSISTANT")

                        .requestMatchers("/api/students/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE", "REFERRAL",
                                "COMPANY", "SENIOR_COUNSELLOR", "JUNIOR_COUNSELLOR", "BRANCH_PARTNER", "ADMINISTRATIVE_ASSISTANT")

                        // Lead import (LeadImportController) - same access as /api/students/**,
                        // since every imported lead lands in the Student table via the same
                        // onboarding creation logic.
                        .requestMatchers("/api/leads/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE", "REFERRAL",
                                "COMPANY", "SENIOR_COUNSELLOR", "JUNIOR_COUNSELLOR", "BRANCH_PARTNER", "ADMINISTRATIVE_ASSISTANT")

                        .requestMatchers("/api/student/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE", "STUDENT", "BRANCH_PARTNER", "ADMINISTRATIVE_ASSISTANT")

                        .requestMatchers("/api/tasks/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE",
                                "SENIOR_COUNSELLOR", "JUNIOR_COUNSELLOR", "VIDEO_EDITOR", "BRANCH_PARTNER", "ADMINISTRATIVE_ASSISTANT")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}