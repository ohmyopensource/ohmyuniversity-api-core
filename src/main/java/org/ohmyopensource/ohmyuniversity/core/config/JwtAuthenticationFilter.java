package org.ohmyopensource.ohmyuniversity.core.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.ohmyopensource.ohmyuniversity.core.service.OmuJwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the OhMyUniversity JWT from the Authorization header,
 * validates it and populates the Spring Security context.
 *
 * The JWT claims are stored as an {@link OmuPrincipal} so that downstream
 * services can read matId, stuId, universityId without re-parsing the token.
 *
 * JJWT 0.12.x deserializes numeric JSON values as Integer internally,
 * so Long claims must be read via {@code (Number).longValue()} to avoid null.
 *
 * If no token is present or the token is invalid, the filter chain
 * continues unauthenticated — SecurityConfig will reject protected endpoints.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER_PREFIX = "Bearer ";

  private final OmuJwtService jwtService;

  public JwtAuthenticationFilter(OmuJwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(BEARER_PREFIX.length());

    try {
      Claims claims = jwtService.validate(token);

      Number stuIdNum = (Number) claims.get("stuId");
      Number matIdNum = (Number) claims.get("matId");

      OmuPrincipal principal = new OmuPrincipal(
          claims.getSubject(),
          claims.get("cf", String.class),
          claims.get("uni", String.class),
          stuIdNum != null ? stuIdNum.longValue() : null,
          matIdNum != null ? matIdNum.longValue() : null,
          claims.get("matricola", String.class));

      UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
          principal,
          null,
          List.of(new SimpleGrantedAuthority("ROLE_STUDENTE")));

      SecurityContextHolder.getContext().setAuthentication(auth);

    } catch (JwtException e) {
      log.warn("JwtAuthenticationFilter: invalid token — {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
  }
}