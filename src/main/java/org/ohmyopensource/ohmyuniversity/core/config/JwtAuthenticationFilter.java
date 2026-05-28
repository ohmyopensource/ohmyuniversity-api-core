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
 * Spring Security filter that validates OhMyUniversity JWT tokens and populates the SecurityContext
 * with an authenticated principal.
 *
 * <p>This filter:
 * - Extracts the Bearer token from the Authorization header
 * - Validates and parses JWT claims via OmuJwtService
 * - Builds an OmuPrincipal containing user and academic context
 * - Injects authentication into Spring Security context
 *
 * <p>If the token is missing or invalid, the request proceeds unauthenticated. Protected endpoints
 * are enforced later by SecurityConfig.
 *
 * <p>Note on JJWT behavior: Numeric claims may be deserialized as Integer/Double internally,
 * therefore they must be safely converted using Number.longValue().
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER_PREFIX = "Bearer ";

  private final OmuJwtService jwtService;

  // ============ Constructor ============

  /**
   * Creates the filter and injects the JWT service used for validation.
   *
   * @param jwtService service responsible for validating and parsing JWT tokens
   */
  public JwtAuthenticationFilter(OmuJwtService jwtService) {
    this.jwtService = jwtService;
  }

  // ============ Override Methods ============

  /**
   * Intercepts every HTTP request and attempts JWT authentication.
   *
   * <p>Flow:
   * 1. Extract Authorization header
   * 2. Validate Bearer token format
   * 3. Parse and validate JWT claims
   * 4. Build OmuPrincipal from claims
   * 5. Store authentication in SecurityContext
   *
   * <p>If validation fails, the request continues without authentication.
   *
   * @param request     incoming HTTP request
   * @param response    HTTP response
   * @param filterChain remaining filter chain
   * @throws ServletException if servlet processing fails
   * @throws IOException      if I/O error occurs during filtering
   */
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