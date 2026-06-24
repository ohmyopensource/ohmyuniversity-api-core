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
import org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3.CareerController;
import org.ohmyopensource.ohmyuniversity.core.dto.LibrettoResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.LibrettoResponse.RigaLibretto;
import org.ohmyopensource.ohmyuniversity.core.dto.MediaResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.PianoStudioResponse;
import org.ohmyopensource.ohmyuniversity.core.service.esse3.CareerService;
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
 * Unit tests for {@link CareerController} using {@link MockMvc}.
 *
 * <p>The Spring Security filter chain is disabled via
 * {@link AutoConfigureMockMvc#addFilters()} so that only the controller layer is exercised.
 * {@link CareerService} is replaced by a Mockito mock.
 */
@WebMvcTest(controllers = CareerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CareerControllerTest {

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private MockMvc mockMvc;

  @MockitoBean
  private CareerService careerService;

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
   * Verifies the HTTP contract of {@code GET /api/v1/career/transcript}.
   */
  @Nested
  @DisplayName("GET /api/v1/career/transcript")
  class Transcript {

    /**
     * Verifies that a successful call to {@link CareerService#getTranscript} produces a
     * {@code 200 OK} response containing the expected {@link RigaLibretto} fields.
     */
    @Test
    @DisplayName("returns 200 with righe")
    void returns200() throws Exception {
      RigaLibretto riga = new RigaLibretto();
      riga.setAdCod("411114");
      riga.setAdDes("Architettura degli elaboratori");
      riga.setVoto(29);
      riga.setSuperata(true);

      LibrettoResponse response = new LibrettoResponse();
      response.setRighe(List.of(riga));

      when(careerService.getTranscript(any())).thenReturn(response);

      mockMvc.perform(get("/api/v1/career/transcript").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.righe[0].adCod").value("411114"))
          .andExpect(jsonPath("$.righe[0].voto").value(29))
          .andExpect(jsonPath("$.righe[0].superata").value(true));
    }

    /**
     * Verifies that a {@link CinecaAuthException} is mapped to {@code 401}.
     */
    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401OnExpiredSession() throws Exception {
      when(careerService.getTranscript(any()))
          .thenThrow(new CinecaAuthException("Session expired"));

      mockMvc.perform(get("/api/v1/career/transcript").with(auth()))
          .andExpect(status().isUnauthorized());
    }

    /**
     * Verifies that a {@link CinecaUnavailableException} is mapped to {@code 503}.
     */
    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503WhenCinecaDown() throws Exception {
      when(careerService.getTranscript(any()))
          .thenThrow(new CinecaUnavailableException("Cineca down"));

      mockMvc.perform(get("/api/v1/career/transcript").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/career/grades}.
   */
  @Nested
  @DisplayName("GET /api/v1/career/grades")
  class Grades {

    /**
     * Verifies that a successful call to {@link CareerService#getGrades} produces a
     * {@code 200 OK} response containing the expected average fields.
     */
    @Test
    @DisplayName("returns 200 with averages")
    void returns200() throws Exception {
      MediaResponse response = new MediaResponse();
      response.setMediaAritmetica(26.17);
      response.setMediaPesata(25.84);
      response.setBaseMax110(95.94);
      response.setEsamiSuperati(21);
      response.setEsamiTotali(27);
      response.setCfu(139.0);
      response.setCfuTotali(181.0);
      response.setPercentualeCfu(76.8);

      when(careerService.getGrades(any())).thenReturn(response);

      mockMvc.perform(get("/api/v1/career/grades").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.mediaAritmetica").value(26.17))
          .andExpect(jsonPath("$.mediaPesata").value(25.84))
          .andExpect(jsonPath("$.baseMax110").value(95.94))
          .andExpect(jsonPath("$.esamiSuperati").value(21))
          .andExpect(jsonPath("$.percentualeCfu").value(76.8));
    }

    /**
     * Verifies that a {@link CinecaAuthException} is mapped to {@code 401}.
     */
    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(careerService.getGrades(any()))
          .thenThrow(new CinecaAuthException("expired"));

      mockMvc.perform(get("/api/v1/career/grades").with(auth()))
          .andExpect(status().isUnauthorized());
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/career/study-plan}.
   */
  @Nested
  @DisplayName("GET /api/v1/career/study-plan")
  class StudyPlan {

    /**
     * Verifies that a successful call to {@link CareerService#getStudyPlan} produces a
     * {@code 200 OK} response containing the expected activity fields.
     */
    @Test
    @DisplayName("returns 200 with righe piano")
    void returns200() throws Exception {
      PianoStudioResponse.RigaPiano riga = new PianoStudioResponse.RigaPiano();
      riga.setAdCod("411114");
      riga.setAdDes("Architettura degli elaboratori");
      riga.setCfu(6.0);
      riga.setAnnoCorso(1);

      PianoStudioResponse response = new PianoStudioResponse();
      response.setRighe(List.of(riga));

      when(careerService.getStudyPlan(any())).thenReturn(response);

      mockMvc.perform(get("/api/v1/career/study-plan").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.righe[0].adCod").value("411114"))
          .andExpect(jsonPath("$.righe[0].cfu").value(6.0));
    }

    /**
     * Verifies that a {@link CinecaUnavailableException} is mapped to {@code 503}.
     */
    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503() throws Exception {
      when(careerService.getStudyPlan(any()))
          .thenThrow(new CinecaUnavailableException("down"));

      mockMvc.perform(get("/api/v1/career/study-plan").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }
}