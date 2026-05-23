package org.ohmyopensource.ohmyuniversity.core.cineca;

import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Stores and retrieves Cineca session tokens in Redis.
 *
 * Cineca tokens are NEVER persisted to the database — they live only in Redis
 * with TTL. When the TTL expires the user must re-authenticate.
 *
 * TTL values match Cineca's session limits:
 * Cineca JWT expires after ~15 minutes (confirmed from real response).
 * We store it with 14 minutes TTL to avoid using an already-expired token.
 * The OhMyU refresh token lives for 7 days — the user stays logged in
 * until they explicitly logout or the refresh token expires.
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

  public CinecaSessionStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  // ================================
  // Cineca JWT
  // ================================

  public void storeCinecaJwt(String omuUserId, String universityId, String jwt) {
    String key = String.format(KEY_CINECA_JWT, omuUserId, universityId);
    redis.opsForValue().set(key, jwt, CINECA_JWT_TTL);
    log.debug("CinecaSessionStore: stored Cineca JWT for user={} uni={}", omuUserId, universityId);
  }

  public Optional<String> getCinecaJwt(String omuUserId, String universityId) {
    String key = String.format(KEY_CINECA_JWT, omuUserId, universityId);
    return Optional.ofNullable(redis.opsForValue().get(key));
  }

  public void deleteCinecaJwt(String omuUserId, String universityId) {
    redis.delete(String.format(KEY_CINECA_JWT, omuUserId, universityId));
  }

  // ================================
  // Cineca authToken (jsessionid)
  // ================================

  public void storeCinecaAuthToken(String omuUserId, String universityId, String authToken) {
    String key = String.format(KEY_CINECA_AUTH, omuUserId, universityId);
    redis.opsForValue().set(key, authToken, CINECA_AUTH_TTL);
  }

  public Optional<String> getCinecaAuthToken(String omuUserId, String universityId) {
    String key = String.format(KEY_CINECA_AUTH, omuUserId, universityId);
    return Optional.ofNullable(redis.opsForValue().get(key));
  }

  // ================================
  // OhMyU refresh token
  // ================================

  /**
   * Stores the OhMyU refresh token mapping to the omuUserId.
   * When the client presents a refresh token, we look up the user from Redis.
   */
  public void storeRefreshToken(String refreshToken, String omuUserId) {
    String key = String.format(KEY_OMU_REFRESH, refreshToken);
    redis.opsForValue().set(key, omuUserId, OMU_REFRESH_TTL);
  }

  public Optional<String> getUserIdByRefreshToken(String refreshToken) {
    String key = String.format(KEY_OMU_REFRESH, refreshToken);
    return Optional.ofNullable(redis.opsForValue().get(key));
  }

  public void deleteRefreshToken(String refreshToken) {
    redis.delete(String.format(KEY_OMU_REFRESH, refreshToken));
  }

  /**
   * Deletes all session data for a user at a specific university.
   * Called on logout.
   */
  public void clearSession(String omuUserId, String universityId) {
    deleteCinecaJwt(omuUserId, universityId);
    redis.delete(String.format(KEY_CINECA_AUTH, omuUserId, universityId));
    log.info("CinecaSessionStore: cleared session for user={} uni={}", omuUserId, universityId);
  }
}