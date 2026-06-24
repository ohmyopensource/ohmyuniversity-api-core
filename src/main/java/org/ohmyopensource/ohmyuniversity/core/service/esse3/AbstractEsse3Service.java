package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import java.util.Optional;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.UniversityConnection;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;

/**
 * Abstract base class for all ESSE3-backed services.
 *
 * <p>Centralises session resolution logic shared across all Cineca service
 * implementations, eliminating duplication and enforcing a consistent
 * authentication model throughout the ESSE3 integration layer.
 *
 * <p>Subclasses obtain credentials exclusively through the protected helpers
 * defined here — never by accessing the session store or registry directly.
 */
public abstract class AbstractEsse3Service {

  protected final CinecaSessionStore sessionStore;
  protected final UniversityRegistry universityRegistry;
  protected final UniversityConnectionRepository connectionRepository;

  // ============ Constructor ============

  protected AbstractEsse3Service(
      CinecaSessionStore sessionStore,
      UniversityRegistry universityRegistry,
      UniversityConnectionRepository connectionRepository) {
    this.sessionStore = sessionStore;
    this.universityRegistry = universityRegistry;
    this.connectionRepository = connectionRepository;
  }

  // ============ Protected Helpers ============

  /**
   * Resolves the Cineca JWT for the authenticated user.
   *
   * @param principal authenticated OhMyU principal
   * @return Cineca JWT token
   * @throws CinecaClient.CinecaAuthException if no valid session exists in Redis
   */
  protected String resolveCinecaJwt(OmuPrincipal principal) {
    return sessionStore
        .getCinecaJwt(principal.omuUserId(), principal.universityId())
        .orElseThrow(() -> new CinecaClient.CinecaAuthException(
            "Cineca session expired — please log in again"));
  }

  /**
   * Resolves the Cineca auth token (JSESSIONID) for the authenticated user.
   *
   * <p>Required by calesa-service endpoints that mandate session-level
   * authentication in addition to the Bearer JWT.
   *
   * @param principal authenticated OhMyU principal
   * @return Cineca auth token
   * @throws CinecaClient.CinecaAuthException if no valid token exists in Redis
   */
  protected String resolveCinecaAuthToken(OmuPrincipal principal) {
    return sessionStore
        .getCinecaAuthToken(principal.omuUserId(), principal.universityId())
        .orElseThrow(() -> new CinecaClient.CinecaAuthException(
            "Cineca auth token expired — please log in again"));
  }

  /**
   * Resolves the Cineca ESSE3 base URL for the given university.
   *
   * @param universityId university short code (e.g. {@code UNIMOL})
   * @return configured ESSE3 base URL
   * @throws IllegalArgumentException if the university is not registered
   */
  protected String resolveBaseUrl(String universityId) {
    return universityRegistry
        .resolve(universityId)
        .map(UniversityRegistry.UniversityConfig::baseUrl)
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown university: " + universityId));
  }

  /**
   * Resolves the Cineca username from the stored university connection.
   *
   * <p>Used by endpoints that require Basic Auth (e.g. calesa-service prenotazioni).
   *
   * @param principal authenticated OhMyU principal
   * @return Cineca username associated with the active university connection
   * @throws CinecaClient.CinecaAuthException if no matching connection is found
   */
  protected String resolveUsername(OmuPrincipal principal) {
    return connectionRepository
        .findByUserId(UUID.fromString(principal.omuUserId()))
        .stream()
        .filter(c -> c.getUniversityId().equals(principal.universityId()))
        .map(UniversityConnection::getUsernameCineca)
        .findFirst()
        .orElseThrow(() -> new CinecaClient.CinecaAuthException(
            "No Cineca connection found for university: " + principal.universityId()));
  }

  /**
   * Resolves the Cineca persId for the authenticated user, throwing if absent.
   *
   * <p>Use this variant when persId is strictly required to proceed.
   *
   * @param principal authenticated OhMyU principal
   * @return Cineca person identifier
   * @throws CinecaClient.CinecaAuthException if persId is not cached in Redis
   */
  protected Long resolvePersId(OmuPrincipal principal) {
    return sessionStore
        .getCinecaPersId(principal.omuUserId(), principal.universityId())
        .orElseThrow(() -> new CinecaClient.CinecaAuthException(
            "PersId not found in session — please log in again"));
  }

  /**
   * Resolves the Cineca persId for the authenticated user, returning empty if absent.
   *
   * <p>Use this variant when the caller can handle a missing persId gracefully
   * (e.g. falling back to JWT claims).
   *
   * @param principal authenticated OhMyU principal
   * @return optional Cineca person identifier
   */
  protected Optional<Long> resolvePersIdOptional(OmuPrincipal principal) {
    return sessionStore.getCinecaPersId(principal.omuUserId(), principal.universityId());
  }
}