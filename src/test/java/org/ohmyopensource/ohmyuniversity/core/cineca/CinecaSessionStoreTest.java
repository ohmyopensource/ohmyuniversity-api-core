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
 * StringRedisTemplate and ValueOperations are mocked — no Redis instance required.
 * Tests verify key format, TTL, Optional wrapping, Long parsing, and delete behaviour.
 */
@SuppressWarnings("unchecked")
class CinecaSessionStoreTest {

  private StringRedisTemplate redis;
  private ValueOperations<String, String> valueOps;
  private CinecaSessionStore store;

  private static final String USER_ID = "auth0|abc123";
  private static final String UNI_ID  = "UNIMOL";

  @BeforeEach
  void setUp() {
    redis    = mock(StringRedisTemplate.class);
    valueOps = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(valueOps);
    store = new CinecaSessionStore(redis);
  }

  // ============================================================
  // Cineca JWT
  // ============================================================

  @Nested
  @DisplayName("Cineca JWT")
  class CinecaJwt {

    @Test
    @DisplayName("store → set with correct key and 14-minute TTL")
    void store() {
      store.storeCinecaJwt(USER_ID, UNI_ID, "jwt-token");

      verify(valueOps).set(
          "cineca:jwt:" + USER_ID + ":" + UNI_ID,
          "jwt-token",
          Duration.ofMinutes(14));
    }

    @Test
    @DisplayName("get → returns Optional.of when key present")
    void getPresent() {
      when(valueOps.get("cineca:jwt:" + USER_ID + ":" + UNI_ID)).thenReturn("jwt-token");

      Optional<String> result = store.getCinecaJwt(USER_ID, UNI_ID);

      assertThat(result).contains("jwt-token");
    }

    @Test
    @DisplayName("get → returns Optional.empty when key absent")
    void getAbsent() {
      when(valueOps.get("cineca:jwt:" + USER_ID + ":" + UNI_ID)).thenReturn(null);

      assertThat(store.getCinecaJwt(USER_ID, UNI_ID)).isEmpty();
    }

    @Test
    @DisplayName("delete → deletes correct key")
    void delete() {
      store.deleteCinecaJwt(USER_ID, UNI_ID);

      verify(redis).delete("cineca:jwt:" + USER_ID + ":" + UNI_ID);
    }
  }

  // ============================================================
  // Cineca Auth Token
  // ============================================================

  @Nested
  @DisplayName("Cineca Auth Token")
  class CinecaAuthToken {

    @Test
    @DisplayName("store → set with correct key and 14-minute TTL")
    void store() {
      store.storeCinecaAuthToken(USER_ID, UNI_ID, "auth-token");

      verify(valueOps).set(
          "cineca:auth:" + USER_ID + ":" + UNI_ID,
          "auth-token",
          Duration.ofMinutes(14));
    }

    @Test
    @DisplayName("get → returns Optional.of when present")
    void getPresent() {
      when(valueOps.get("cineca:auth:" + USER_ID + ":" + UNI_ID)).thenReturn("auth-token");

      assertThat(store.getCinecaAuthToken(USER_ID, UNI_ID)).contains("auth-token");
    }

    @Test
    @DisplayName("get → returns Optional.empty when absent")
    void getAbsent() {
      when(valueOps.get("cineca:auth:" + USER_ID + ":" + UNI_ID)).thenReturn(null);

      assertThat(store.getCinecaAuthToken(USER_ID, UNI_ID)).isEmpty();
    }
  }

  // ============================================================
  // Cineca PersId
  // ============================================================

  @Nested
  @DisplayName("Cineca PersId")
  class CinecaPersId {

    @Test
    @DisplayName("store → persId serialized to string with correct key and TTL")
    void store() {
      store.storeCinecaPersId(USER_ID, UNI_ID, 99999L);

      verify(valueOps).set(
          "cineca:pers:" + USER_ID + ":" + UNI_ID,
          "99999",
          Duration.ofMinutes(14));
    }

    @Test
    @DisplayName("get → parses string back to Long correctly")
    void getPresent() {
      when(valueOps.get("cineca:pers:" + USER_ID + ":" + UNI_ID)).thenReturn("99999");

      Optional<Long> result = store.getCinecaPersId(USER_ID, UNI_ID);

      assertThat(result).contains(99999L);
    }

    @Test
    @DisplayName("get → returns Optional.empty when absent")
    void getAbsent() {
      when(valueOps.get("cineca:pers:" + USER_ID + ":" + UNI_ID)).thenReturn(null);

      assertThat(store.getCinecaPersId(USER_ID, UNI_ID)).isEmpty();
    }
  }

  // ============================================================
  // OhMyU Refresh Token
  // ============================================================

  @Nested
  @DisplayName("OhMyU Refresh Token")
  class RefreshToken {

    @Test
    @DisplayName("store → set with correct key and 7-day TTL")
    void store() {
      store.storeRefreshToken("refresh-abc", USER_ID);

      verify(valueOps).set(
          "omu:refresh:refresh-abc",
          USER_ID,
          Duration.ofDays(7));
    }

    @Test
    @DisplayName("getUserId → returns Optional.of when token valid")
    void getPresent() {
      when(valueOps.get("omu:refresh:refresh-abc")).thenReturn(USER_ID);

      assertThat(store.getUserIdByRefreshToken("refresh-abc")).contains(USER_ID);
    }

    @Test
    @DisplayName("getUserId → returns Optional.empty when token expired or absent")
    void getAbsent() {
      when(valueOps.get("omu:refresh:refresh-abc")).thenReturn(null);

      assertThat(store.getUserIdByRefreshToken("refresh-abc")).isEmpty();
    }

    @Test
    @DisplayName("delete → deletes correct key")
    void delete() {
      store.deleteRefreshToken("refresh-abc");

      verify(redis).delete("omu:refresh:refresh-abc");
    }
  }

  // ============================================================
  // clearSession
  // ============================================================

  @Nested
  @DisplayName("clearSession")
  class ClearSession {

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