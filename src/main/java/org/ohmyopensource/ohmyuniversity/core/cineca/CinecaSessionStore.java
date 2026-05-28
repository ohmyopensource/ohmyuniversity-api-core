package org.ohmyopensource.ohmyuniversity.core.cineca;

import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Stores and manages Cineca and OhMyUniversity session tokens using Redis.
 *
 * <p>This component acts as a transient session store:
 * - Cineca JWT and auth tokens are cached with a strict TTL
 * - OhMyU refresh tokens are mapped to internal user IDs
 *
 * <p>No session-related data is persisted to the database. All entries expire automatically based
 * on their TTL, ensuring alignment with Cineca session constraints and improving security.
 *
 * <p>This design enforces stateless authentication at the application level, while still supporting
 * short-lived external sessions.
 */
@Component
public class CinecaSessionStore {

  private static final Logger log = LoggerFactory.getLogger(CinecaSessionStore.class);

  private static final Duration CINECA_JWT_TTL = Duration.ofMinutes(14);
  private static final Duration CINECA_AUTH_TTL = Duration.ofMinutes(14);
  private static final Duration OMU_REFRESH_TTL = Duration.ofDays(7);

  private static final String KEY_CINECA_JWT = "cineca:jwt:%s:%s";
  private static final String KEY_CINECA_AUTH = "cineca:auth:%s:%s";
  private static final String KEY_OMU_REFRESH = "omu:refresh:%s";

  private final StringRedisTemplate redis;

  // ============ Constructor ============

  /**
   * Creates a new session store backed by Redis.
   *
   * @param redis Redis template used for all session operations
   */
  public CinecaSessionStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  // ============ Class Methods ============

  /**
   * Stores a Cineca JWT for a specific user and university.
   *
   * @param omuUserId    internal user ID
   * @param universityId university identifier
   * @param jwt          Cineca JWT token
   */
  public void storeCinecaJwt(String omuUserId, String universityId, String jwt) {
    String key = String.format(KEY_CINECA_JWT, omuUserId, universityId);
    redis.opsForValue().set(key, jwt, CINECA_JWT_TTL);
    log.debug("CinecaSessionStore: stored Cineca JWT for user={} uni={}", omuUserId, universityId);
  }

  /**
   * Retrieves the Cineca JWT for a user and university.
   *
   * @param omuUserId    internal user ID
   * @param universityId university identifier
   * @return optional JWT if present and not expired
   */
  public Optional<String> getCinecaJwt(String omuUserId, String universityId) {
    String key = String.format(KEY_CINECA_JWT, omuUserId, universityId);
    return Optional.ofNullable(redis.opsForValue().get(key));
  }

  /**
   * Deletes the Cineca JWT for a user and university.
   *
   * @param omuUserId    internal user ID
   * @param universityId university identifier
   */
  public void deleteCinecaJwt(String omuUserId, String universityId) {
    redis.delete(String.format(KEY_CINECA_JWT, omuUserId, universityId));
  }

  /**
   * Stores Cineca auth/session token.
   *
   * @param omuUserId    internal user ID
   * @param universityId university identifier
   * @param authToken    Cineca session token
   */
  public void storeCinecaAuthToken(String omuUserId, String universityId, String authToken) {
    String key = String.format(KEY_CINECA_AUTH, omuUserId, universityId);
    redis.opsForValue().set(key, authToken, CINECA_AUTH_TTL);
  }

  /**
   * Retrieves Cineca auth/session token.
   *
   * @param omuUserId    internal user ID
   * @param universityId university identifier
   * @return optional auth token if present
   */
  public Optional<String> getCinecaAuthToken(String omuUserId, String universityId) {
    String key = String.format(KEY_CINECA_AUTH, omuUserId, universityId);
    return Optional.ofNullable(redis.opsForValue().get(key));
  }

  /**
   * Stores a refresh token mapped to a user ID.
   *
   * @param refreshToken refresh token
   * @param omuUserId    internal user ID
   */
  public void storeRefreshToken(String refreshToken, String omuUserId) {
    String key = String.format(KEY_OMU_REFRESH, refreshToken);
    redis.opsForValue().set(key, omuUserId, OMU_REFRESH_TTL);
  }

  /**
   * Resolves a user ID from a refresh token.
   *
   * @param refreshToken refresh token
   * @return optional user ID if token is valid
   */
  public Optional<String> getUserIdByRefreshToken(String refreshToken) {
    String key = String.format(KEY_OMU_REFRESH, refreshToken);
    return Optional.ofNullable(redis.opsForValue().get(key));
  }

  /**
   * Deletes a refresh token, invalidating the session.
   *
   * @param refreshToken refresh token
   */
  public void deleteRefreshToken(String refreshToken) {
    redis.delete(String.format(KEY_OMU_REFRESH, refreshToken));
  }

  /**
   * Clears all session data for a user and university.
   *
   * @param omuUserId    internal user ID
   * @param universityId university identifier
   */
  public void clearSession(String omuUserId, String universityId) {
    deleteCinecaJwt(omuUserId, universityId);
    redis.delete(String.format(KEY_CINECA_AUTH, omuUserId, universityId));
    log.info("CinecaSessionStore: cleared session for user={} uni={}", omuUserId, universityId);
  }
}