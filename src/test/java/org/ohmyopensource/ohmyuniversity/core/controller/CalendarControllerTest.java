package org.ohmyopensource.ohmyuniversity.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.ohmyopensource.ohmyuniversity.core.controller.v1.CalendarController;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CalendarEventType;
import org.ohmyopensource.ohmyuniversity.core.dto.calendar.CalendarEventResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.calendar.UniversityEventResponse;
import org.ohmyopensource.ohmyuniversity.core.service.CalendarService;
import org.ohmyopensource.ohmyuniversity.core.service.CalendarService.EventAlreadyImportedException;
import org.ohmyopensource.ohmyuniversity.core.service.CalendarService.EventNotFoundException;
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
 * Unit tests for {@link CalendarController} using {@link MockMvc}.
 *
 * <p>The Spring Security filter chain is disabled via
 * {@link AutoConfigureMockMvc#addFilters()} so that only the controller layer is exercised.
 * {@link CalendarService} is replaced by a Mockito mock; the authenticated {@link OmuPrincipal} is
 * injected into each request through {@link SecurityMockMvcRequestPostProcessors#authentication}.
 */
@WebMvcTest(controllers = CalendarController.class)
@AutoConfigureMockMvc(addFilters = false)
class CalendarControllerTest {

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private MockMvc mockMvc;

  @MockitoBean
  private CalendarService calendarService;

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

  private CalendarEventResponse sampleEventResponse() {
    CalendarEventResponse r = new CalendarEventResponse();
    r.setId(UUID.randomUUID().toString());
    r.setTitle("Esame Sistemi");
    r.setStartDate("2026-07-15T09:00:00Z");
    r.setType(CalendarEventType.EXAM);
    r.setAllDay(false);
    r.setCreatedAt("2026-06-12T07:00:00Z");
    r.setUpdatedAt("2026-06-12T07:00:00Z");
    return r;
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/calendar/events}, covering successful
   * retrieval without filters and with date range filters.
   */
  @Nested
  @DisplayName("GET /api/v1/calendar/events")
  class GetEvents {

    /**
     * Verifies that a successful call to {@link CalendarService#getEvents} without date filters
     * produces a {@code 200 OK} response whose JSON body contains the expected event fields.
     */
    @Test
    @DisplayName("returns 200 with event list")
    void returns200() throws Exception {
      when(calendarService.getEvents(any(), any(), any()))
          .thenReturn(List.of(sampleEventResponse()));

      mockMvc.perform(get("/api/v1/calendar/events").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].title").value("Esame Sistemi"))
          .andExpect(jsonPath("$[0].type").value("EXAM"));
    }

    /**
     * Verifies that a successful call to {@link CalendarService#getEvents} with {@code from} and
     * {@code to} query parameters produces a {@code 200 OK} response containing the filtered
     * events.
     */
    @Test
    @DisplayName("returns 200 with date range filters")
    void returns200WithFilters() throws Exception {
      when(calendarService.getEvents(any(), eq("2026-07-01T00:00:00Z"),
          eq("2026-07-31T23:59:59Z")))
          .thenReturn(List.of(sampleEventResponse()));

      mockMvc.perform(get("/api/v1/calendar/events")
              .param("from", "2026-07-01T00:00:00Z")
              .param("to", "2026-07-31T23:59:59Z")
              .with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].title").value("Esame Sistemi"));
    }

    /**
     * Verifies that when {@link CalendarService#getEvents} returns an empty list, the controller
     * produces a {@code 200 OK} response with an empty JSON array.
     */
    @Test
    @DisplayName("returns 200 with empty list")
    void returns200WhenEmpty() throws Exception {
      when(calendarService.getEvents(any(), any(), any())).thenReturn(List.of());

      mockMvc.perform(get("/api/v1/calendar/events").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isEmpty());
    }
  }

  /**
   * Verifies the HTTP contract of {@code POST /api/v1/calendar/events}, covering successful
   * creation and request body validation.
   */
  @Nested
  @DisplayName("POST /api/v1/calendar/events")
  class CreateEvent {

    /**
     * Verifies that a valid JSON body produces a {@code 201 Created} response whose JSON body
     * contains the expected event fields returned by {@link CalendarService#createEvent}.
     */
    @Test
    @DisplayName("returns 201 with created event")
    void returns201() throws Exception {
      when(calendarService.createEvent(any(), any())).thenReturn(sampleEventResponse());

      mockMvc.perform(post("/api/v1/calendar/events")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"title\":\"Esame Sistemi\",\"startDate\":\"2026-07-15T09:00:00Z\"}"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.title").value("Esame Sistemi"))
          .andExpect(jsonPath("$.type").value("EXAM"));
    }

    /**
     * Verifies that a JSON body missing the required {@code title} field fails bean validation and
     * produces a {@code 400 Bad Request} response without invoking
     * {@link CalendarService#createEvent}.
     */
    @Test
    @DisplayName("returns 400 when title is missing")
    void returns400WhenTitleMissing() throws Exception {
      mockMvc.perform(post("/api/v1/calendar/events")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"startDate\":\"2026-07-15T09:00:00Z\"}"))
          .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that a JSON body missing the required {@code startDate} field fails bean validation
     * and produces a {@code 400 Bad Request} response without invoking
     * {@link CalendarService#createEvent}.
     */
    @Test
    @DisplayName("returns 400 when startDate is missing")
    void returns400WhenStartDateMissing() throws Exception {
      mockMvc.perform(post("/api/v1/calendar/events")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"title\":\"Esame Sistemi\"}"))
          .andExpect(status().isBadRequest());
    }
  }

  /**
   * Verifies the HTTP contract of {@code PUT /api/v1/calendar/events/{id}}, covering successful
   * update and event not found.
   */
  @Nested
  @DisplayName("PUT /api/v1/calendar/events/{id}")
  class UpdateEvent {

    /**
     * Verifies that a valid {@code PUT} request produces a {@code 200 OK} response whose JSON body
     * contains the updated event fields returned by {@link CalendarService#updateEvent}.
     */
    @Test
    @DisplayName("returns 200 with updated event")
    void returns200() throws Exception {
      CalendarEventResponse updated = sampleEventResponse();
      updated.setTitle("Esame Sistemi Operativi");

      when(calendarService.updateEvent(any(), any(), any())).thenReturn(updated);

      mockMvc.perform(put("/api/v1/calendar/events/" + UUID.randomUUID())
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content(
                  "{\"title\":\"Esame Sistemi Operativi\",\"startDate\":\"2026-07-15T09:00:00Z\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value("Esame Sistemi Operativi"));
    }

    /**
     * Verifies that an {@link EventNotFoundException} thrown by {@link CalendarService#updateEvent}
     * is mapped to a {@code 404 Not Found} response.
     */
    @Test
    @DisplayName("returns 404 when event not found")
    void returns404() throws Exception {
      when(calendarService.updateEvent(any(), any(), any()))
          .thenThrow(new EventNotFoundException("not found"));

      mockMvc.perform(put("/api/v1/calendar/events/" + UUID.randomUUID())
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content(
                  "{\"title\":\"Esame Sistemi\",\"startDate\":\"2026-07-15T09:00:00Z\"}"))
          .andExpect(status().isNotFound());
    }
  }

  /**
   * Verifies the HTTP contract of {@code DELETE /api/v1/calendar/events/{id}}, covering successful
   * deletion and event not found.
   */
  @Nested
  @DisplayName("DELETE /api/v1/calendar/events/{id}")
  class DeleteEvent {

    /**
     * Verifies that a successful call to {@link CalendarService#deleteEvent} produces a
     * {@code 204 No Content} response with no body.
     */
    @Test
    @DisplayName("returns 204 on success")
    void returns204() throws Exception {
      doNothing().when(calendarService).deleteEvent(any(), any());

      mockMvc.perform(delete("/api/v1/calendar/events/" + UUID.randomUUID()).with(auth()))
          .andExpect(status().isNoContent());
    }

    /**
     * Verifies that an {@link EventNotFoundException} thrown by {@link CalendarService#deleteEvent}
     * is mapped to a {@code 404 Not Found} response.
     */
    @Test
    @DisplayName("returns 404 when event not found")
    void returns404() throws Exception {
      doThrow(new EventNotFoundException("not found"))
          .when(calendarService).deleteEvent(any(), any());

      mockMvc.perform(delete("/api/v1/calendar/events/" + UUID.randomUUID()).with(auth()))
          .andExpect(status().isNotFound());
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/calendar/university-events}, covering
   * successful retrieval with the {@code imported} flag.
   */
  @Nested
  @DisplayName("GET /api/v1/calendar/university-events")
  class GetUniversityEvents {

    /**
     * Verifies that a successful call to {@link CalendarService#getUniversityEvents} produces a
     * {@code 200 OK} response whose JSON body contains the expected university event fields,
     * including the {@code imported} flag.
     */
    @Test
    @DisplayName("returns 200 with university events")
    void returns200() throws Exception {
      UniversityEventResponse r = new UniversityEventResponse();
      r.setId(UUID.randomUUID().toString());
      r.setTitle("Inaugurazione Anno Accademico");
      r.setUniversityId("UNIMOL");
      r.setStartDate("2026-10-01T10:00:00Z");
      r.setType(CalendarEventType.UNIVERSITY);
      r.setImported(false);
      r.setPublishedAt("2026-06-12T07:00:00Z");

      when(calendarService.getUniversityEvents(any())).thenReturn(List.of(r));

      mockMvc.perform(get("/api/v1/calendar/university-events").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].title").value("Inaugurazione Anno Accademico"))
          .andExpect(jsonPath("$[0].universityId").value("UNIMOL"))
          .andExpect(jsonPath("$[0].imported").value(false));
    }

    /**
     * Verifies that when {@link CalendarService#getUniversityEvents} returns an empty list, the
     * controller produces a {@code 200 OK} response with an empty JSON array.
     */
    @Test
    @DisplayName("returns 200 with empty list")
    void returns200WhenEmpty() throws Exception {
      when(calendarService.getUniversityEvents(any())).thenReturn(List.of());

      mockMvc.perform(get("/api/v1/calendar/university-events").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isEmpty());
    }
  }

  /**
   * Verifies the HTTP contract of {@code POST /api/v1/calendar/university-events/{id}/import},
   * covering successful import, event not found, and duplicate import conflict.
   */
  @Nested
  @DisplayName("POST /api/v1/calendar/university-events/{id}/import")
  class ImportUniversityEvent {

    /**
     * Verifies that a successful call to {@link CalendarService#importUniversityEvent} produces a
     * {@code 204 No Content} response with no body.
     */
    @Test
    @DisplayName("returns 204 on success")
    void returns204() throws Exception {
      doNothing().when(calendarService).importUniversityEvent(any(), any());

      mockMvc.perform(
              post("/api/v1/calendar/university-events/" + UUID.randomUUID() + "/import")
                  .with(auth()))
          .andExpect(status().isNoContent());
    }

    /**
     * Verifies that an {@link EventNotFoundException} thrown by
     * {@link CalendarService#importUniversityEvent} — indicating the university event does not
     * exist — is mapped to a {@code 404 Not Found} response.
     */
    @Test
    @DisplayName("returns 404 when university event not found")
    void returns404() throws Exception {
      doThrow(new EventNotFoundException("not found"))
          .when(calendarService).importUniversityEvent(any(), any());

      mockMvc.perform(
              post("/api/v1/calendar/university-events/" + UUID.randomUUID() + "/import")
                  .with(auth()))
          .andExpect(status().isNotFound());
    }

    /**
     * Verifies that an {@link EventAlreadyImportedException} thrown by
     * {@link CalendarService#importUniversityEvent} — indicating the student already imported this
     * event — is mapped to a {@code 409 Conflict} response.
     */
    @Test
    @DisplayName("returns 409 when event already imported")
    void returns409() throws Exception {
      doThrow(new EventAlreadyImportedException("already imported"))
          .when(calendarService).importUniversityEvent(any(), any());

      mockMvc.perform(
              post("/api/v1/calendar/university-events/" + UUID.randomUUID() + "/import")
                  .with(auth()))
          .andExpect(status().isConflict());
    }
  }
}