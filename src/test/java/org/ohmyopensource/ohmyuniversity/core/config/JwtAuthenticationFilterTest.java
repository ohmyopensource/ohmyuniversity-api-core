package org.ohmyopensource.ohmyuniversity.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.service.OmuJwtService;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 *
 * OmuJwtService is mocked. SecurityContextHolder is cleared before each test.
 * Spring servlet mocks are used — no MockMvc, no application context.
 */
class JwtAuthenticationFilterTest {

  private OmuJwtService jwtService;
  private JwtAuthenticationFilter filter;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockFilterChain chain;

  @BeforeEach
  void setUp() {
    jwtService = mock(OmuJwtService.class);
    filter = new JwtAuthenticationFilter(jwtService);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    chain = new MockFilterChain();
    SecurityContextHolder.clearContext();
  }

  // ============================================================
  // No / malformed Authorization header > filter skips auth
  // ============================================================

  @Nested
  @DisplayName("When Authorization header is absent or malformed")
  class NoHeader {

    @Test
    @DisplayName("no header → chain proceeds, SecurityContext empty")
    void noHeader() throws Exception {
      filter.doFilterInternal(request, response, chain);

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      assertThat(chain.getRequest()).isNotNull(); // chain was called
      verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("header without 'Bearer ' prefix → chain proceeds, SecurityContext empty")
    void headerWithoutBearerPrefix() throws Exception {
      request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

      filter.doFilterInternal(request, response, chain);

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("header is exactly 'Bearer ' with no token → jwtService called, JwtException swallowed")
    void bearerPrefixOnly() throws Exception {
      request.addHeader("Authorization", "Bearer ");
      when(jwtService.validate("")).thenThrow(new JwtException("empty token"));

      filter.doFilterInternal(request, response, chain);

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      assertThat(chain.getRequest()).isNotNull();
    }
  }

  // ============================================================
  // Invalid / expired token → JwtException swallowed, no auth
  // ============================================================

  @Nested
  @DisplayName("When token is invalid or expired")
  class InvalidToken {

    @Test
    @DisplayName("JwtException → chain proceeds, SecurityContext empty")
    void invalidToken() throws Exception {
      request.addHeader("Authorization", "Bearer bad.token.here");
      when(jwtService.validate("bad.token.here"))
          .thenThrow(new JwtException("signature invalid"));

      filter.doFilterInternal(request, response, chain);

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      assertThat(chain.getRequest()).isNotNull();
    }
  }

  // ============================================================
  // Valid token > OmuPrincipal built and injected into context
  // ============================================================

  @Nested
  @DisplayName("When token is valid")
  class ValidToken {

    private static final String TOKEN = "valid.jwt.token";

    private Claims buildClaims(Object stuId, Object matId) {
      Claims claims = mock(Claims.class);
      when(claims.getSubject()).thenReturn("auth0|abc123");
      when(claims.get("cf", String.class)).thenReturn("TSTXXX00A00X000X");
      when(claims.get("uni", String.class)).thenReturn("UNIMOL");
      when(claims.get("stuId")).thenReturn(stuId);
      when(claims.get("matId")).thenReturn(matId);
      when(claims.get("matricola", String.class)).thenReturn("178026");
      return claims;
    }

    @Test
    @DisplayName("Long stuId/matId → OmuPrincipal populated, ROLE_STUDENTE granted")
    void validToken_longIds() throws Exception {
      Claims claims = buildClaims(89486L, 106279L);
      request.addHeader("Authorization", "Bearer " + TOKEN);
      when(jwtService.validate(TOKEN)).thenReturn(claims);

      filter.doFilterInternal(request, response, chain);

      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      assertThat(auth).isNotNull();
      assertThat(auth.isAuthenticated()).isTrue();
      assertThat(auth.getAuthorities())
          .extracting("authority")
          .containsExactly("ROLE_STUDENTE");

      OmuPrincipal principal = (OmuPrincipal) auth.getPrincipal();
      assertThat(principal.omuUserId()).isEqualTo("auth0|abc123");
      assertThat(principal.codiceFiscale()).isEqualTo("TSTXXX00A00X000X");
      assertThat(principal.universityId()).isEqualTo("UNIMOL");
      assertThat(principal.stuId()).isEqualTo(89486L);
      assertThat(principal.matId()).isEqualTo(106279L);
      assertThat(principal.matricola()).isEqualTo("178026");
    }

    @Test
    @DisplayName("Integer stuId/matId (JJWT deserialization) → correctly cast to Long")
    void validToken_integerIds_castToLong() throws Exception {
      Claims claims = buildClaims(89486, 106279);
      request.addHeader("Authorization", "Bearer " + TOKEN);
      when(jwtService.validate(TOKEN)).thenReturn(claims);

      filter.doFilterInternal(request, response, chain);

      OmuPrincipal principal = (OmuPrincipal) SecurityContextHolder
          .getContext().getAuthentication().getPrincipal();

      assertThat(principal.stuId()).isEqualTo(89486L);
      assertThat(principal.matId()).isEqualTo(106279L);
    }

    @Test
    @DisplayName("null stuId and matId in claims → OmuPrincipal has null ids, no NPE")
    void validToken_nullIds() throws Exception {
      Claims claims = buildClaims(null, null);
      request.addHeader("Authorization", "Bearer " + TOKEN);
      when(jwtService.validate(TOKEN)).thenReturn(claims);

      filter.doFilterInternal(request, response, chain);

      OmuPrincipal principal = (OmuPrincipal) SecurityContextHolder
          .getContext().getAuthentication().getPrincipal();

      assertThat(principal.stuId()).isNull();
      assertThat(principal.matId()).isNull();
    }

    @Test
    @DisplayName("valid token → filter chain continues after auth injection")
    void validToken_chainProceeds() throws Exception {
      Claims claims = buildClaims(1L, 2L);
      request.addHeader("Authorization", "Bearer " + TOKEN);
      when(jwtService.validate(TOKEN)).thenReturn(claims);

      filter.doFilterInternal(request, response, chain);

      assertThat(chain.getRequest()).isNotNull();
      verify(jwtService).validate(TOKEN);
    }
  }
}