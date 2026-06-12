package org.ohmyopensource.ohmyuniversity.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.config.JwtAuthenticationFilter;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry.UniversityConfig;
import org.ohmyopensource.ohmyuniversity.core.controller.v1.ExternalServicesController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Unit tests for {@link ExternalServicesController} using {@link MockMvc}.
 *
 * <p>The Spring Security filter chain is disabled via
 * {@link AutoConfigureMockMvc#addFilters()} so that only the controller layer is exercised.
 * {@link UniversityRegistry} is replaced by a Mockito mock; the authenticated {@link OmuPrincipal}
 * is injected into each request through
 * {@link SecurityMockMvcRequestPostProcessors#authentication}.
 */
@WebMvcTest(controllers = ExternalServicesController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExternalServicesControllerTest {

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private MockMvc mockMvc;

  @MockitoBean
  private UniversityRegistry universityRegistry;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  private OmuPrincipal principal;

  /**
   * Initialises a test {@link OmuPrincipal} injected into the {@link MockMvc} security context
   * before each test via {@link #auth()}.
   */
  @BeforeEach
  void setUp() {
    principal = new OmuPrincipal(
        UUID.randomUUID().toString(),
        "TSTXXX00A00X000X",
        "UNIMOL",
        89486L,
        106279L,
        "178026");
  }

  /**
   * Returns a {@link RequestPostProcessor} that wraps {@link #principal} in a
   * {@link UsernamePasswordAuthenticationToken} and registers it as the current security context
   * principal for the outgoing {@link MockMvc} request.
   *
   * @return a post-processor that injects the test principal into the request
   */
  private RequestPostProcessor auth() {
    return SecurityMockMvcRequestPostProcessors.authentication(
        new UsernamePasswordAuthenticationToken(
            principal, null,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENTE"))));
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/university/external-services}, covering
   * successful retrieval and unknown university.
   */
  @Nested
  @DisplayName("GET /api/v1/university/external-services")
  class GetExternalServices {

    /**
     * Verifies that when {@link UniversityRegistry#resolve} returns a matching
     * {@link UniversityConfig} for the authenticated student's university, the controller produces
     * a {@code 200 OK} response whose JSON body contains the expected university name, Moodle URL,
     * and library URL.
     */
    @Test
    @DisplayName("returns 200 with university service URLs")
    void returns200() throws Exception {
      UniversityConfig config = new UniversityConfig(
          "Università degli Studi del Molise",
          "https://unimol.esse3.cineca.it/e3rest/api",
          "https://moodle.unimol.it",
          "https://biblioteche.unimol.it");

      when(universityRegistry.resolve(any())).thenReturn(Optional.of(config));

      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              principal, null,
              List.of(new SimpleGrantedAuthority("ROLE_STUDENTE"))));

      mockMvc.perform(get("/api/v1/university/external-services").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value("Università degli Studi del Molise"))
          .andExpect(jsonPath("$.moodleUrl").value("https://moodle.unimol.it"))
          .andExpect(jsonPath("$.libraryUrl").value("https://biblioteche.unimol.it"));
    }

    /**
     * Verifies that when {@link UniversityRegistry#resolve} returns an empty {@link Optional} —
     * indicating that the authenticated student's university is not registered in configuration —
     * the controller produces a {@code 404 Not Found} response.
     */
    @Test
    @DisplayName("returns 404 when university not registered")
    void returns404WhenUnknownUniversity() throws Exception {
      when(universityRegistry.resolve("UNIMOL")).thenReturn(Optional.empty());

      mockMvc.perform(get("/api/v1/university/external-services").with(auth()))
          .andExpect(status().isNotFound());
    }
  }
}