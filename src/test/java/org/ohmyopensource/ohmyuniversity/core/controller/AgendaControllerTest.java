package org.ohmyopensource.ohmyuniversity.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.config.JwtAuthenticationFilter;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.controller.v1.AgendaController;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CalendarEventType;
import org.ohmyopensource.ohmyuniversity.core.dto.agenda.AgendaEventResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.agenda.UniversityEventResponse;
import org.ohmyopensource.ohmyuniversity.core.service.AgendaService;
import org.ohmyopensource.ohmyuniversity.core.service.AgendaService.EventAlreadyImportedException;
import org.ohmyopensource.ohmyuniversity.core.service.AgendaService.EventNotFoundException;
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
 * Unit tests for {@link AgendaController} using {@link MockMvc}.
 *
 * <p>The Spring Security filter chain is disabled via
 * {@link AutoConfigureMockMvc#addFilters()} so that only the controller layer is exercised.
 * {@link AgendaService} is replaced by a Mockito mock; the authenticated {@link OmuPrincipal} is
 * injected into each request through {@link SecurityMockMvcRequestPostProcessors#authentication}.
 */
@WebMvcTest(controllers = AgendaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AgendaControllerTest {

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private MockMvc mockMvc;

  @MockitoBean
  private AgendaService agendaService;

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
        "178026",
        true);
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

  private AgendaEventResponse sampleEventResponse() {
    AgendaEventResponse r = new AgendaEventResponse();
    r.setId(UUID.randomUUID().toString());
    r.setTitle("Esame Sistemi");
    r.setStartDate("2026-07-15T09:00:00Z");
    r.setType(CalendarEventType.EXAM);
    r.setAllDay(false);
    return r;
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/agenda/events}.
   */
  @Nested
  @DisplayName("GET /api/v1/agenda/events")
  class GetEvents {

    /**
     * Verifies that a successful call produces a {@code 200 OK} response with events.
     */
    @Test
    @DisplayName("returns 200 with events list")
    void returns200() throws Exception {
      when(agendaService.getEvents(any(), any(), any()))
          .thenReturn(List.of(sampleEventResponse()));

      mockMvc.perform(get("/api/v1/agenda/events").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].title").value("Esame Sistemi"));
    }
  }

  /**
   * Verifies the HTTP contract of {@code POST /api/v1/agenda/events}.
   */
  @Nested
  @DisplayName("POST /api/v1/agenda/events")
  class CreateEvent {

    /**
     * Verifies that a successful creation produces a {@code 200 OK} response.
     */
    @Test
    @DisplayName("returns 200 on success")
    void returns200() throws Exception {
      when(agendaService.createEvent(any(), any())).thenReturn(sampleEventResponse());

      mockMvc.perform(post("/api/v1/agenda/events")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"title\":\"Esame Sistemi\",\"startDate\":\"2026-07-15T09:00:00Z\","
                  + "\"type\":\"EXAM\",\"allDay\":false}"))
          .andExpect(status().isCreated());
    }
  }

  /**
   * Verifies the HTTP contract of {@code PUT /api/v1/agenda/events/{id}}.
   */
  @Nested
  @DisplayName("PUT /api/v1/agenda/events/{id}")
  class UpdateEvent {

    /**
     * Verifies that a successful update produces a {@code 200 OK} response.
     */
    @Test
    @DisplayName("returns 200 on success")
    void returns200() throws Exception {
      when(agendaService.updateEvent(any(), any(), any())).thenReturn(sampleEventResponse());

      mockMvc.perform(put("/api/v1/agenda/events/" + UUID.randomUUID())
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"title\":\"Updated\",\"startDate\":\"2026-07-15T09:00:00Z\","
                  + "\"type\":\"EXAM\",\"allDay\":false}"))
          .andExpect(status().isOk());
    }

    /**
     * Verifies that an {@link EventNotFoundException} produces a {@code 404 Not Found} response.
     */
    @Test
    @DisplayName("returns 404 when event not found")
    void returns404() throws Exception {
      when(agendaService.updateEvent(any(), any(), any()))
          .thenThrow(new EventNotFoundException("not found"));

      mockMvc.perform(put("/api/v1/agenda/events/" + UUID.randomUUID())
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"title\":\"Updated\",\"startDate\":\"2026-07-15T09:00:00Z\","
                  + "\"type\":\"EXAM\",\"allDay\":false}"))
          .andExpect(status().isNotFound());
    }
  }

  /**
   * Verifies the HTTP contract of {@code DELETE /api/v1/agenda/events/{id}}.
   */
  @Nested
  @DisplayName("DELETE /api/v1/agenda/events/{id}")
  class DeleteEvent {

    /**
     * Verifies that a successful deletion produces a {@code 204 No Content} response.
     */
    @Test
    @DisplayName("returns 204 on success")
    void returns204() throws Exception {
      doNothing().when(agendaService).deleteEvent(any(), any());

      mockMvc.perform(delete("/api/v1/agenda/events/" + UUID.randomUUID()).with(auth()))
          .andExpect(status().isNoContent());
    }

    /**
     * Verifies that an {@link EventNotFoundException} produces a {@code 404 Not Found} response.
     */
    @Test
    @DisplayName("returns 404 when event not found")
    void returns404() throws Exception {
      doThrow(new EventNotFoundException("not found"))
          .when(agendaService).deleteEvent(any(), any());

      mockMvc.perform(delete("/api/v1/agenda/events/" + UUID.randomUUID()).with(auth()))
          .andExpect(status().isNotFound());
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/agenda/university-events}.
   */
  @Nested
  @DisplayName("GET /api/v1/agenda/university-events")
  class GetUniversityEvents {

    /**
     * Verifies that a successful call produces a {@code 200 OK} response with events.
     */
    @Test
    @DisplayName("returns 200 with university events")
    void returns200() throws Exception {
      UniversityEventResponse ue = new UniversityEventResponse();
      ue.setTitle("Apertura iscrizioni");
      when(agendaService.getUniversityEvents(any())).thenReturn(List.of(ue));

      mockMvc.perform(get("/api/v1/agenda/university-events").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].title").value("Apertura iscrizioni"));
    }
  }

  /**
   * Verifies the HTTP contract of {@code POST /api/v1/agenda/university-events/{id}/import}.
   *
   * <p>{@link AgendaService#importUniversityEvent} returns {@code void}, so {@code doNothing}
   * and {@code doThrow} are used instead of {@code when(...).thenReturn(...)}.
   */
  @Nested
  @DisplayName("POST /api/v1/agenda/university-events/{id}/import")
  class ImportUniversityEvent {

    /**
     * Verifies that a successful import produces a {@code 204 No Content} response.
     */
    @Test
    @DisplayName("returns 204 on success")
    void returns204() throws Exception {
      doNothing().when(agendaService).importUniversityEvent(any(), any());

      mockMvc.perform(post("/api/v1/agenda/university-events/" + UUID.randomUUID() + "/import")
              .with(auth()))
          .andExpect(status().isNoContent());
    }

    /**
     * Verifies that an {@link EventAlreadyImportedException} produces a {@code 409 Conflict}.
     */
    @Test
    @DisplayName("returns 409 when already imported")
    void returns409() throws Exception {
      doThrow(new EventAlreadyImportedException("already imported"))
          .when(agendaService).importUniversityEvent(any(), any());

      mockMvc.perform(post("/api/v1/agenda/university-events/" + UUID.randomUUID() + "/import")
              .with(auth()))
          .andExpect(status().isConflict());
    }
  }
}