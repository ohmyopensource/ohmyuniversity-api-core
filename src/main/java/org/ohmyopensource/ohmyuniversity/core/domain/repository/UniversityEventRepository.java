package org.ohmyopensource.ohmyuniversity.core.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.UniversityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link UniversityEvent} entities.
 *
 * <p>Provides persistence and query operations for university-published events. Queries are scoped
 * to a specific university identifier to isolate events across institutions.
 */
public interface UniversityEventRepository extends JpaRepository<UniversityEvent, UUID> {

  /**
   * Returns upcoming university-published events for a given institution whose start date is on or
   * after the specified instant, ordered by start date ascending.
   *
   * @param universityId the university identifier (e.g. {@code UNIMOL})
   * @param from         the inclusive lower bound for {@code startDate} (UTC); typically
   *                     {@link Instant#now()} to exclude past events
   * @return list of {@link UniversityEvent} entities ordered by {@code startDate} ascending,
   *     possibly empty
   */
  @Query("SELECT e FROM UniversityEvent e WHERE e.universityId = :universityId "
      + "AND e.startDate >= :from ORDER BY e.startDate ASC")
  List<UniversityEvent> findUpcomingByUniversity(
      @Param("universityId") String universityId,
      @Param("from") Instant from);
}