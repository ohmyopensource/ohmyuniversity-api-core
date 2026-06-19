package org.ohmyopensource.ohmyuniversity.core.domain.repository;

import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CinecaSyncState;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CinecaSyncState.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link CinecaSyncState} entities.
 *
 * <p>Used by {@code CinecaSyncService} to check whether a specific Kafka
 * integration event has already been published for a given user/university/course
 * combination, preventing duplicate event publishing on repeated logins.
 */
@Repository
public interface CinecaSyncStateRepository extends JpaRepository<CinecaSyncState, UUID> {

  /**
   * Checks whether a Kafka event of the given type has already been published
   * for the specified user, university, and Cineca activity.
   *
   * @param userId       the OhMyU user UUID
   * @param universityId the university identifier (e.g. "UNIMOL")
   * @param adsceId      the Cineca activity identifier from the libretto row
   * @param eventType    the type of Kafka event to check
   * @return {@code true} if the event was already published, {@code false} otherwise
   */
  boolean existsByUserIdAndUniversityIdAndAdsceIdAndEventType(
      UUID userId,
      String universityId,
      Long adsceId,
      EventType eventType
  );
}