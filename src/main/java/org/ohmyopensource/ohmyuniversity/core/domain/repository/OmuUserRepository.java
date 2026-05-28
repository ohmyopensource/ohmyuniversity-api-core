package org.ohmyopensource.ohmyuniversity.core.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.OmuUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository responsible for managing {@link OmuUser} entities.
 *
 * <p>This repository provides persistence operations for the core OhMyUniversity user model.
 *
 * <p>It is primarily used to:
 * - Retrieve users by their Italian fiscal code (codice fiscale)
 * - Persist newly registered users during authentication flow
 * - Support identity resolution across multiple university connections
 *
 * <p>The fiscal code acts as the primary external identifier for user deduplication.
 */
@Repository
public interface OmuUserRepository extends JpaRepository<OmuUser, UUID> {

  /**
   * Retrieves a user by their Italian fiscal code.
   *
   * <p>This method is used during authentication to determine whether a user already exists in the
   * system before creating a new record.
   *
   * @param codiceFiscale Italian fiscal code (unique per user)
   * @return user if found, otherwise empty
   */
  Optional<OmuUser> findByCodiceFiscale(String codiceFiscale);
}