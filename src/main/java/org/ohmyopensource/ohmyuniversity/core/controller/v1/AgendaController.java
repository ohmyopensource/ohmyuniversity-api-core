package org.ohmyopensource.ohmyuniversity.core.controller.v1;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.dto.agenda.AgendaEventRequest;
import org.ohmyopensource.ohmyuniversity.core.dto.agenda.AgendaEventResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.agenda.UniversityEventResponse;
import org.ohmyopensource.ohmyuniversity.core.service.AgendaService;
import org.ohmyopensource.ohmyuniversity.core.service.AgendaService.EventAlreadyImportedException;
import org.ohmyopensource.ohmyuniversity.core.service.AgendaService.EventNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for integrated calendar operations.
 *
 * <p>Exposes CRUD endpoints for personal calendar events and read/import endpoints for
 * university-published events.
 *
 * <p>The calendar is shared across all university enrollments of the student — events are
 * associated with the OhMyU user identity, not a specific university.
 *
 * <p>Endpoints:
 * - GET    /api/v1/agenda/events                        — list personal events
 * - POST   /api/v1/agenda/events                        — create personal event
 * - PUT    /api/v1/agenda/events/{id}                   — update personal event
 * - DELETE /api/v1/agenda/events/{id}                   — delete personal event
 * - GET    /api/v1/agenda/university-events             — list university events
 * - POST   /api/v1/agenda/university-events/{id}/import — import university event
 */
@RestController
@RequestMapping("/api/v1/agenda")
public class AgendaController {

  private static final Logger log = LoggerFactory.getLogger(AgendaController.class);

  private final AgendaService agendaService;

  // ============ Constructor ============

  /**
   * Creates the controller and injects the calendar service.
   *
   * @param agendaService service orchestrating calendar operations
   */
  public AgendaController(AgendaService agendaService) {
    this.agendaService = agendaService;
  }

  // ============ Class Methods ============

  /**
   * Returns all personal calendar events for the authenticated student.
   *
   * <p>Optional query parameters {@code from} and {@code to} filter events
   * by start date range (ISO-8601 format, e.g. 2026-06-01T00:00:00Z).
   *
   * @param principal authenticated OhMyU principal
   * @param from      optional range start (ISO-8601)
   * @param to        optional range end (ISO-8601)
   * @return 200 OK with list of events ordered by start date ascending
   */
  @GetMapping("/events")
  public ResponseEntity<List<AgendaEventResponse>> getEvents(
      @AuthenticationPrincipal OmuPrincipal principal,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    return ResponseEntity.ok(agendaService.getEvents(principal, from, to));
  }

  /**
   * Creates a new personal calendar event.
   *
   * @param principal authenticated OhMyU principal
   * @param request   event data — title and startDate are required
   * @return 201 Created with the created event
   */
  @PostMapping("/events")
  public ResponseEntity<AgendaEventResponse> createEvent(
      @AuthenticationPrincipal OmuPrincipal principal,
      @Valid @RequestBody AgendaEventRequest request) {
    AgendaEventResponse response = agendaService.createEvent(principal, request);
    return ResponseEntity.status(201).body(response);
  }

  /**
   * Updates an existing personal calendar event.
   *
   * @param principal authenticated OhMyU principal
   * @param id        event identifier
   * @param request   updated event data
   * @return 200 OK with the updated event, 404 if not found
   */
  @PutMapping("/events/{id}")
  public ResponseEntity<AgendaEventResponse> updateEvent(
      @AuthenticationPrincipal OmuPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody AgendaEventRequest request) {
    try {
      return ResponseEntity.ok(agendaService.updateEvent(principal, id, request));
    } catch (EventNotFoundException e) {
      log.warn("CalendarController: event not found id={}", id);
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Deletes a personal calendar event.
   *
   * @param principal authenticated OhMyU principal
   * @param id        event identifier
   * @return 204 No Content on success, 404 if not found
   */
  @DeleteMapping("/events/{id}")
  public ResponseEntity<Void> deleteEvent(
      @AuthenticationPrincipal OmuPrincipal principal,
      @PathVariable UUID id) {
    try {
      agendaService.deleteEvent(principal, id);
      return ResponseEntity.noContent().build();
    } catch (EventNotFoundException e) {
      log.warn("CalendarController: event not found id={}", id);
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Returns upcoming university-published events for the student's active university.
   *
   * <p>Each event includes an {@code imported} flag indicating whether
   * the student has already added it to their personal calendar.
   *
   * @param principal authenticated OhMyU principal
   * @return 200 OK with list of university events
   */
  @GetMapping("/university-events")
  public ResponseEntity<List<UniversityEventResponse>> getUniversityEvents(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return ResponseEntity.ok(agendaService.getUniversityEvents(principal));
  }

  /**
   * Imports a university-published event into the student's personal calendar.
   *
   * @param principal authenticated OhMyU principal
   * @param id        university event identifier
   * @return 204 No Content on success, 404 if the university event does not exist, 409 Conflict if
   * already imported
   */
  @PostMapping("/university-events/{id}/import")
  public ResponseEntity<Void> importUniversityEvent(
      @AuthenticationPrincipal OmuPrincipal principal,
      @PathVariable UUID id) {
    try {
      agendaService.importUniversityEvent(principal, id);
      return ResponseEntity.noContent().build();
    } catch (EventNotFoundException e) {
      log.warn("CalendarController: university event not found id={}", id);
      return ResponseEntity.notFound().build();
    } catch (EventAlreadyImportedException e) {
      log.warn("CalendarController: event already imported id={}", id);
      return ResponseEntity.status(409).build();
    }
  }
}