package org.ohmyopensource.ohmyuniversity.core.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
import org.ohmyopensource.ohmyuniversity.core.dto.agenda.AgendaEventRequest;
import org.ohmyopensource.ohmyuniversity.core.dto.agenda.AgendaEventResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.agenda.UniversityEventResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates calendar operations for authenticated OhMyUniversity students.
 *
 * <p>The calendar is shared across all university enrollments of a student — events are associated
 * with the OhMyU user identity, not a specific university.
 *
 * <p>University-published events are stored separately in {@link UniversityEvent} and can be
 * imported by the student into their personal calendar via {@link CalendarEventImport}.
 */
@Service
public class AgendaService {

  private static final Logger log = LoggerFactory.getLogger(AgendaService.class);

  private final CalendarEventRepository eventRepository;
  private final UniversityEventRepository universityEventRepository;
  private final CalendarEventImportRepository importRepository;
  private final OmuUserRepository userRepository;

  // ============ Constructor ============

  /**
   * Creates the calendar service and injects all required repositories.
   *
   * @param eventRepository            repository for personal calendar events
   * @param universityEventRepository  repository for university-published events
   * @param importRepository           repository for import bridge records
   * @param userRepository             repository for OhMyU user entities
   */
  public AgendaService(
      CalendarEventRepository eventRepository,
      UniversityEventRepository universityEventRepository,
      CalendarEventImportRepository importRepository,
      OmuUserRepository userRepository) {
    this.eventRepository = eventRepository;
    this.universityEventRepository = universityEventRepository;
    this.importRepository = importRepository;
    this.userRepository = userRepository;
  }

  // ============ Class Methods ============

  /**
   * Returns all personal calendar events for the authenticated student, optionally filtered by
   * date range.
   *
   * <p>When both {@code from} and {@code to} are provided, only events whose {@code startDate}
   * falls within the range (inclusive) are returned. When either parameter is {@code null}, all
   * events for the user are returned ordered by start date ascending.
   *
   * @param principal authenticated OhMyU principal
   * @param from      optional range start as an ISO-8601 string, inclusive
   * @param to        optional range end as an ISO-8601 string, inclusive
   * @return list of {@link AgendaEventResponse} ordered by start date ascending
   */
  @Transactional(readOnly = true)
  public List<AgendaEventResponse> getEvents(
      OmuPrincipal principal, String from, String to) {

    UUID userId = UUID.fromString(principal.omuUserId());

    List<CalendarEvent> events;
    if (from != null && to != null) {
      events = eventRepository.findByUserIdAndDateRange(
          userId, Instant.parse(from), Instant.parse(to));
    } else {
      events = eventRepository.findByUserIdOrderByStartDateAsc(userId);
    }

    log.debug("CalendarService: fetched {} events for user={}", events.size(),
        principal.omuUserId());
    return events.stream().map(this::toResponse).toList();
  }

  /**
   * Creates a new personal calendar event for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @param request   event data; {@code title} and {@code startDate} are required
   * @return the created event as a {@link AgendaEventResponse}
   */
  @Transactional
  public AgendaEventResponse createEvent(
      OmuPrincipal principal, AgendaEventRequest request) {

    OmuUser user = resolveUser(principal);

    CalendarEvent event = new CalendarEvent();
    event.setUser(user);
    mapRequestToEntity(request, event);
    event = eventRepository.save(event);

    log.debug("CalendarService: created event id={} for user={}", event.getId(),
        principal.omuUserId());
    return toResponse(event);
  }

