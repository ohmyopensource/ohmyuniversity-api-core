package org.ohmyopensource.ohmyuniversity.core.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.service.OmuJwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integration tests for {@link SecurityConfig} authorization rules.
 *
 * <p>Verifies which endpoints are publicly accessible and which require
 * a valid authenticated principal. The real {@link JwtAuthenticationFilter} is loaded so that the
 * security filter chain behaves as in production. {@link OmuJwtService} is mocked to avoid
 * cryptographic dependencies.
 *
 * <p>Every request is sent without an {@code Authorization} header, meaning
 * no {@code Authentication} is ever placed in the
 * {@link org.springframework.security.core.context.SecurityContext}. Public endpoints must not
 * return {@code 401} or {@code 403}; protected endpoints must return {@code 403}.
 *
 * <p>A {@link StubController} scoped to the {@code security-test} profile
 * registers all tested routes without pulling real controllers — and their transitive dependencies
 * — into the slice context.
 */
@WebMvcTest(controllers = SecurityConfigTest.StubController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@org.springframework.test.context.ActiveProfiles("security-test")
class SecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  /**
   * Mocked to satisfy {@link JwtAuthenticationFilter}'s constructor dependency.
   */
  @MockitoBean
  private OmuJwtService omuJwtService;

  /**
   * Custom {@link ResultMatcher} that fails the test if Spring Security blocked the request with
   * {@code 401 Unauthorized} or {@code 403 Forbidden}.
   *
   * @return matcher asserting the response is not an auth-related rejection
   */
  private static ResultMatcher notBlocked() {
    return result -> {
      int status = result.getResponse().getStatus();
      assert status != 401 && status != 403
          : "Expected endpoint to be public but got HTTP " + status;
    };
  }


  /**
   * Minimal controller that registers all routes exercised by this test class.
   *
   * <p>Scoped to the {@code security-test} profile to prevent component scan
   * from picking it up in the full application context loaded by {@code @SpringBootTest}, which
   * would cause ambiguous mapping conflicts with the real controllers.
   */
  @RestController
  @org.springframework.context.annotation.Profile("security-test")
  static class StubController {

    /**
     * Stub for {@code POST /api/auth/login}.
     */
    @org.springframework.web.bind.annotation.PostMapping("/api/auth/login")
    ResponseEntity<Void> login(
        @org.springframework.web.bind.annotation.RequestBody(required = false) Object b) {
      return ResponseEntity.ok().build();
    }

    /**
     * Stub for {@code POST /api/auth/refresh}.
     */
    @org.springframework.web.bind.annotation.PostMapping("/api/auth/refresh")
    ResponseEntity<Void> refresh() {
      return ResponseEntity.ok().build();
    }

    /**
     * Stub for {@code POST /api/auth/logout}.
     */
    @org.springframework.web.bind.annotation.PostMapping("/api/auth/logout")
    ResponseEntity<Void> logout() {
      return ResponseEntity.ok().build();
    }

    /**
     * Stub for {@code GET /actuator/health}.
     */
    @org.springframework.web.bind.annotation.GetMapping("/actuator/health")
    ResponseEntity<Void> health() {
      return ResponseEntity.ok().build();
    }

    /**
     * Stub for {@code GET /swagger-ui/index.html}.
     */
    @org.springframework.web.bind.annotation.GetMapping("/swagger-ui/index.html")
    ResponseEntity<Void> swagger() {
      return ResponseEntity.ok().build();
    }

    /**
     * Stub for {@code GET /v3/api-docs}.
     */
    @org.springframework.web.bind.annotation.GetMapping("/v3/api-docs")
    ResponseEntity<Void> apiDocs() {
      return ResponseEntity.ok().build();
    }

    /**
     * Stub for {@code GET /api/carriera/libretto}.
     */
    @org.springframework.web.bind.annotation.GetMapping("/api/carriera/libretto")
    ResponseEntity<Void> libretto() {
      return ResponseEntity.ok().build();
    }

    /**
     * Stub for {@code GET /api/carriera/medie}.
     */
    @org.springframework.web.bind.annotation.GetMapping("/api/carriera/medie")
    ResponseEntity<Void> medie() {
      return ResponseEntity.ok().build();
    }

    /**
     * Stub for {@code GET /api/carriera/piano}.
     */
    @org.springframework.web.bind.annotation.GetMapping("/api/carriera/piano")
    ResponseEntity<Void> piano() {
      return ResponseEntity.ok().build();
    }

    /**
     * Stub for {@code GET /api/carriera/tasse}.
     */
    @org.springframework.web.bind.annotation.GetMapping("/api/carriera/tasse")
    ResponseEntity<Void> tasse() {
      return ResponseEntity.ok().build();
    }

    /**
     * Stub for {@code GET /api/carriera/badge}.
     */
    @org.springframework.web.bind.annotation.GetMapping("/api/carriera/badge")
    ResponseEntity<Void> badge() {
      return ResponseEntity.ok().build();
    }

    /**
     * Stub for {@code POST /api/carriera/prenotazioni}.
     */
    @org.springframework.web.bind.annotation.PostMapping("/api/carriera/prenotazioni")
    ResponseEntity<Void> prenotazioni(
        @org.springframework.web.bind.annotation.RequestBody(required = false) Object b) {
      return ResponseEntity.ok().build();
    }
  }


  /**
   * Verifies that endpoints explicitly permitted in {@link SecurityConfig} are reachable without
   * any authentication token.
   */
  @Nested
  @DisplayName("Public endpoints — must not return 401 or 403")
  class PublicEndpoints {

    /**
     * Verifies that {@code POST /api/auth/login} is publicly accessible without an
     * {@code Authorization} header.
     */
    @Test
    @DisplayName("POST /api/auth/login is publicly accessible")
    void authLoginIsPublic() throws Exception {
      mockMvc.perform(post("/api/auth/login")
              .contentType("application/json")
              .content("{}"))
          .andExpect(notBlocked());
    }

    /**
     * Verifies that {@code POST /api/auth/refresh} is publicly accessible without an
     * {@code Authorization} header.
     */
    @Test
    @DisplayName("POST /api/auth/refresh is publicly accessible")
    void authRefreshIsPublic() throws Exception {
      mockMvc.perform(post("/api/auth/refresh")
              .param("refreshToken", "tok")
              .param("universityId", "UNIMOL"))
          .andExpect(notBlocked());
    }

    /**
     * Verifies that {@code POST /api/auth/logout} is publicly accessible without an
     * {@code Authorization} header.
     */
    @Test
    @DisplayName("POST /api/auth/logout is publicly accessible")
    void authLogoutIsPublic() throws Exception {
      mockMvc.perform(post("/api/auth/logout")
              .param("refreshToken", "tok")
              .param("universityId", "UNIMOL"))
          .andExpect(notBlocked());
    }

    /**
     * Verifies that {@code GET /actuator/health} is publicly accessible without an
     * {@code Authorization} header.
     */
    @Test
    @DisplayName("GET /actuator/health is publicly accessible")
    void actuatorHealthIsPublic() throws Exception {
      mockMvc.perform(get("/actuator/health"))
          .andExpect(notBlocked());
    }

    /**
     * Verifies that {@code GET /swagger-ui/index.html} is publicly accessible without an
     * {@code Authorization} header.
     */
    @Test
    @DisplayName("GET /swagger-ui/index.html is publicly accessible")
    void swaggerUiIsPublic() throws Exception {
      mockMvc.perform(get("/swagger-ui/index.html"))
          .andExpect(notBlocked());
    }

    /**
     * Verifies that {@code GET /v3/api-docs} is publicly accessible without an
     * {@code Authorization} header.
     */
    @Test
    @DisplayName("GET /v3/api-docs is publicly accessible")
    void apiDocsIsPublic() throws Exception {
      mockMvc.perform(get("/v3/api-docs"))
          .andExpect(notBlocked());
    }
  }

  /**
   * Verifies that endpoints not explicitly permitted in {@link SecurityConfig} return
   * {@code 403 Forbidden} when no authentication token is provided.
   */
  @Nested
  @DisplayName("Protected endpoints — must return 403 without authentication")
  class ProtectedEndpoints {

    /**
     * Verifies that {@code GET /api/carriera/libretto} returns {@code 403} when no
     * {@code Authorization} header is present.
     */
    @Test
    @DisplayName("GET /api/carriera/libretto returns 403 without token")
    void librettoRequiresAuth() throws Exception {
      mockMvc.perform(get("/api/carriera/libretto"))
          .andExpect(status().isForbidden());
    }

    /**
     * Verifies that {@code GET /api/carriera/medie} returns {@code 403} when no
     * {@code Authorization} header is present.
     */
    @Test
    @DisplayName("GET /api/carriera/medie returns 403 without token")
    void medieRequiresAuth() throws Exception {
      mockMvc.perform(get("/api/carriera/medie"))
          .andExpect(status().isForbidden());
    }

    /**
     * Verifies that {@code GET /api/carriera/piano} returns {@code 403} when no
     * {@code Authorization} header is present.
     */
    @Test
    @DisplayName("GET /api/carriera/piano returns 403 without token")
    void pianoRequiresAuth() throws Exception {
      mockMvc.perform(get("/api/carriera/piano"))
          .andExpect(status().isForbidden());
    }

    /**
     * Verifies that {@code GET /api/carriera/tasse} returns {@code 403} when no
     * {@code Authorization} header is present.
     */
    @Test
    @DisplayName("GET /api/carriera/tasse returns 403 without token")
    void tasseRequiresAuth() throws Exception {
      mockMvc.perform(get("/api/carriera/tasse"))
          .andExpect(status().isForbidden());
    }

    /**
     * Verifies that {@code GET /api/carriera/badge} returns {@code 403} when no
     * {@code Authorization} header is present.
     */
    @Test
    @DisplayName("GET /api/carriera/badge returns 403 without token")
    void badgeRequiresAuth() throws Exception {
      mockMvc.perform(get("/api/carriera/badge"))
          .andExpect(status().isForbidden());
    }

    /**
     * Verifies that {@code POST /api/carriera/prenotazioni} returns {@code 403} when no
     * {@code Authorization} header is present.
     */
    @Test
    @DisplayName("POST /api/carriera/prenotazioni returns 403 without token")
    void prenotazioniRequiresAuth() throws Exception {
      mockMvc.perform(post("/api/carriera/prenotazioni")
              .contentType("application/json")
              .content("{\"password\":\"secret\"}"))
          .andExpect(status().isForbidden());
    }
  }
}