package org.ohmyopensource.ohmyuniversity.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.ohmyopensource.ohmyuniversity.core.dto.AppelloResponse;
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
 * Unit tests for {@link CarrieraController} using MockMvc.
 *
 * Spring Security filter chain is disabled — only the controller layer is tested.
 * CarrieraService is mocked. The OmuPrincipal is injected via SecurityMockMvc.
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

  // Injects the OmuPrincipal into the MockMvc security context
  private RequestPostProcessor auth() {
    return SecurityMockMvcRequestPostProcessors.authentication(
        new UsernamePasswordAuthenticationToken(
            principal, null,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENTE"))));
  }

  // ================================
  // GET /api/carriera/libretto
  // ================================

  @Nested
  @DisplayName("GET /api/carriera/libretto")
  class Libretto {

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

    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401OnExpiredSession() throws Exception {
      when(carrieraService.getLibretto(any()))
          .thenThrow(new CinecaAuthException("Session expired"));

      mockMvc.perform(get("/api/carriera/libretto").with(auth()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503WhenCinecaDown() throws Exception {
      when(carrieraService.getLibretto(any()))
          .thenThrow(new CinecaUnavailableException("Cineca down"));

      mockMvc.perform(get("/api/carriera/libretto").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }

  // ================================
  // GET /api/carriera/medie
  // ================================

  @Nested
  @DisplayName("GET /api/carriera/medie")
  class Medie {

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

    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(carrieraService.getMedia(any()))
          .thenThrow(new CinecaAuthException("expired"));

      mockMvc.perform(get("/api/carriera/medie").with(auth()))
          .andExpect(status().isUnauthorized());
    }
  }

  // ================================
  // GET /api/carriera/piano
  // ================================

  @Nested
  @DisplayName("GET /api/carriera/piano")
  class Piano {

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

    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503() throws Exception {
      when(carrieraService.getPiano(any()))
          .thenThrow(new CinecaUnavailableException("down"));

      mockMvc.perform(get("/api/carriera/piano").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }

  // ================================
  // POST /api/carriera/prenotazioni
  // ================================

  @Nested
  @DisplayName("POST /api/carriera/prenotazioni")
  class Prenotazioni {

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

    @Test
    @DisplayName("returns 400 when password is missing")
    void returns400WhenPasswordMissing() throws Exception {
      mockMvc.perform(post("/api/carriera/prenotazioni")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"password\": \"\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("returns 400 when body is empty")
    void returns400WhenBodyEmpty() throws Exception {
      mockMvc.perform(post("/api/carriera/prenotazioni")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest());
    }

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

  // ================================
  // GET /api/carriera/tasse
  // ================================

  @Nested
  @DisplayName("GET /api/carriera/tasse")
  class Tasse {

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

    @Test
    @DisplayName("returns 401 when Cineca session expired")
    void returns401() throws Exception {
      when(carrieraService.getTasse(any()))
          .thenThrow(new CinecaAuthException("expired"));

      mockMvc.perform(get("/api/carriera/tasse").with(auth()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("returns 503 when Cineca unavailable")
    void returns503() throws Exception {
      when(carrieraService.getTasse(any()))
          .thenThrow(new CinecaUnavailableException("down"));

      mockMvc.perform(get("/api/carriera/tasse").with(auth()))
          .andExpect(status().isServiceUnavailable());
    }
  }

  // ================================
  // GET /api/carriera/badge
  // ================================

  @Nested
  @DisplayName("GET /api/carriera/badge")
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

      when(carrieraService.getBadge(any())).thenReturn(badge);

      mockMvc.perform(get("/api/carriera/badge").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.bdgId").value(39498))
          .andExpect(jsonPath("$.nome").value("ALESSIO"))
          .andExpect(jsonPath("$.cognome").value("DEL MUTO"));
    }

    @Test
    @DisplayName("returns 404 when no badge found")
    void returns404WhenNoBadge() throws Exception {
      when(carrieraService.getBadge(any())).thenReturn(null);

      mockMvc.perform(get("/api/carriera/badge").with(auth()))
          .andExpect(status().isNotFound());
    }

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