  /**
   * Updates an existing personal calendar event owned by the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @param eventId   identifier of the event to update
   * @param request   updated event data
   * @return the updated event as a {@link AgendaEventResponse}
   * @throws EventNotFoundException if the event does not exist or belongs to another user
   */
  @Transactional
  public AgendaEventResponse updateEvent(
      OmuPrincipal principal, UUID eventId, AgendaEventRequest request) {

    UUID userId = UUID.fromString(principal.omuUserId());
    CalendarEvent event = eventRepository.findByIdAndUserId(eventId, userId)
        .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));

    mapRequestToEntity(request, event);
    event.setUpdatedAt(Instant.now());
    event = eventRepository.save(event);

    log.debug("CalendarService: updated event id={} for user={}", eventId,
        principal.omuUserId());
    return toResponse(event);
  }

  /**
   * Deletes a personal calendar event owned by the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @param eventId   identifier of the event to delete
   * @throws EventNotFoundException if the event does not exist or belongs to another user
   */
  @Transactional
  public void deleteEvent(OmuPrincipal principal, UUID eventId) {
    UUID userId = UUID.fromString(principal.omuUserId());
    CalendarEvent event = eventRepository.findByIdAndUserId(eventId, userId)
        .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));

    eventRepository.delete(event);
    log.debug("CalendarService: deleted event id={} for user={}", eventId,
        principal.omuUserId());
  }

  /**
   * Returns upcoming university-published events for the student's active university.
   *
   * <p>Each event in the response includes an {@code imported} flag indicating whether the
   * authenticated student has already added it to their personal calendar.
   *
   * @param principal authenticated OhMyU principal
   * @return list of {@link UniversityEventResponse} ordered by start date ascending
   */
  @Transactional(readOnly = true)
  public List<UniversityEventResponse> getUniversityEvents(OmuPrincipal principal) {
    UUID userId = UUID.fromString(principal.omuUserId());
    List<UniversityEvent> events = universityEventRepository
        .findUpcomingByUniversity(principal.universityId(), Instant.now());

    List<CalendarEventImport> imports = importRepository.findByUserId(userId);
    java.util.Set<UUID> importedIds = imports.stream()
        .map(i -> i.getUniversityEvent().getId())
        .collect(java.util.stream.Collectors.toSet());

    return events.stream()
        .map(e -> toUniversityResponse(e, importedIds.contains(e.getId())))
        .toList();
  }

  /**
   * Imports a university-published event into the authenticated student's personal calendar.
   *
   * <p>Creates a {@link CalendarEventImport} bridge record linking the student to the university
   * event. The unique constraint on the bridge table prevents duplicate imports.
   *
   * @param principal         authenticated OhMyU principal
   * @param universityEventId identifier of the university event to import
   * @throws EventNotFoundException        if the university event does not exist
   * @throws EventAlreadyImportedException if the student has already imported this event
   */
  @Transactional
  public void importUniversityEvent(OmuPrincipal principal, UUID universityEventId) {
    UUID userId = UUID.fromString(principal.omuUserId());
    OmuUser user = resolveUser(principal);

    UniversityEvent universityEvent = universityEventRepository.findById(universityEventId)
        .orElseThrow(() -> new EventNotFoundException(
            "University event not found: " + universityEventId));

    if (importRepository.existsByUserIdAndUniversityEventId(userId, universityEventId)) {
      throw new EventAlreadyImportedException("Event already imported: " + universityEventId);
    }

    CalendarEventImport imp = new CalendarEventImport();
    imp.setUser(user);
    imp.setUniversityEvent(universityEvent);
    importRepository.save(imp);

    log.debug("CalendarService: imported university event id={} for user={}",
        universityEventId, principal.omuUserId());
  }

  /**
   * Resolves the {@link OmuUser} entity for the authenticated principal.
   *
   * @param principal authenticated OhMyU principal
   * @return the matching {@link OmuUser}
   * @throws IllegalStateException if no user record exists for the given id
   */
  private OmuUser resolveUser(OmuPrincipal principal) {
    return userRepository.findById(UUID.fromString(principal.omuUserId()))
        .orElseThrow(() -> new IllegalStateException(
            "User not found: " + principal.omuUserId()));
  }

  /**
   * Maps fields from a {@link AgendaEventRequest} onto an existing {@link CalendarEvent} entity.
   *
   * <p>When {@code request.getType()} is {@code null}, the type defaults to
   * {@link CalendarEventType#PERSONAL}.
   *
   * @param request the incoming request DTO
   * @param event   the entity to populate
   */
  private void mapRequestToEntity(AgendaEventRequest request, CalendarEvent event) {
    event.setTitle(request.getTitle());
    event.setDescription(request.getDescription());
    event.setStartDate(Instant.parse(request.getStartDate()));
    event.setEndDate(request.getEndDate() != null ? Instant.parse(request.getEndDate()) : null);
    event.setAllDay(request.isAllDay());
    event.setType(request.getType() != null ? request.getType() : CalendarEventType.PERSONAL);
    event.setColor(request.getColor());
    event.setUrl(request.getUrl());
    event.setNotes(request.getNotes());
    event.setLocation(request.getLocation());
  }

  /**
   * Maps a {@link CalendarEvent} entity to a {@link AgendaEventResponse} DTO.
   *
   * @param e the source entity
   * @return the populated response DTO
   */
  private AgendaEventResponse toResponse(CalendarEvent e) {
    AgendaEventResponse r = new AgendaEventResponse();
    r.setId(e.getId().toString());
    r.setTitle(e.getTitle());
    r.setDescription(e.getDescription());
    r.setStartDate(e.getStartDate().toString());
    r.setEndDate(e.getEndDate() != null ? e.getEndDate().toString() : null);
    r.setAllDay(e.isAllDay());
    r.setType(e.getType());
    r.setColor(e.getColor());
    r.setUrl(e.getUrl());
    r.setNotes(e.getNotes());
    r.setLocation(e.getLocation());
    r.setCreatedAt(e.getCreatedAt().toString());
    r.setUpdatedAt(e.getUpdatedAt().toString());
    return r;
  }

  /**
   * Maps a {@link UniversityEvent} entity to a {@link UniversityEventResponse} DTO.
   *
   * @param e        the source entity
   * @param imported whether the authenticated student has already imported this event
   * @return the populated response DTO
   */
  private UniversityEventResponse toUniversityResponse(UniversityEvent e, boolean imported) {
    UniversityEventResponse r = new UniversityEventResponse();
    r.setId(e.getId().toString());
    r.setUniversityId(e.getUniversityId());
    r.setTitle(e.getTitle());
    r.setDescription(e.getDescription());
    r.setStartDate(e.getStartDate().toString());
    r.setEndDate(e.getEndDate() != null ? e.getEndDate().toString() : null);
    r.setAllDay(e.isAllDay());
    r.setType(e.getType());
    r.setColor(e.getColor());
    r.setUrl(e.getUrl());
    r.setSourceUrl(e.getSourceUrl());
    r.setPublishedAt(e.getPublishedAt().toString());
    r.setImported(imported);
    return r;
  }

  /**
   * Thrown when a calendar event is not found or belongs to another user.
   */
  public static class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(String message) {
      super(message);
    }
  }

  /**
   * Thrown when a student attempts to import a university event they have already imported.
   */
  public static class EventAlreadyImportedException extends RuntimeException {

    public EventAlreadyImportedException(String message) {
      super(message);
    }
  }
}