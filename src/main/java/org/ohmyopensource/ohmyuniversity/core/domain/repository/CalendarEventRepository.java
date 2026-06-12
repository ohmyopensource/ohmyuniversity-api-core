package org.ohmyopensource.ohmyuniversity.core.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link CalendarEvent} entities.
 *
 * <p>Provides persistence and query operations for personal calendar events. All queries are
 * scoped to a specific user to prevent cross-user data access.
 */
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

  /**
   * Returns all personal calendar events for a given student, ordered by start date ascending.
   *
   * @param userId the OhMyU user identifier
   * @return list of {@link CalendarEvent} entities ordered by {@code startDate} ascending,
   *     possibly empty
   */
  List<CalendarEvent> findByUserIdOrderByStartDateAsc(UUID userId);

  /**
   * Returns personal calendar events for a given student whose start date falls within the
   * specified range, ordered by start date ascending.
   *
   * @param userId the OhMyU user identifier
   * @param from   the inclusive range start (UTC)
   * @param to     the inclusive range end (UTC)
   * @return list of {@link CalendarEvent} entities within the range, possibly empty
   */
  @Query("SELECT e FROM CalendarEvent e WHERE e.user.id = :userId "
      + "AND e.startDate >= :from AND e.startDate <= :to "
      + "ORDER BY e.startDate ASC")
  List<CalendarEvent> findByUserIdAndDateRange(
      @Param("userId") UUID userId,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Finds a single personal calendar event by its identifier and owning user.
   *
   * <p>The user scope prevents a student from accessing or modifying events belonging to another
   * user.
   *
   * @param id     the event identifier
   * @param userId the OhMyU user identifier
   * @return an {@link Optional} containing the event if found and owned by the user,
   *     or empty otherwise
   */
  Optional<CalendarEvent> findByIdAndUserId(UUID id, UUID userId);
}