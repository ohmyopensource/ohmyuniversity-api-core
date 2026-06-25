package org.ohmyopensource.ohmyuniversity.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3.ProfileController;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BadgeResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.CareerInfoResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.PersonaResponse;
import org.ohmyopensource.ohmyuniversity.core.service.esse3.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Unit tests for {@link ProfileController} using {@link MockMvc}.
 *
 * <p>Covers all four endpoints: persona, info, avatar, badge.
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
        "DLMLSS04E14L113Q",
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

  @Nested
  @DisplayName("GET /api/v1/profile/persona")
  class Persona {

    @Test
    @DisplayName("returns 200 with persona data")
    void returns200() throws Exception {
      PersonaResponse response = new PersonaResponse();
      response.setPersId(92533L);
      response.setNome("ALESSIO");
      response.setCognome("DEL MUTO");
      response.setCodFis("DLMLSS04E14L113Q");
      when(profileService.getPersona(any())).thenReturn(response);

      mockMvc.perform(get("/api/v1/profile/persona").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.persId").value(92533))
          .andExpect(jsonPath("$.nome").value("ALESSIO"))
          .andExpect(jsonPath("$.cognome").value("DEL MUTO"));
    }

    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(profileService.getPersona(any())).thenThrow(new CinecaAuthException("expired"));
      mockMvc.perform(get("/api/v1/profile/persona").with(auth()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503() throws Exception {
      when(profileService.getPersona(any())).thenThrow(new CinecaUnavailableException("down"));
      mockMvc.perform(get("/api/v1/profile/persona").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/profile/info")
  class Info {

    @Test
    @DisplayName("returns 200 with career info")
    void returns200() throws Exception {
      CareerInfoResponse response = new CareerInfoResponse();
      response.setStuId(89486L);
      response.setMatId(106279L);
      response.setMatricola("178026");
      response.setTipoCorsoCod("L2");
      response.setCdsDes("INFORMATICA");
      response.setAnnoCorso(3);
      when(profileService.getInfo(any())).thenReturn(response);

      mockMvc.perform(get("/api/v1/profile/info").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.stuId").value(89486))
          .andExpect(jsonPath("$.tipoCorsoCod").value("L2"))
          .andExpect(jsonPath("$.cdsDes").value("INFORMATICA"));
    }

    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(profileService.getInfo(any())).thenThrow(new CinecaAuthException("expired"));
      mockMvc.perform(get("/api/v1/profile/info").with(auth()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503() throws Exception {
      when(profileService.getInfo(any())).thenThrow(new CinecaUnavailableException("down"));
      mockMvc.perform(get("/api/v1/profile/info").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/profile/avatar")
  class Avatar {

    @Test
    @DisplayName("returns 200 with JPEG bytes")
    void returns200() throws Exception {
      byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
      when(profileService.getAvatar(any())).thenReturn(jpeg);

      mockMvc.perform(get("/api/v1/profile/avatar").with(auth()))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.IMAGE_JPEG))
          .andExpect(content().bytes(jpeg));
    }

    @Test
    @DisplayName("returns 404 when no avatar available")
    void returns404WhenNull() throws Exception {
      when(profileService.getAvatar(any())).thenReturn(null);
      mockMvc.perform(get("/api/v1/profile/avatar").with(auth()))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(profileService.getAvatar(any())).thenThrow(new CinecaAuthException("expired"));
      mockMvc.perform(get("/api/v1/profile/avatar").with(auth()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503() throws Exception {
      when(profileService.getAvatar(any())).thenThrow(new CinecaUnavailableException("down"));
      mockMvc.perform(get("/api/v1/profile/avatar").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/profile/badge")
  class Badge {

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
          .andExpect(jsonPath("$.nome").value("ALESSIO"));
    }

    @Test
    @DisplayName("returns 404 when no badge found")
    void returns404WhenNoBadge() throws Exception {
      when(profileService.getBadge(any())).thenReturn(null);
      mockMvc.perform(get("/api/v1/profile/badge").with(auth()))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(profileService.getBadge(any())).thenThrow(new CinecaAuthException("expired"));
      mockMvc.perform(get("/api/v1/profile/badge").with(auth()))
          .andExpect(status().isUnauthorized());
    }
  }
}