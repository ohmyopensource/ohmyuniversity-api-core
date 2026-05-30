package org.ohmyopensource.ohmyuniversity.core.cineca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Unit tests for {@link CinecaSessionStore}.
 *
 * <p>{@link StringRedisTemplate} and {@link ValueOperations} are mocked via
 * Mockito — no running Redis instance is required. Tests verify Redis key
 * formatting, TTL values, {@link Optional} wrapping, {@link Long} parsing,
 * and key deletion behaviour.
 */
@SuppressWarnings("unchecked")
class CinecaSessionStoreTest {

  private StringRedisTemplate redis;
  private ValueOperations<String, String> valueOps;
  private CinecaSessionStore store;

  /** OhMyUniversity user identifier used as a stable fixture across all tests. */
  private static final String USER_ID = "auth0|abc123";

  /** University identifier used as a stable fixture across all tests. */
  private static final String UNI_ID  = "UNIMOL";

  /**
   * Initialises fresh mocks and a new {@link CinecaSessionStore} instance
   * before each test to guarantee isolation.
   */
  @BeforeEach
  void setUp() {
    redis    = mock(StringRedisTemplate.class);
    valueOps = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(valueOps);
    store = new CinecaSessionStore(redis);
  }

  /**
   * Verifies the storage, retrieval, and deletion behaviour for
   * Cineca JWT tokens managed under the {@code cineca:jwt:} key namespace.
   */
  @Nested
  @DisplayName("Cineca JWT")
  class CinecaJwt {

    /**
     * Verifies that {@link CinecaSessionStore#storeCinecaJwt} writes to Redis
     * using the expected composite key and a 14-minute TTL.
     */
    @Test
    @DisplayName("store → set with correct key and 14-minute TTL")
    void store() {
      store.storeCinecaJwt(USER_ID, UNI_ID, "jwt-token");

      verify(valueOps).set(
          "cineca:jwt:" + USER_ID + ":" + UNI_ID,
          "jwt-token",
          Duration.ofMinutes(14));
    }

    /**
     * Verifies that {@link CinecaSessionStore#getCinecaJwt} returns a non-empty
     * {@link Optional} containing the stored value when the key is present in Redis.
     */
    @Test
    @DisplayName("get → returns Optional.of when key present")
    void getPresent() {
      when(valueOps.get("cineca:jwt:" + USER_ID + ":" + UNI_ID)).thenReturn("jwt-token");

      Optional<String> result = store.getCinecaJwt(USER_ID, UNI_ID);

      assertThat(result).contains("jwt-token");
    }

    /**
     * Verifies that {@link CinecaSessionStore#getCinecaJwt} returns an empty
     * {@link Optional} when the key is absent from Redis.
     */
    @Test
    @DisplayName("get → returns Optional.empty when key absent")
    void getAbsent() {
      when(valueOps.get("cineca:jwt:" + USER_ID + ":" + UNI_ID)).thenReturn(null);

      assertThat(store.getCinecaJwt(USER_ID, UNI_ID)).isEmpty();
    }

    /**
     * Verifies that {@link CinecaSessionStore#deleteCinecaJwt} removes the
     * correct composite key from Redis.
     */
    @Test
    @DisplayName("delete → deletes correct key")
    void delete() {
      store.deleteCinecaJwt(USER_ID, UNI_ID);

      verify(redis).delete("cineca:jwt:" + USER_ID + ":" + UNI_ID);
    }
  }

  /**
   * Verifies the storage and retrieval behaviour for Cineca authentication
   * tokens managed under the {@code cineca:auth:} key namespace.
   */
  @Nested
  @DisplayName("Cineca Auth Token")
  class CinecaAuthToken {

    /**
     * Verifies that {@link CinecaSessionStore#storeCinecaAuthToken} writes to
     * Redis using the expected composite key and a 14-minute TTL.
     */
    @Test
    @DisplayName("store → set with correct key and 14-minute TTL")
    void store() {
      store.storeCinecaAuthToken(USER_ID, UNI_ID, "auth-token");

      verify(valueOps).set(
          "cineca:auth:" + USER_ID + ":" + UNI_ID,
          "auth-token",
          Duration.ofMinutes(14));
    }

    /**
     * Verifies that {@link CinecaSessionStore#getCinecaAuthToken} returns a
     * non-empty {@link Optional} when the key is present in Redis.
     */
    @Test
    @DisplayName("get → returns Optional.of when present")
    void getPresent() {
      when(valueOps.get("cineca:auth:" + USER_ID + ":" + UNI_ID)).thenReturn("auth-token");

      assertThat(store.getCinecaAuthToken(USER_ID, UNI_ID)).contains("auth-token");
    }

    /**
     * Verifies that {@link CinecaSessionStore#getCinecaAuthToken} returns an
     * empty {@link Optional} when the key is absent from Redis.
     */
    @Test
    @DisplayName("get → returns Optional.empty when absent")
    void getAbsent() {
      when(valueOps.get("cineca:auth:" + USER_ID + ":" + UNI_ID)).thenReturn(null);

      assertThat(store.getCinecaAuthToken(USER_ID, UNI_ID)).isEmpty();
    }
  }

