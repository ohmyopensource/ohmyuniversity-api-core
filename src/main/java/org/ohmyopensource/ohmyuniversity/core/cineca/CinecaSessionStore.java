package org.ohmyopensource.ohmyuniversity.core.cineca;

import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Read-only access to vendor session data cached by the auth service.
 *
 * <p>The auth service (ohmyuniversity-auth) owns all writes to this Redis keyspace —
 * see its {@code AuthSessionStore} — issued at login and cleared at logout. This class only reads
 * what auth has already written, using the exact same key format (including the {@code {omuUserId}}
 * Redis Cluster hashtag) so that both services agree on where the data lives even if Redis is later
 * sharded.
 *
 * <p>All write/session-lifecycle methods (login, logout, refresh token management,
 * tracked sessions) moved to the auth service entirely — this class has none of them.
 */
@Component
public class CinecaSessionStore {

  private static final String KEY_VENDOR_TOKEN = "auth:{%s}:vendor-token:%s";
  private static final String KEY_VENDOR_AUTH = "auth:{%s}:vendor-auth:%s";
  private static final String KEY_VENDOR_PERS_ID = "auth:{%s}:vendor-pers-id:%s";
  private static final String KEY_STU_ID = "auth:{%s}:stu-id:%s";
  private static final String KEY_MAT_ID = "auth:{%s}:mat-id:%s";
  private static final String KEY_MATRICOLA = "auth:{%s}:matricola:%s";
  private static final String KEY_USER_NOME = "auth:{%s}:nome";
  private static final String KEY_USER_COGNOME = "auth:{%s}:cognome";
  private static final String KEY_EXTERNAL_USER_ID = "auth:{%s}:external-user-id";

  private final StringRedisTemplate redis;

  // ============ Constructor ============

  /**
   * Creates the store backed by the given Redis template.
   *
   * @param redis Redis template used to read the vendor session keyspace
   */
  public CinecaSessionStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  // ============ Class Methods ============

  /**
   * Retrieves the cached vendor-issued JWT (e.g. Cineca JWT) for a user/university pair.
   *
   * @param omuUserId    internal user identifier
   * @param universityId university tenant identifier
   * @return the cached JWT, or empty if none is cached or the session has expired
   */
  public Optional<String> getCinecaJwt(String omuUserId, String universityId) {
    return Optional.ofNullable(
        redis.opsForValue().get(String.format(KEY_VENDOR_TOKEN, omuUserId, universityId)));
  }

  /**
   * Retrieves the cached vendor-issued legacy session token (e.g. Cineca authToken) for a
   * user/university pair.
   *
   * @param omuUserId    internal user identifier
   * @param universityId university tenant identifier
   * @return the cached auth token, or empty if none is cached or the session has expired
   */
  public Optional<String> getCinecaAuthToken(String omuUserId, String universityId) {
    return Optional.ofNullable(
        redis.opsForValue().get(String.format(KEY_VENDOR_AUTH, omuUserId, universityId)));
  }

  /**
   * Retrieves the cached vendor-side person identifier (e.g. Cineca persId) for a user/university
   * pair.
   *
   * @param omuUserId    internal user identifier
   * @param universityId university tenant identifier
   * @return the cached person identifier, or empty if none is cached
   */
  public Optional<Long> getCinecaPersId(String omuUserId, String universityId) {
    String value = redis.opsForValue().get(
        String.format(KEY_VENDOR_PERS_ID, omuUserId, universityId));
    return value == null ? Optional.empty() : Optional.of(Long.parseLong(value));
  }

  /**
   * Retrieves the cached active career identifier (e.g. Cineca stuId) for a user/university pair.
   *
   * @param omuUserId    internal user identifier
   * @param universityId university tenant identifier
   * @return the cached career identifier, or empty if none is cached
   */
  public Optional<Long> getStuId(String omuUserId, String universityId) {
    String value = redis.opsForValue().get(String.format(KEY_STU_ID, omuUserId, universityId));
    return value == null ? Optional.empty() : Optional.of(Long.parseLong(value));
  }

  /**
   * Retrieves the cached active career segment identifier (e.g. Cineca matId) for a user/university
   * pair.
   *
   * @param omuUserId    internal user identifier
   * @param universityId university tenant identifier
   * @return the cached career segment identifier, or empty if none is cached
   */
  public Optional<Long> getMatId(String omuUserId, String universityId) {
    String value = redis.opsForValue().get(String.format(KEY_MAT_ID, omuUserId, universityId));
    return value == null ? Optional.empty() : Optional.of(Long.parseLong(value));
  }

  /**
   * Retrieves the cached active student registration number (matricola) for a user/university
   * pair.
   *
   * @param omuUserId    internal user identifier
   * @param universityId university tenant identifier
   * @return the cached matricola, or empty if none is cached
   */
  public Optional<String> getMatricola(String omuUserId, String universityId) {
    return Optional.ofNullable(
        redis.opsForValue().get(String.format(KEY_MATRICOLA, omuUserId, universityId)));
  }

  /**
   * Retrieves the cached first name for a user, shared across all their university connections.
   *
   * @param omuUserId internal user identifier
   * @return the cached first name, or empty if none is cached
   */
  public Optional<String> getUserNome(String omuUserId) {
    return Optional.ofNullable(redis.opsForValue().get(String.format(KEY_USER_NOME, omuUserId)));
  }

  /**
   * Retrieves the cached last name for a user, shared across all their university connections.
   *
   * @param omuUserId internal user identifier
   * @return the cached last name, or empty if none is cached
   */
  public Optional<String> getUserCognome(String omuUserId) {
    return Optional.ofNullable(
        redis.opsForValue().get(String.format(KEY_USER_COGNOME, omuUserId)));
  }

  /**
   * Retrieves the cached vendor-side external user identifier (e.g. Cineca username) for a user,
   * shared across all their university connections.
   *
   * @param omuUserId internal user identifier
   * @return the cached external user identifier, or empty if none is cached
   */
  public Optional<String> getUserId(String omuUserId) {
    return Optional.ofNullable(
        redis.opsForValue().get(String.format(KEY_EXTERNAL_USER_ID, omuUserId)));
  }
}