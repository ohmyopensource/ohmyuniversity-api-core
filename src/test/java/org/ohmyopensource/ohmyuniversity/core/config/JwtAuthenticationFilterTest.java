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
 * <p>{@link OmuJwtService} is mocked via Mockito. The
 * {@link SecurityContextHolder} is cleared before each test to prevent
 * state leaking across cases. Spring servlet mocks are used in place of
 * MockMvc so that no application context is required.
 */
class JwtAuthenticationFilterTest {

  private OmuJwtService jwtService;
  private JwtAuthenticationFilter filter;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockFilterChain chain;

  /**
   * Initialises fresh collaborators and clears the {@link SecurityContextHolder}
   * before each test to guarantee isolation.
   */
  @BeforeEach
  void setUp() {
    jwtService = mock(OmuJwtService.class);
    filter = new JwtAuthenticationFilter(jwtService);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    chain = new MockFilterChain();
    SecurityContextHolder.clearContext();
  }

  /**
   * Verifies that the filter skips JWT processing and leaves the
   * {@link SecurityContextHolder} empty when the {@code Authorization}
   * header is absent or does not carry a Bearer token.
   */
  @Nested
  @DisplayName("When Authorization header is absent or malformed")
  class NoHeader {

    /**
     * Verifies that when no {@code Authorization} header is present the
     * filter chain proceeds, {@link OmuJwtService} is never invoked, and
     * no {@link Authentication} is placed in the security context.
     */
    @Test
    @DisplayName("no header → chain proceeds, SecurityContext empty")
    void noHeader() throws Exception {
      filter.doFilterInternal(request, response, chain);

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      assertThat(chain.getRequest()).isNotNull();
      verifyNoInteractions(jwtService);
    }

    /**
     * Verifies that a header using a non-Bearer scheme (e.g. Basic) is ignored,
     * the filter chain proceeds, and {@link OmuJwtService} is never invoked.
     */
    @Test
    @DisplayName("header without 'Bearer ' prefix → chain proceeds, SecurityContext empty")
    void headerWithoutBearerPrefix() throws Exception {
      request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

      filter.doFilterInternal(request, response, chain);

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      verifyNoInteractions(jwtService);
    }

    /**
     * Verifies that a header consisting of exactly {@code "Bearer "} with no
     * following token causes {@link OmuJwtService#validate(String)} to be called
     * with an empty string, the resulting {@link JwtException} to be swallowed,
     * and the filter chain to proceed without populating the security context.
     */
    @Test
    @DisplayName("header is exactly 'Bearer ' with no token → jwtService called, "
        + "JwtException swallowed")
    void bearerPrefixOnly() throws Exception {
      request.addHeader("Authorization", "Bearer ");
      when(jwtService.validate("")).thenThrow(new JwtException("empty token"));

      filter.doFilterInternal(request, response, chain);

      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      assertThat(chain.getRequest()).isNotNull();
    }
  }

  /**
   * Verifies that the filter swallows {@link JwtException} thrown by
   * {@link OmuJwtService#validate(String)} and continues the filter chain
   * without populating the security context.
   */
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

  /**
   * Verifies that a valid JWT causes the filter to build an {@link OmuPrincipal},
   * inject it into the {@link SecurityContextHolder}, and continue the filter chain.
   */
  @Nested
  @DisplayName("When token is valid")
  class ValidToken {

    private static final String TOKEN = "valid.jwt.token";

    /**
     * Builds a mocked {@link Claims} instance with standard identity fields
     * and the provided {@code stuId} and {@code matId} values.
     *
     * @param stuId value to return for the {@code stuId} claim
     * @param matId value to return for the {@code matId} claim
     * @return configured {@link Claims} mock
     */
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

    /**
     * Verifies that when claims carry {@link Long} values for {@code stuId}
     * and {@code matId} the resulting {@link OmuPrincipal} is fully populated
     * and the {@code ROLE_STUDENTE} authority is granted.
     */
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

    /**
     * Verifies that when JJWT deserialises numeric claims as {@link Integer}
     * the filter correctly promotes them to {@link Long} via
     * {@link Number#longValue()}, preventing silent precision loss.
     */
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

    /**
     * Verifies that {@code null} values for {@code stuId} and {@code matId}
     * in the JWT claims result in an {@link OmuPrincipal} with {@code null}
     * identifiers, without throwing a {@link NullPointerException}.
     */
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

    /**
     * Verifies that the filter chain continues after a successful authentication
     * injection, and that {@link OmuJwtService#validate(String)} was invoked
     * exactly once with the extracted token value.
     */
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