  /**
   * Verifies the storage and retrieval behaviour for Cineca person identifiers
   * managed under the {@code cineca:pers:} key namespace, including
   * {@link Long} serialisation and deserialisation.
   */
  @Nested
  @DisplayName("Cineca PersId")
  class CinecaPersId {

    /**
     * Verifies that {@link CinecaSessionStore#storeCinecaPersId} serialises the
     * {@link Long} value to its string representation and writes it to Redis
     * with the expected composite key and a 14-minute TTL.
     */
    @Test
    @DisplayName("store → persId serialized to string with correct key and TTL")
    void store() {
      store.storeCinecaPersId(USER_ID, UNI_ID, 99999L);

      verify(valueOps).set(
          "cineca:pers:" + USER_ID + ":" + UNI_ID,
          "99999",
          Duration.ofMinutes(14));
    }

    /**
     * Verifies that {@link CinecaSessionStore#getCinecaPersId} correctly parses
     * the string value stored in Redis back to a {@link Long}.
     */
    @Test
    @DisplayName("get → parses string back to Long correctly")
    void getPresent() {
      when(valueOps.get("cineca:pers:" + USER_ID + ":" + UNI_ID)).thenReturn("99999");

      Optional<Long> result = store.getCinecaPersId(USER_ID, UNI_ID);

      assertThat(result).contains(99999L);
    }

    /**
     * Verifies that {@link CinecaSessionStore#getCinecaPersId} returns an empty
     * {@link Optional} when the key is absent from Redis.
     */
    @Test
    @DisplayName("get → returns Optional.empty when absent")
    void getAbsent() {
      when(valueOps.get("cineca:pers:" + USER_ID + ":" + UNI_ID)).thenReturn(null);

      assertThat(store.getCinecaPersId(USER_ID, UNI_ID)).isEmpty();
    }
  }

  /**
   * Verifies the storage, retrieval, and deletion behaviour for
   * OhMyUniversity refresh tokens managed under the {@code omu:refresh:} key namespace.
   */
  @Nested
  @DisplayName("OhMyU Refresh Token")
  class RefreshToken {

    /**
     * Verifies that {@link CinecaSessionStore#storeRefreshToken} writes the
     * user identifier to Redis under the expected key with a 7-day TTL.
     */
    @Test
    @DisplayName("store → set with correct key and 7-day TTL")
    void store() {
      store.storeRefreshToken("refresh-abc", USER_ID);

      verify(valueOps).set(
          "omu:refresh:refresh-abc",
          USER_ID,
          Duration.ofDays(7));
    }

    /**
     * Verifies that {@link CinecaSessionStore#getUserIdByRefreshToken} returns
     * a non-empty {@link Optional} containing the associated user identifier
     * when the refresh token key is present in Redis.
     */
    @Test
    @DisplayName("getUserId → returns Optional.of when token valid")
    void getPresent() {
      when(valueOps.get("omu:refresh:refresh-abc")).thenReturn(USER_ID);

      assertThat(store.getUserIdByRefreshToken("refresh-abc")).contains(USER_ID);
    }

    /**
     * Verifies that {@link CinecaSessionStore#getUserIdByRefreshToken} returns
     * an empty {@link Optional} when the refresh token has expired or was
     * never stored.
     */
    @Test
    @DisplayName("getUserId → returns Optional.empty when token expired or absent")
    void getAbsent() {
      when(valueOps.get("omu:refresh:refresh-abc")).thenReturn(null);

      assertThat(store.getUserIdByRefreshToken("refresh-abc")).isEmpty();
    }

    /**
     * Verifies that {@link CinecaSessionStore#deleteRefreshToken} removes the
     * correct key from Redis.
     */
    @Test
    @DisplayName("delete → deletes correct key")
    void delete() {
      store.deleteRefreshToken("refresh-abc");

      verify(redis).delete("omu:refresh:refresh-abc");
    }
  }

  /**
   * Verifies the composite session cleanup performed by
   * {@link CinecaSessionStore#clearSession(String, String)}.
   */
  @Nested
  @DisplayName("clearSession")
  class ClearSession {

    /**
     * Verifies that {@link CinecaSessionStore#clearSession} deletes all three
     * session keys — JWT, auth token, and person identifier — for the given
     * user and university combination.
     */
    @Test
    @DisplayName("deletes JWT, auth and pers keys for the given user and university")
    void deletesAllThreeKeys() {
      store.clearSession(USER_ID, UNI_ID);

      verify(redis).delete("cineca:jwt:"  + USER_ID + ":" + UNI_ID);
      verify(redis).delete("cineca:auth:" + USER_ID + ":" + UNI_ID);
      verify(redis).delete("cineca:pers:" + USER_ID + ":" + UNI_ID);
    }
  }
}