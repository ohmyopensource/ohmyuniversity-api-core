package org.ohmyopensource.ohmyuniversity.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the core microservice.
 *
 * Auth endpoints are public — all other endpoints require a valid OhMyU JWT.
 * JWT validation is handled by the Spring Security OAuth2 Resource Server
 * configured with our own signing key.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // Auth endpoints — public, no token required
            .requestMatchers("/api/auth/**").permitAll()
            // Actuator health — public
            .requestMatchers("/actuator/health").permitAll()
            // Swagger — public in dev, disabled in prod via YAML
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            // Everything else — requires valid OhMyU JWT
            .anyRequest().authenticated());

    return http.build();
  }
}