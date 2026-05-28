package org.ohmyopensource.ohmyuniversity.core.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.UniversityConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository responsible for managing {@link UniversityConnection} entities.
 *
 * <p>This repository handles persistence of the relationship between an OhMyUniversity user and
 * their associated university/Cineca account.
 *
 * <p>It is used to:
 * - Store and retrieve university-specific user credentials
 * - Resolve Cineca usernames per university context
 * - Maintain mappings between internal users and external ESSE3 accounts
 *
 * <p>This layer is part of the authentication and identity linkage subsystem.
 */
@Repository
public interface UniversityConnectionRepository
    extends JpaRepository<UniversityConnection, UUID> {

  /**
   * Retrieves all university connections associated with a given user.
   *
   * @param userId internal user identifier
   * @return list of university connections for the user
   */
  List<UniversityConnection> findByUserId(UUID userId);

  /**
   * Finds a specific university connection for a user and Cineca username.
   *
   * <p>This method is used to ensure uniqueness of the Cineca account mapping within a specific
   * university context.
   *
   * @param userId         internal user identifier
   * @param universityId   university identifier
   * @param usernameCineca Cineca username
   * @return matching university connection if present
   */
  Optional<UniversityConnection> findByUserIdAndUniversityIdAndUsernameCineca(
      UUID userId,
      String universityId,
      String usernameCineca);
}