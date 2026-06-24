package org.ohmyopensource.ohmyuniversity.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CalendarEvent;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CalendarEventImport;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CalendarEventType;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.OmuUser;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.UniversityEvent;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.CalendarEventImportRepository;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.CalendarEventRepository;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.OmuUserRepository;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityEventRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.calendar.CalendarEventRequest;
import org.ohmyopensource.ohmyuniversity.core.dto.calendar.CalendarEventResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.calendar.UniversityEventResponse;
import org.ohmyopensource.ohmyuniversity.core.service.AgendaService.EventAlreadyImportedException;
import org.ohmyopensource.ohmyuniversity.core.service.AgendaService.EventNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link AgendaService}.
 *
 * <p>All repositories are replaced by Mockito mocks so that no database or Spring context is
 * required. Each test group covers a single public method of the service, verifying both the happy
 * path and the relevant exception paths.
 */
@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

  @Mock
  private CalendarEventRepository eventRepository;

  @Mock
  private UniversityEventRepository universityEventRepository;

  @Mock
  private CalendarEventImportRepository importRepository;

  @Mock
  private OmuUserRepository userRepository;

  @InjectMocks
  private AgendaService agendaService;

  private OmuPrincipal principal;
  private UUID userId;
  private OmuUser user;

  /**
   * Initialises a test {@link OmuPrincipal} and a matching {@link OmuUser} entity reused across all
   * test cases.
   */
  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    principal = new OmuPrincipal(
        userId.toString(),
        "TSTXXX00A00X000X",
        "UNIMOL",
        89486L,
        106279L,
        "178026", true);
    user = new OmuUser();
  }

  // ============ Helpers ============

  private CalendarEvent sampleEvent() {
    CalendarEvent e = new CalendarEvent();
    ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
    e.setUser(user);
    e.setTitle("Esame Sistemi");
    e.setStartDate(Instant.parse("2026-07-15T09:00:00Z"));
    e.setType(CalendarEventType.EXAM);
    e.setUpdatedAt(Instant.now());
    return e;
  }

  private CalendarEventRequest sampleRequest() {
    CalendarEventRequest r = new CalendarEventRequest();
    r.setTitle("Esame Sistemi");
    r.setStartDate("2026-07-15T09:00:00Z");
    r.setType(CalendarEventType.EXAM);
    return r;
  }

  private UniversityEvent sampleUniversityEvent() {
    UniversityEvent e = new UniversityEvent();
    ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
    e.setUniversityId("UNIMOL");
    e.setTitle("Inaugurazione Anno Accademico");
    e.setStartDate(Instant.parse("2026-10-01T10:00:00Z"));
    e.setType(CalendarEventType.UNIVERSITY);
    e.setPublishedAt(Instant.now());
    return e;
  }

  /**
   * Verifies {@link AgendaService#getEvents} covering retrieval without filters and with date
   * range filters.
   */
  @Nested
  @DisplayName("getEvents")
  class GetEvents {

    /**
     * Verifies that when no date filters are supplied,
     * {@link CalendarEventRepository #findByUserIdOrderByStartDateAsc} is called and the result is
     * mapped to {@link CalendarEventResponse} correctly.
     */
    @Test
    @DisplayName("returns events ordered by start date when no filters")
    void returnsEventsWithoutFilters() {
      CalendarEvent event = sampleEvent();
      when(eventRepository.findByUserIdOrderByStartDateAsc(userId))
          .thenReturn(List.of(event));

      List<CalendarEventResponse> result = agendaService.getEvents(principal, null, null);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTitle()).isEqualTo("Esame Sistemi");
      assertThat(result.get(0).getType()).isEqualTo(CalendarEventType.EXAM);
    }

    /**
     * Verifies that when both {@code from} and {@code to} filters are supplied,
     * {@link CalendarEventRepository#findByUserIdAndDateRange} is called with the parsed instants
     * and the result is mapped to {@link CalendarEventResponse} correctly.
     */
    @Test
    @DisplayName("returns events filtered by date range")
    void returnsEventsWithFilters() {
      CalendarEvent event = sampleEvent();
      when(eventRepository.findByUserIdAndDateRange(any(), any(), any()))
          .thenReturn(List.of(event));

      List<CalendarEventResponse> result = agendaService.getEvents(
          principal, "2026-07-01T00:00:00Z", "2026-07-31T23:59:59Z");

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTitle()).isEqualTo("Esame Sistemi");
    }

    /**
     * Verifies that when the repository returns an empty list, {@link AgendaService#getEvents}
     * returns an empty list without throwing.
     */
    @Test
    @DisplayName("returns empty list when no events found")
    void returnsEmptyList() {
      when(eventRepository.findByUserIdOrderByStartDateAsc(userId)).thenReturn(List.of());

      List<CalendarEventResponse> result = agendaService.getEvents(principal, null, null);

      assertThat(result).isEmpty();
    }
  }

  /**
   * Verifies {@link AgendaService#createEvent} covering successful creation and the default type
   * fallback.
   */
  @Nested
  @DisplayName("createEvent")
  class CreateEvent {

    /**
     * Verifies that {@link AgendaService#createEvent} resolves the user, maps the request to a
     * {@link CalendarEvent} entity, persists it via the repository, and returns a correctly
     * populated {@link CalendarEventResponse}.
     */
    @Test
    @DisplayName("creates and returns event")
    void createsEvent() {
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      CalendarEvent saved = sampleEvent();
      when(eventRepository.save(any())).thenReturn(saved);

      CalendarEventResponse result = agendaService.createEvent(principal, sampleRequest());

      assertThat(result.getTitle()).isEqualTo("Esame Sistemi");
      assertThat(result.getType()).isEqualTo(CalendarEventType.EXAM);
      verify(eventRepository).save(any());
    }

    /**
     * Verifies that when the request omits the {@code type} field, the service defaults to
     * {@link CalendarEventType#PERSONAL} before persisting the entity.
     */
    @Test
    @DisplayName("defaults type to PERSONAL when not specified")
    void defaultsTypeToPersonal() {
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      CalendarEvent saved = sampleEvent();
      saved.setType(CalendarEventType.PERSONAL);
      when(eventRepository.save(any())).thenReturn(saved);

      CalendarEventRequest request = new CalendarEventRequest();
      request.setTitle("Evento generico");
      request.setStartDate("2026-07-15T09:00:00Z");

      CalendarEventResponse result = agendaService.createEvent(principal, request);

      assertThat(result.getType()).isEqualTo(CalendarEventType.PERSONAL);
    }

    /**
     * Verifies that when the authenticated user is not found in the database,
     * {@link AgendaService#createEvent} throws {@link IllegalStateException}.
     */
    @Test
    @DisplayName("throws IllegalStateException when user not found")
    void throwsWhenUserNotFound() {
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> agendaService.createEvent(principal, sampleRequest()))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  /**
   * Verifies {@link AgendaService#updateEvent} covering successful update and event not found.
   */
  @Nested
  @DisplayName("updateEvent")
  class UpdateEvent {

    /**
     * Verifies that {@link AgendaService#updateEvent} loads the event by id and user id, applies
     * the request fields, persists the updated entity, and returns a correctly populated
     * {@link CalendarEventResponse}.
     */
    @Test
    @DisplayName("updates and returns event")
    void updatesEvent() {
      UUID eventId = UUID.randomUUID();
      CalendarEvent existing = sampleEvent();
      when(eventRepository.findByIdAndUserId(eventId, userId))
          .thenReturn(Optional.of(existing));
      when(eventRepository.save(any())).thenReturn(existing);

      CalendarEventRequest request = sampleRequest();
      request.setTitle("Esame Aggiornato");

      CalendarEventResponse result = agendaService.updateEvent(principal, eventId, request);

      assertThat(result.getTitle()).isEqualTo("Esame Aggiornato");
      verify(eventRepository).save(existing);
    }

    /**
     * Verifies that when the event is not found or belongs to another user,
     * {@link AgendaService#updateEvent} throws {@link EventNotFoundException}.
     */
    @Test
    @DisplayName("throws EventNotFoundException when event not found")
    void throwsWhenNotFound() {
      UUID eventId = UUID.randomUUID();
      when(eventRepository.findByIdAndUserId(eventId, userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> agendaService.updateEvent(principal, eventId, sampleRequest()))
          .isInstanceOf(EventNotFoundException.class);
    }
  }

  /**
   * Verifies {@link AgendaService#deleteEvent} covering successful deletion and event not found.
   */
  @Nested
  @DisplayName("deleteEvent")
  class DeleteEvent {

    /**
     * Verifies that {@link AgendaService#deleteEvent} loads the event by id and user id and
     * delegates deletion to {@link CalendarEventRepository#delete}.
     */
    @Test
    @DisplayName("deletes event")
    void deletesEvent() {
      UUID eventId = UUID.randomUUID();
      CalendarEvent existing = sampleEvent();
      when(eventRepository.findByIdAndUserId(eventId, userId))
          .thenReturn(Optional.of(existing));

      agendaService.deleteEvent(principal, eventId);

      verify(eventRepository).delete(existing);
    }

    /**
     * Verifies that when the event is not found or belongs to another user,
     * {@link AgendaService#deleteEvent} throws {@link EventNotFoundException}.
     */
    @Test
    @DisplayName("throws EventNotFoundException when event not found")
    void throwsWhenNotFound() {
      UUID eventId = UUID.randomUUID();
      when(eventRepository.findByIdAndUserId(eventId, userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> agendaService.deleteEvent(principal, eventId))
          .isInstanceOf(EventNotFoundException.class);
    }
  }

  /**
   * Verifies {@link AgendaService#getUniversityEvents} covering retrieval with and without
   * already-imported events.
   */
  @Nested
  @DisplayName("getUniversityEvents")
  class GetUniversityEvents {

    /**
     * Verifies that {@link AgendaService#getUniversityEvents} returns university events with the
     * {@code imported} flag set to {@code false} when the student has not yet imported any of
     * them.
     */
    @Test
    @DisplayName("returns university events with imported=false")
    void returnsEventsNotImported() {
      UniversityEvent event = sampleUniversityEvent();
      when(universityEventRepository.findUpcomingByUniversity(any(), any()))
          .thenReturn(List.of(event));
      when(importRepository.findByUserId(userId)).thenReturn(List.of());

      List<UniversityEventResponse> result = agendaService.getUniversityEvents(principal);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTitle()).isEqualTo("Inaugurazione Anno Accademico");
      assertThat(result.get(0).isImported()).isFalse();
    }

    /**
     * Verifies that {@link AgendaService#getUniversityEvents} sets the {@code imported} flag to
     * {@code true} for events the student has already imported into their personal calendar.
     */
    @Test
    @DisplayName("returns university events with imported=true when already imported")
    void returnsEventsAlreadyImported() {
      UniversityEvent event = sampleUniversityEvent();

      CalendarEventImport imp = new CalendarEventImport();
      imp.setUniversityEvent(event);

      when(universityEventRepository.findUpcomingByUniversity(any(), any()))
          .thenReturn(List.of(event));
      when(importRepository.findByUserId(userId)).thenReturn(List.of(imp));

      List<UniversityEventResponse> result = agendaService.getUniversityEvents(principal);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).isImported()).isTrue();
    }
  }

  /**
   * Verifies {@link AgendaService#importUniversityEvent} covering successful import, event not
   * found, and duplicate import.
   */
  @Nested
  @DisplayName("importUniversityEvent")
  class ImportUniversityEvent {

    /**
     * Verifies that {@link AgendaService#importUniversityEvent} resolves the user and university
     * event, creates a {@link CalendarEventImport} record, and persists it via the repository.
     */
    @Test
    @DisplayName("imports university event successfully")
    void importsEvent() {
      UUID eventId = UUID.randomUUID();
      UniversityEvent event = sampleUniversityEvent();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(universityEventRepository.findById(eventId)).thenReturn(Optional.of(event));
      when(importRepository.existsByUserIdAndUniversityEventId(userId, eventId))
          .thenReturn(false);

      agendaService.importUniversityEvent(principal, eventId);

      verify(importRepository).save(any());
    }

    /**
     * Verifies that when the university event is not found,
     * {@link AgendaService#importUniversityEvent} throws {@link EventNotFoundException}.
     */
    @Test
    @DisplayName("throws EventNotFoundException when university event not found")
    void throwsWhenEventNotFound() {
      UUID eventId = UUID.randomUUID();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(universityEventRepository.findById(eventId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> agendaService.importUniversityEvent(principal, eventId))
          .isInstanceOf(EventNotFoundException.class);
    }

    /**
     * Verifies that when the student has already imported the university event,
     * {@link AgendaService#importUniversityEvent} throws {@link EventAlreadyImportedException}.
     */
    @Test
    @DisplayName("throws EventAlreadyImportedException when already imported")
    void throwsWhenAlreadyImported() {
      UUID eventId = UUID.randomUUID();
      UniversityEvent event = sampleUniversityEvent();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(universityEventRepository.findById(eventId)).thenReturn(Optional.of(event));
      when(importRepository.existsByUserIdAndUniversityEventId(userId, eventId))
          .thenReturn(true);

      assertThatThrownBy(() -> agendaService.importUniversityEvent(principal, eventId))
          .isInstanceOf(EventAlreadyImportedException.class);
    }
  }
}