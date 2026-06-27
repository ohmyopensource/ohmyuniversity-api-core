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
 * <p>This design enforces stateless authentication at the application level, while still
 * supporting short-lived external sessions.
 */
@Component
public class CinecaSessionStore {

  private static final Logger log = LoggerFactory.getLogger(CinecaSessionStore.class);

  private static final Duration CINECA_JWT_TTL = Duration.ofDays(30);
  private static final Duration CINECA_AUTH_TTL = Duration.ofDays(30);
  private static final Duration CINECA_PERS_TTL = Duration.ofDays(30);
  private static final Duration CINECA_CAREER_TTL = Duration.ofDays(30);
  private static final Duration OMU_REFRESH_TTL = Duration.ofDays(7);
  private static final Duration USER_INFO_TTL = Duration.ofDays(7);

  private static final String KEY_CINECA_JWT = "cineca:jwt:%s:%s";
  private static final String KEY_CINECA_AUTH = "cineca:auth:%s:%s";
  private static final String KEY_CINECA_PERS = "cineca:pers:%s:%s";
  private static final String KEY_CINECA_STU_ID = "cineca:stuid:%s:%s";
  private static final String KEY_CINECA_MAT_ID = "cineca:matid:%s:%s";
  private static final String KEY_CINECA_MATRICOLA = "cineca:matricola:%s:%s";
  private static final String KEY_USER_ID = "omu:userid:%s";
  private static final String KEY_USER_NOME = "omu:nome:%s";
  private static final String KEY_USER_COGNOME = "omu:cognome:%s";
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
   * Stores the Cineca person identifier (persId) in Redis for a specific user and university
   * context.
   *
   * <p>The value is stored as a string with a TTL to avoid persistence beyond the validity of the
   * Cineca session. This identifier is later used to resolve student-related operations without
   * re-querying login data.
   *
   * @param omuUserId    internal OhMyUniversity user identifier
   * @param universityId target university identifier (tenant)
   * @param persId       Cineca person identifier to store
   */
  public void storeCinecaPersId(String omuUserId, String universityId, Long persId) {
    String key = String.format(KEY_CINECA_PERS, omuUserId, universityId);
    redis.opsForValue().set(key, persId.toString(), CINECA_PERS_TTL);
    log.debug("CinecaSessionStore: stored Cineca persId={} for user={} uni={}",
        persId, omuUserId, universityId);
  }

  /**
   * Retrieves the Cineca person identifier (persId) from Redis.
   *
   * <p>The value is parsed from its string representation back into a Long. If no value is found or
   * the key has expired, an empty Optional is returned.
   *
   * @param omuUserId    internal OhMyUniversity user identifier
   * @param universityId target university identifier (tenant)
   * @return optional containing the Cineca persId if present
   */
  public Optional<Long> getCinecaPersId(String omuUserId, String universityId) {
    String key = String.format(KEY_CINECA_PERS, omuUserId, universityId);
    String value = redis.opsForValue().get(key);
    if (value == null) {
      return Optional.empty();
    }
    return Optional.of(Long.parseLong(value));
  }

  /**
   * Stores the active Cineca career identifier (stuId) for JWT refresh.
   *
   * <p>Persisted with the same TTL as the Cineca JWT so that the refresh endpoint
   * can re-issue a fully populated access token without a new Cineca login.
   *
   * @param omuUserId    internal OhMyUniversity user identifier
   * @param universityId target university identifier (tenant)
   * @param stuId        Cineca student career identifier
   */
  public void storeStuId(String omuUserId, String universityId, Long stuId) {
    String key = String.format(KEY_CINECA_STU_ID, omuUserId, universityId);
    redis.opsForValue().set(key, stuId.toString(), CINECA_CAREER_TTL);
  }

