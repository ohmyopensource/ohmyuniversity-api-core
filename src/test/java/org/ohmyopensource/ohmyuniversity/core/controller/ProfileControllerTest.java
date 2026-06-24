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
import org.ohmyopensource.ohmyuniversity.core.config.JwtAuthenticationFilter;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3.ProfileController;
import org.ohmyopensource.ohmyuniversity.core.dto.BadgeResponse;
import org.ohmyopensource.ohmyuniversity.core.service.esse3.ProfileService;
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
 * Unit tests for {@link ProfileController} using {@link MockMvc}.
 *
 * <p>The Spring Security filter chain is disabled via
 * {@link AutoConfigureMockMvc#addFilters()} so that only the controller layer is exercised.
 * {@link ProfileService} is replaced by a Mockito mock.
 */
@WebMvcTest(controllers = ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private MockMvc mockMvc;

  @MockitoBean
  private ProfileService profileService;

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
   * Verifies the HTTP contract of {@code GET /api/v1/profile/badge}.
   */
  @Nested
  @DisplayName("GET /api/v1/profile/badge")
  class Badge {

    /**
     * Verifies that a successful call to {@link ProfileService#getBadge} produces a
     * {@code 200 OK} response containing the expected badge fields.
     */
    @Test
    @DisplayName("returns 200 with badge data")
    void returns200() throws Exception {
      BadgeResponse badge = new BadgeResponse();
      badge.setBdgId(39498L);
      badge.setNome("ALESSIO");
      badge.setCognome("DEL MUTO");
      badge.setMatricola("178026");
      badge.setFrontImagePresent(false);
      badge.setRearImagePresent(false);

      when(profileService.getBadge(any())).thenReturn(badge);

      mockMvc.perform(get("/api/v1/profile/badge").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.bdgId").value(39498))
          .andExpect(jsonPath("$.nome").value("ALESSIO"))
          .andExpect(jsonPath("$.cognome").value("DEL MUTO"));
    }

    /**
     * Verifies that when {@link ProfileService#getBadge} returns {@code null}
     * the controller produces a {@code 404 Not Found} response.
     */
    @Test
    @DisplayName("returns 404 when no badge found")
    void returns404WhenNoBadge() throws Exception {
      when(profileService.getBadge(any())).thenReturn(null);

      mockMvc.perform(get("/api/v1/profile/badge").with(auth()))
          .andExpect(status().isNotFound());
    }

    /**
     * Verifies that a {@link CinecaAuthException} is mapped to {@code 401}.
     */
    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(profileService.getBadge(any()))
          .thenThrow(new CinecaAuthException("expired"));

      mockMvc.perform(get("/api/v1/profile/badge").with(auth()))
          .andExpect(status().isUnauthorized());
    }
  }
}