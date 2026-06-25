package org.ohmyopensource.ohmyuniversity.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3.ExamsController;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookableSessionsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookableSessionsResponse.AppelloLibretto;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookingsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookingsResponse.IscrizioneAppello;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.LegacyBookingsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.LegacyBookingsResponse.Prenotazione;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SessionsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SessionsResponse.Appello;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveysResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveysResponse.QuestionarioEsame;
import org.ohmyopensource.ohmyuniversity.core.service.esse3.ExamsService;
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
 * Unit tests for {@link ExamsController} using {@link MockMvc}.
 *
 * <p>The Spring Security filter chain is disabled via
 * {@link AutoConfigureMockMvc#addFilters()} so that only the controller layer is exercised.
 * {@link ExamsService} is replaced by a Mockito mock.
 */
@WebMvcTest(controllers = ExamsController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExamsControllerTest {

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private MockMvc mockMvc;

  @MockitoBean
  private ExamsService examsService;

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
   * Verifies the HTTP contract of {@code GET /api/v1/exams/sessions}.
   */
  @Nested
  @DisplayName("GET /api/v1/exams/sessions")
  class Sessions {

    /**
     * Verifies that a successful call produces {@code 200 OK} with session data.
     */
    @Test
    @DisplayName("returns 200 with sessions")
    void returns200() throws Exception {
      Appello appello = new Appello();
      appello.setAppId(1001L);
      appello.setAdCod("411114");
      appello.setAdDes("Architettura degli elaboratori");
      appello.setPrenotabile(true);

      SessionsResponse response = new SessionsResponse();
      response.setAppelli(List.of(appello));

      when(examsService.getSessions(any(), any(), any())).thenReturn(response);

      mockMvc.perform(get("/api/v1/exams/sessions")
              .param("cdsId", "10018")
              .param("adId", "5001")
              .with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.appelli[0].appId").value(1001))
          .andExpect(jsonPath("$.appelli[0].adCod").value("411114"))
          .andExpect(jsonPath("$.appelli[0].prenotabile").value(true));
    }

    /**
     * Verifies that a {@link CinecaAuthException} is mapped to {@code 401}.
     */
    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(examsService.getSessions(any(), any(), any()))
          .thenThrow(new CinecaAuthException("expired"));

      mockMvc.perform(get("/api/v1/exams/sessions")
              .param("cdsId", "10018")
              .param("adId", "5001")
              .with(auth()))
          .andExpect(status().isUnauthorized());
    }

    /**
     * Verifies that a {@link CinecaUnavailableException} is mapped to {@code 503}.
     */
    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503() throws Exception {
      when(examsService.getSessions(any(), any(), any()))
          .thenThrow(new CinecaUnavailableException("down"));

      mockMvc.perform(get("/api/v1/exams/sessions")
              .param("cdsId", "10018")
              .param("adId", "5001")
              .with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/exams/bookable}.
   */
  @Nested
  @DisplayName("GET /api/v1/exams/bookable")
  class BookableSessions {

    /**
     * Verifies that a successful call produces {@code 200 OK} with bookable sessions.
     */
    @Test
    @DisplayName("returns 200 with bookable sessions")
    void returns200() throws Exception {
      AppelloLibretto appello = new AppelloLibretto();
      appello.setAppId(2001L);
      appello.setAdCod("411114");
      appello.setDocente("MARIO ROSSI");

      BookableSessionsResponse response = new BookableSessionsResponse();
      response.setAppelli(List.of(appello));

      when(examsService.getBookableSessions(any())).thenReturn(response);

      mockMvc.perform(get("/api/v1/exams/bookable").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.appelli[0].appId").value(2001))
          .andExpect(jsonPath("$.appelli[0].adCod").value("411114"))
          .andExpect(jsonPath("$.appelli[0].docente").value("MARIO ROSSI"));
    }

    /**
     * Verifies that a {@link CinecaAuthException} is mapped to {@code 401}.
     */
    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(examsService.getBookableSessions(any()))
          .thenThrow(new CinecaAuthException("expired"));

      mockMvc.perform(get("/api/v1/exams/bookable").with(auth()))
          .andExpect(status().isUnauthorized());
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/exams/bookings}.
   */
  @Nested
  @DisplayName("GET /api/v1/exams/bookings")
  class Bookings {

    /**
     * Verifies that a successful call produces {@code 200 OK} with active bookings.
     */
    @Test
    @DisplayName("returns 200 with active bookings")
    void returns200() throws Exception {
      IscrizioneAppello iscrizione = new IscrizioneAppello();
      iscrizione.setApplistaId(999L);
      iscrizione.setAdStuCod("411114");
      iscrizione.setDataOraTurno("31/12/2099 09:00:00");

      BookingsResponse response = new BookingsResponse();
      response.setPrenotazioni(List.of(iscrizione));

      when(examsService.getBookings(any())).thenReturn(response);

      mockMvc.perform(get("/api/v1/exams/bookings").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.prenotazioni[0].applistaId").value(999))
          .andExpect(jsonPath("$.prenotazioni[0].adStuCod").value("411114"));
    }

    /**
     * Verifies that a {@link CinecaUnavailableException} is mapped to {@code 503}.
     */
    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503() throws Exception {
      when(examsService.getBookings(any()))
          .thenThrow(new CinecaUnavailableException("down"));

      mockMvc.perform(get("/api/v1/exams/bookings").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }

  /**
   * Verifies the HTTP contract of {@code POST /api/v1/exams/bookings/legacy}.
   */
  @Nested
  @DisplayName("POST /api/v1/exams/bookings/legacy")
  class LegacyBookings {

    /**
     * Verifies that a valid request with password produces {@code 200 OK}.
     */
    @Test
    @DisplayName("returns 200 with booking history")
    void returns200() throws Exception {
      Prenotazione p = new Prenotazione();
      p.setApplistaId(1228134L);
      p.setAdStuCod("429008");

      LegacyBookingsResponse response = new LegacyBookingsResponse();
      response.setPrenotazioni(List.of(p));

      when(examsService.getLegacyBookings(any(), anyString())).thenReturn(response);

      mockMvc.perform(post("/api/v1/exams/bookings/legacy")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"password\": \"secret\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.prenotazioni[0].applistaId").value(1228134));
    }

    /**
     * Verifies that an empty password produces {@code 400 Bad Request}.
     */
    @Test
    @DisplayName("returns 400 when password is empty")
    void returns400WhenPasswordEmpty() throws Exception {
      mockMvc.perform(post("/api/v1/exams/bookings/legacy")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"password\": \"\"}"))
          .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that a missing password body produces {@code 400 Bad Request}.
     */
    @Test
    @DisplayName("returns 400 when body is empty")
    void returns400WhenBodyEmpty() throws Exception {
      mockMvc.perform(post("/api/v1/exams/bookings/legacy")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that a {@link CinecaAuthException} is mapped to {@code 401}.
     */
    @Test
    @DisplayName("returns 401 when Cineca auth fails")
    void returns401() throws Exception {
      when(examsService.getLegacyBookings(any(), anyString()))
          .thenThrow(new CinecaAuthException("auth failed"));

      mockMvc.perform(post("/api/v1/exams/bookings/legacy")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"password\": \"secret\"}"))
          .andExpect(status().isUnauthorized());
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/exams/surveys}.
   */
  @Nested
  @DisplayName("GET /api/v1/exams/surveys")
  class Surveys {

    /**
     * Verifies that a successful call produces {@code 200 OK} with survey data.
     */
    @Test
    @DisplayName("returns 200 with surveys split into pending and completed")
    void returns200() throws Exception {
      QuestionarioEsame pending = new QuestionarioEsame();
      pending.setAdCod("411114");
      pending.setAdDes("Architettura degli elaboratori");
      pending.setStatoLink(3);

      SurveysResponse response = new SurveysResponse();
      response.setDaCompilare(List.of(pending));
      response.setCompilati(List.of());

      when(examsService.getSurveys(any())).thenReturn(response);

      mockMvc.perform(get("/api/v1/exams/surveys").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.daCompilare[0].adCod").value("411114"))
          .andExpect(jsonPath("$.daCompilare[0].statoLink").value(3))
          .andExpect(jsonPath("$.compilati").isEmpty());
    }

    /**
     * Verifies that a {@link CinecaAuthException} is mapped to {@code 401}.
     */
    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(examsService.getSurveys(any()))
          .thenThrow(new CinecaAuthException("expired"));

      mockMvc.perform(get("/api/v1/exams/surveys").with(auth()))
          .andExpect(status().isUnauthorized());
    }

    /**
     * Verifies that a {@link CinecaUnavailableException} is mapped to {@code 503}.
     */
    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503() throws Exception {
      when(examsService.getSurveys(any()))
          .thenThrow(new CinecaUnavailableException("down"));

      mockMvc.perform(get("/api/v1/exams/surveys").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }
}