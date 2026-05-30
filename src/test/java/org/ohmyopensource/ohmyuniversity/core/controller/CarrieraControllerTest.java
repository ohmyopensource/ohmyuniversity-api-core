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
import org.ohmyopensource.ohmyuniversity.core.dto.BadgeResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.LibrettoResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.LibrettoResponse.RigaLibretto;
import org.ohmyopensource.ohmyuniversity.core.dto.MediaResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.PianoStudioResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.PrenotazioneResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.TasseResponse;
import org.ohmyopensource.ohmyuniversity.core.service.CarrieraService;
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
 * Unit tests for {@link CarrieraController} using {@link MockMvc}.
 *
 * <p>The Spring Security filter chain is disabled via
 * {@link AutoConfigureMockMvc#addFilters()} so that only the controller layer is exercised.
 * {@link CarrieraService} is replaced by a Mockito mock; the authenticated {@link OmuPrincipal} is
 * injected into each request through {@link SecurityMockMvcRequestPostProcessors#authentication}.
 */
@WebMvcTest(controllers = CarrieraController.class)
@AutoConfigureMockMvc(addFilters = false)
class CarrieraControllerTest {

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private MockMvc mockMvc;

  @MockitoBean
  private CarrieraService carrieraService;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  private OmuPrincipal principal;

  /**
   * Initialises a test {@link OmuPrincipal} that is injected into the {@link MockMvc} security
   * context before each test via {@link #auth()}.
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
   * Verifies the HTTP contract of {@code GET /api/carriera/libretto}, covering successful
   * retrieval, Cineca session expiry, and service unavailability.
   */
  @Nested
  @DisplayName("GET /api/carriera/libretto")
  class Libretto {

    /**
     * Verifies that a successful call to {@link CarrieraService#getLibretto} produces a
     * {@code 200 OK} response whose JSON body contains the expected {@link RigaLibretto} fields.
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

      when(carrieraService.getLibretto(any())).thenReturn(response);

      mockMvc.perform(get("/api/carriera/libretto").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.righe[0].adCod").value("411114"))
          .andExpect(jsonPath("$.righe[0].voto").value(29))
          .andExpect(jsonPath("$.righe[0].superata").value(true));
    }

    /**
     * Verifies that a {@link CinecaAuthException} thrown by {@link CarrieraService#getLibretto} —
     * indicating an expired Cineca session — is mapped to a {@code 401 Unauthorized} response.
     */
    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401OnExpiredSession() throws Exception {
      when(carrieraService.getLibretto(any()))
          .thenThrow(new CinecaAuthException("Session expired"));

      mockMvc.perform(get("/api/carriera/libretto").with(auth()))
          .andExpect(status().isUnauthorized());
    }

    /**
     * Verifies that a {@link CinecaUnavailableException} thrown by
     * {@link CarrieraService#getLibretto} — indicating that the Cineca ESSE3 backend is unreachable
     * — is mapped to a {@code 503 Service Unavailable} response.
     */
    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503WhenCinecaDown() throws Exception {
      when(carrieraService.getLibretto(any()))
          .thenThrow(new CinecaUnavailableException("Cineca down"));

      mockMvc.perform(get("/api/carriera/libretto").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/carriera/medie}, covering successful retrieval of
   * grade averages and Cineca session expiry.
   */
  @Nested
  @DisplayName("GET /api/carriera/medie")
  class Medie {

    /**
     * Verifies that a successful call to {@link CarrieraService#getMedia} produces a {@code 200 OK}
     * response whose JSON body contains the expected arithmetic average, weighted average,
     * projected graduation score, and CFU completion percentage fields of {@link MediaResponse}.
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

      when(carrieraService.getMedia(any())).thenReturn(response);

      mockMvc.perform(get("/api/carriera/medie").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.mediaAritmetica").value(26.17))
          .andExpect(jsonPath("$.mediaPesata").value(25.84))
          .andExpect(jsonPath("$.baseMax110").value(95.94))
          .andExpect(jsonPath("$.esamiSuperati").value(21))
          .andExpect(jsonPath("$.percentualeCfu").value(76.8));
    }

    /**
     * Verifies that a {@link CinecaAuthException} thrown by {@link CarrieraService#getMedia} —
     * indicating an expired Cineca session — is mapped to a {@code 401 Unauthorized} response.
     */
    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(carrieraService.getMedia(any()))
          .thenThrow(new CinecaAuthException("expired"));

      mockMvc.perform(get("/api/carriera/medie").with(auth()))
          .andExpect(status().isUnauthorized());
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/carriera/piano}, covering successful retrieval of
   * the study plan and service unavailability.
   */
  @Nested
  @DisplayName("GET /api/carriera/piano")
  class Piano {

    /**
     * Verifies that a successful call to {@link CarrieraService#getPiano} produces a {@code 200 OK}
     * response whose JSON body contains the expected {@link PianoStudioResponse.RigaPiano} fields,
     * including the activity code and the number of CFU credits.
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

      when(carrieraService.getPiano(any())).thenReturn(response);

      mockMvc.perform(get("/api/carriera/piano").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.righe[0].adCod").value("411114"))
          .andExpect(jsonPath("$.righe[0].cfu").value(6.0));
    }

    /**
     * Verifies that a {@link CinecaUnavailableException} thrown by {@link CarrieraService#getPiano}
     * — indicating that the Cineca ESSE3 backend is unreachable — is mapped to a
     * {@code 503 Service Unavailable} response.
     */
    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503() throws Exception {
      when(carrieraService.getPiano(any()))
          .thenThrow(new CinecaUnavailableException("down"));

      mockMvc.perform(get("/api/carriera/piano").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }

  /**
   * Verifies the HTTP contract of {@code POST /api/carriera/prenotazioni}, covering successful
   * retrieval of exam bookings, request body validation, and Cineca authentication failure.
   */
  @Nested
  @DisplayName("POST /api/carriera/prenotazioni")
  class Prenotazioni {

    /**
     * Verifies that a {@code POST} request with a valid JSON body containing a non-blank password
     * produces a {@code 200 OK} response whose JSON body contains the expected
     * {@link PrenotazioneResponse.Prenotazione} fields.
     */
    @Test
    @DisplayName("returns 200 with prenotazioni list")
    void returns200() throws Exception {
      PrenotazioneResponse.Prenotazione p = new PrenotazioneResponse.Prenotazione();
      p.setApplistaId(1228134L);
      p.setAdStuCod("429008");

      PrenotazioneResponse response = new PrenotazioneResponse();
      response.setPrenotazioni(List.of(p));

      when(carrieraService.getPrenotazioni(any(), anyString())).thenReturn(response);

      mockMvc.perform(post("/api/carriera/prenotazioni")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"password\": \"secret\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.prenotazioni[0].applistaId").value(1228134));
    }

    /**
     * Verifies that a {@code POST} request whose JSON body contains an empty {@code password} field
     * fails bean validation and produces a {@code 400 Bad Request} response without invoking
     * {@link CarrieraService#getPrenotazioni}.
     */
    @Test
    @DisplayName("returns 400 when password is missing")
    void returns400WhenPasswordMissing() throws Exception {
      mockMvc.perform(post("/api/carriera/prenotazioni")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"password\": \"\"}"))
          .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that a {@code POST} request whose JSON body is an empty object — omitting the
     * required {@code password} field entirely — fails bean validation and produces a
     * {@code 400 Bad Request} response without invoking {@link CarrieraService#getPrenotazioni}.
     */
    @Test
    @DisplayName("returns 400 when body is empty")
    void returns400WhenBodyEmpty() throws Exception {
      mockMvc.perform(post("/api/carriera/prenotazioni")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that a {@link CinecaAuthException} thrown by {@link CarrieraService#getPrenotazioni}
     * — indicating that the supplied credentials were rejected by Cineca — is mapped to a
     * {@code 401 Unauthorized} response.
     */
    @Test
    @DisplayName("returns 401 when Cineca auth fails")
    void returns401OnAuthFailure() throws Exception {
      when(carrieraService.getPrenotazioni(any(), anyString()))
          .thenThrow(new CinecaAuthException("auth failed"));

      mockMvc.perform(post("/api/carriera/prenotazioni")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"password\": \"secret\"}"))
          .andExpect(status().isUnauthorized());
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/carriera/tasse}, covering successful retrieval of
   * the tax status, Cineca session expiry, and service unavailability.
   */
  @Nested
  @DisplayName("GET /api/carriera/tasse")
  class Tasse {

    /**
     * Verifies that a successful call to {@link CarrieraService#getTasse} produces a {@code 200 OK}
     * response whose JSON body contains the expected {@link TasseResponse} fields, including the
     * traffic-light status {@code VERDE} and a zero outstanding amount.
     */
    @Test
    @DisplayName("returns 200 with semaforo VERDE")
    void returns200() throws Exception {
      TasseResponse response = new TasseResponse();
      response.setSemaforo("VERDE");
      response.setImportoDovuto("0.0");
      response.setTasseScadute(List.of());
      response.setTasseDovute(List.of());
      response.setAddebiti(List.of());

      when(carrieraService.getTasse(any())).thenReturn(response);

      mockMvc.perform(get("/api/carriera/tasse").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.semaforo").value("VERDE"))
          .andExpect(jsonPath("$.importoDovuto").value("0.0"));
    }

    /**
     * Verifies that a {@link CinecaAuthException} thrown by {@link CarrieraService#getTasse} —
     * indicating an expired Cineca session — is mapped to a {@code 401 Unauthorized} response.
     */
    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(carrieraService.getTasse(any()))
          .thenThrow(new CinecaAuthException("expired"));

      mockMvc.perform(get("/api/carriera/tasse").with(auth()))
          .andExpect(status().isUnauthorized());
    }

    /**
     * Verifies that a {@link CinecaUnavailableException} thrown by {@link CarrieraService#getTasse}
     * — indicating that the Cineca ESSE3 backend is unreachable — is mapped to a
     * {@code 503 Service Unavailable} response.
     */
    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503() throws Exception {
      when(carrieraService.getTasse(any()))
          .thenThrow(new CinecaUnavailableException("down"));

      mockMvc.perform(get("/api/carriera/tasse").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/carriera/badge}, covering successful retrieval of
   * badge data, the absence of a badge record, and Cineca session expiry.
   */
  @Nested
  @DisplayName("GET /api/carriera/badge")
  class Badge {

    /**
     * Verifies that a successful call to {@link CarrieraService#getBadge} produces a {@code 200 OK}
     * response whose JSON body contains the expected {@link BadgeResponse} fields, including badge
     * identifier, first name, and last name.
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

      when(carrieraService.getBadge(any())).thenReturn(badge);

      mockMvc.perform(get("/api/carriera/badge").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.bdgId").value(39498))
          .andExpect(jsonPath("$.nome").value("ALESSIO"))
          .andExpect(jsonPath("$.cognome").value("DEL MUTO"));
    }

    /**
     * Verifies that when {@link CarrieraService#getBadge} returns {@code null} — indicating that no
     * badge record exists for the authenticated student — the controller produces a
     * {@code 404 Not Found} response.
     */
    @Test
    @DisplayName("returns 404 when no badge found")
    void returns404WhenNoBadge() throws Exception {
      when(carrieraService.getBadge(any())).thenReturn(null);

      mockMvc.perform(get("/api/carriera/badge").with(auth()))
          .andExpect(status().isNotFound());
    }

    /**
     * Verifies that a {@link CinecaAuthException} thrown by {@link CarrieraService#getBadge} —
     * indicating an expired Cineca session — is mapped to a {@code 401 Unauthorized} response.
     */
    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(carrieraService.getBadge(any()))
          .thenThrow(new CinecaAuthException("expired"));

      mockMvc.perform(get("/api/carriera/badge").with(auth()))
          .andExpect(status().isUnauthorized());
    }
  }
}