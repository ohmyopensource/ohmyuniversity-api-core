package org.ohmyopensource.ohmyuniversity.core.domain.repository;

import java.util.List;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CalendarEventImport;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link CalendarEventImport} records.
 *
 * <p>Provides persistence operations for the bridge table that tracks which university events
 * each student has imported into their personal calendar.
 */
public interface CalendarEventImportRepository extends JpaRepository<CalendarEventImport, UUID> {

  /**
   * Returns whether a student has already imported a specific university event.
   *
   * @param userId            the OhMyU user identifier
   * @param universityEventId the university event identifier
   * @return {@code true} if the import record exists
   */
  boolean existsByUserIdAndUniversityEventId(UUID userId, UUID universityEventId);

  /**
   * Returns all import records for a given student, ordered by insertion order.
   *
   * @param userId the OhMyU user identifier
   * @return list of {@link CalendarEventImport} records, possibly empty
   */
  List<CalendarEventImport> findByUserId(UUID userId);
}