  /**
   * Retrieves the active Cineca career identifier (stuId) from Redis.
   *
   * @param omuUserId    internal OhMyUniversity user identifier
   * @param universityId target university identifier (tenant)
   * @return optional containing stuId if present and not expired
   */
  public Optional<Long> getStuId(String omuUserId, String universityId) {
    String value = redis.opsForValue().get(
        String.format(KEY_CINECA_STU_ID, omuUserId, universityId));
    return value == null ? Optional.empty() : Optional.of(Long.parseLong(value));
  }

  /**
   * Stores the active Cineca career segment identifier (matId) for JWT refresh.
   *
   * @param omuUserId    internal OhMyUniversity user identifier
   * @param universityId target university identifier (tenant)
   * @param matId        Cineca career segment identifier
   */
  public void storeMatId(String omuUserId, String universityId, Long matId) {
    String key = String.format(KEY_CINECA_MAT_ID, omuUserId, universityId);
    redis.opsForValue().set(key, matId.toString(), CINECA_CAREER_TTL);
  }

  /**
   * Retrieves the active Cineca career segment identifier (matId) from Redis.
   *
   * @param omuUserId    internal OhMyUniversity user identifier
   * @param universityId target university identifier (tenant)
   * @return optional containing matId if present and not expired
   */
  public Optional<Long> getMatId(String omuUserId, String universityId) {
    String value = redis.opsForValue().get(
        String.format(KEY_CINECA_MAT_ID, omuUserId, universityId));
    return value == null ? Optional.empty() : Optional.of(Long.parseLong(value));
  }

  /**
   * Stores the active student registration number (matricola) for JWT refresh.
   *
   * @param omuUserId    internal OhMyUniversity user identifier
   * @param universityId target university identifier (tenant)
   * @param matricola    student registration number
   */
  public void storeMatricola(String omuUserId, String universityId, String matricola) {
    if (matricola == null) return;
    String key = String.format(KEY_CINECA_MATRICOLA, omuUserId, universityId);
    redis.opsForValue().set(key, matricola.replaceAll("[\r\n]", "_"), CINECA_CAREER_TTL);
  }

  /**
   * Retrieves the active student registration number (matricola) from Redis.
   *
   * @param omuUserId    internal OhMyUniversity user identifier
   * @param universityId target university identifier (tenant)
   * @return optional containing matricola if present and not expired
   */
  public Optional<String> getMatricola(String omuUserId, String universityId) {
    String value = redis.opsForValue().get(
        String.format(KEY_CINECA_MATRICOLA, omuUserId, universityId));
    return Optional.ofNullable(value);
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
    redis.delete(String.format(KEY_CINECA_PERS, omuUserId, universityId));
    redis.delete(String.format(KEY_CINECA_STU_ID, omuUserId, universityId));
    redis.delete(String.format(KEY_CINECA_MAT_ID, omuUserId, universityId));
    redis.delete(String.format(KEY_CINECA_MATRICOLA, omuUserId, universityId));
    log.info("CinecaSessionStore: cleared session for user={} uni={}", omuUserId, universityId);
  }

  public void storeUserNome(String omuUserId, String nome) {
    redis.opsForValue().set(String.format(KEY_USER_NOME, omuUserId), nome, USER_INFO_TTL);
  }

  public void storeUserCognome(String omuUserId, String cognome) {
    redis.opsForValue().set(String.format(KEY_USER_COGNOME, omuUserId), cognome, USER_INFO_TTL);
  }

  public Optional<String> getUserNome(String omuUserId) {
    return Optional.ofNullable(redis.opsForValue().get(String.format(KEY_USER_NOME, omuUserId)));
  }

  public Optional<String> getUserCognome(String omuUserId) {
    return Optional.ofNullable(redis.opsForValue().get(String.format(KEY_USER_COGNOME, omuUserId)));
  }

  public void storeUserId(String omuUserId, String userId) {
    redis.opsForValue().set(String.format(KEY_USER_ID, omuUserId), userId, USER_INFO_TTL);
  }

  public Optional<String> getUserId(String omuUserId) {
    return Optional.ofNullable(redis.opsForValue().get(String.format(KEY_USER_ID, omuUserId)));
  }
}