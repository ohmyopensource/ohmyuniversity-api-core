package org.ohmyopensource.ohmyuniversity.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaUnavailableException;
import org.ohmyopensource.ohmyuniversity.core.config.JwtAuthenticationFilter;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3.FeesController;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.FeeStatusResponse;
import org.ohmyopensource.ohmyuniversity.core.service.esse3.FeesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Unit tests for {@link FeesController} using {@link MockMvc}.
 *
 * <p>The Spring Security filter chain is disabled via
 * {@link AutoConfigureMockMvc#addFilters()} so that only the controller layer is exercised.
 * {@link FeesService} is replaced by a Mockito mock.
 */
@WebMvcTest(controllers = FeesController.class)
@AutoConfigureMockMvc(addFilters = false)
class FeesControllerTest {

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private MockMvc mockMvc;

  @MockitoBean
  private FeesService feesService;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  private OmuPrincipal principal;

  @BeforeEach
  void setUp() {
    principal = new OmuPrincipal(
        UUID.randomUUID().toString(),
        "TSTXXX00A00X000X",
        "UNIMOL",
        89486L,
        106279L,
        "178026",
        true);
  }

  private RequestPostProcessor auth() {
    return SecurityMockMvcRequestPostProcessors.authentication(
        new UsernamePasswordAuthenticationToken(
            principal, null,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENTE"))));
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/fees/status}.
   */
  @Nested
  @DisplayName("GET /api/v1/fees/status")
  class FeesStatus {

    /**
     * Verifies that a successful call to {@link FeesService#getStatus} produces a
     * {@code 200 OK} response containing the expected fee status fields.
     */
    @Test
    @DisplayName("returns 200 with semaforo VERDE")
    void returns200() throws Exception {
      FeeStatusResponse response = new FeeStatusResponse();
      response.setSemaforo("VERDE");
      response.setImportoDovuto("0.0");
      response.setTasseScadute(List.of());
      response.setTasseDovute(List.of());
      response.setAddebiti(List.of());

      when(feesService.getStatus(any())).thenReturn(response);

      mockMvc.perform(get("/api/v1/fees/status").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.semaforo").value("VERDE"))
          .andExpect(jsonPath("$.importoDovuto").value("0.0"));
    }

    /**
     * Verifies that a {@link CinecaAuthException} is mapped to {@code 401}.
     */
    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(feesService.getStatus(any()))
          .thenThrow(new CinecaAuthException("expired"));

      mockMvc.perform(get("/api/v1/fees/status").with(auth()))
          .andExpect(status().isUnauthorized());
    }

    /**
     * Verifies that a {@link CinecaUnavailableException} is mapped to {@code 503}.
     */
    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503() throws Exception {
      when(feesService.getStatus(any()))
          .thenThrow(new CinecaUnavailableException("down"));

      mockMvc.perform(get("/api/v1/fees/status").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }
}