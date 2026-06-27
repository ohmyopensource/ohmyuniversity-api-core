package org.ohmyopensource.ohmyuniversity.core.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CachedProfiloCarriera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link CachedProfiloCarriera} — the read cache of Cineca career profiles.
 *
 * <p>Used to persist and retrieve all career profiles associated with an OhMyUniversity user,
 * enabling multi-university profile aggregation in the {@code LoginResponse}.
 */
@Repository
public interface CachedProfiloCarrieraRepository extends JpaRepository<CachedProfiloCarriera, UUID> {

  /**
   * Returns all cached profiles for a given user, across all universities.
   *
   * @param userId internal OhMyUniversity user identifier
   * @return list of cached career profiles
   */
  List<CachedProfiloCarriera> findByUserId(UUID userId);

  /**
   * Finds a cached profile by user and stuId for upsert purposes.
   *
   * @param userId internal user identifier
   * @param stuId  Cineca student career identifier
   * @return matching cached profile if present
   */
  Optional<CachedProfiloCarriera> findByUserIdAndStuId(UUID userId, Long stuId);
}