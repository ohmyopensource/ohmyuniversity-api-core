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
 * their associated platform vendor account.
 *
 * <p>This table is a local shadow of data owned by the auth service — see
 * {@link UniversityConnection} for details. It is used here to: - Store and retrieve
 * university-specific user connections - Resolve vendor usernames per university context - Maintain
 * mappings between internal users and external vendor accounts
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
   * Finds a specific university connection for a user and vendor username.
   *
   * <p>This method is used to ensure uniqueness of the vendor account mapping within a specific
   * university context.
   *
   * @param userId         internal user identifier
   * @param universityId   university identifier
   * @param usernameVendor vendor username
   * @return matching university connection if present
   */
  Optional<UniversityConnection> findByUserIdAndUniversityIdAndUsernameVendor(
      UUID userId,
      String universityId,
      String usernameVendor);
}