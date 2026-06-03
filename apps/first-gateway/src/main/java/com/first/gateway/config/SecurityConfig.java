package com.first.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.first.gateway.infra.filter.ApiKeyAuthFilter;
import com.first.gateway.infra.filter.JwtAuthFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ApiKeyAuthFilter apiKeyAuthFilter,
                                            JwtAuthFilter jwtAuthFilter,
                                            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/actuator/**", "/h2-console/**",
                    "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/admin/api/v1/auth/login", "/admin/api/v1/auth/login/**",
                    "/admin/api/v1/auth/register", "/admin/api/v1/auth/register/**",
                    "/admin/api/v1/auth/register-enabled", "/admin/api/v1/auth/register-enabled/**",
                    "/api/v1/auth/login", "/api/v1/auth/login/**",
                    "/api/v1/auth/register", "/api/v1/auth/register/**",
                    "/api/v1/auth/register-enabled", "/api/v1/auth/register-enabled/**").permitAll()
                .requestMatchers("/admin/**", "/api/v1/**").authenticated()
                .requestMatchers("/v1/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(h -> h.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
