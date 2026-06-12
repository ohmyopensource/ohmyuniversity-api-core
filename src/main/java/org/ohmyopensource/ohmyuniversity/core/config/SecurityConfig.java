package org.ohmyopensource.ohmyuniversity.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the core microservice.
 *
 * <p>This configuration defines a stateless JWT-based security model where:
 * - Authentication is handled via OhMyUniversity JWT tokens
 * - No server-side session is maintained
 * - Requests are authorized based on JWT claims validated by {@link JwtAuthenticationFilter}
 *
 * <p>Public endpoints:
 * - /api/auth/** (authentication flow)
 * - /actuator/health (health checks)
 * - /swagger-ui/** and /v3/api-docs/** (API documentation)
 *
 * <p>All remaining endpoints require a valid authenticated principal.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  // ============ Constructor ============

  /**
   * Creates the security configuration and injects the JWT filter responsible for validating
   * incoming requests.
   */
  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  // ============ Class Methods ============

  /**
   * Configures the Spring Security filter chain.
   *
   * <p>Security rules:
   * - CSRF disabled (stateless REST API)
   * - Stateless session management
   * - Public access to authentication and documentation endpoints
   * - All other endpoints require authentication
   * - JWT filter applied before UsernamePasswordAuthenticationFilter
   *
   * @param http Spring Security HTTP configuration
   * @return configured SecurityFilterChain
   * @throws Exception if security configuration fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/api/v1/email/auth/callback").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}