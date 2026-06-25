package org.ohmyopensource.ohmyuniversity.core.cineca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Unit tests for {@link CinecaSessionStore} — covers methods added after the initial test suite:
 * storeUserNome/Cognome/UserId, getStuId/MatId/Matricola, storeMatricola null guard.
 */
@SuppressWarnings("unchecked")
class CinecaSessionStoreExtraTest {

  private StringRedisTemplate redis;
  private ValueOperations<String, String> valueOps;
  private CinecaSessionStore store;

  private static final String USER_ID = "auth0|abc123";
  private static final String UNI_ID = "UNIMOL";

  @BeforeEach
  void setUp() {
    redis = mock(StringRedisTemplate.class);
    valueOps = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(valueOps);
    store = new CinecaSessionStore(redis);
  }

  /**
   * Verifies storeUserNome and getUserNome.
   */
  @Nested
  @DisplayName("User Nome")
  class UserNome {

    /**
     * Verifies that storeUserNome writes to Redis with 7-day TTL.
     */
    @Test
    @DisplayName("store → set with correct key and 7-day TTL")
    void store() {
      store.storeUserNome(USER_ID, "ALESSIO");
      verify(valueOps).set("omu:nome:" + USER_ID, "ALESSIO", Duration.ofDays(7));
    }

    /**
     * Verifies that getUserNome returns value when present.
     */
    @Test
    @DisplayName("get → returns Optional.of when present")
    void getPresent() {
      when(valueOps.get("omu:nome:" + USER_ID)).thenReturn("ALESSIO");
      assertThat(store.getUserNome(USER_ID)).contains("ALESSIO");
    }

    /**
     * Verifies that getUserNome returns empty when absent.
     */
    @Test
    @DisplayName("get → returns Optional.empty when absent")
    void getAbsent() {
      when(valueOps.get("omu:nome:" + USER_ID)).thenReturn(null);
      assertThat(store.getUserNome(USER_ID)).isEmpty();
    }
  }

  /**
   * Verifies storeUserCognome and getUserCognome.
   */
  @Nested
  @DisplayName("User Cognome")
  class UserCognome {

    /**
     * Verifies that storeUserCognome writes to Redis with 7-day TTL.
     */
    @Test
    @DisplayName("store → set with correct key and 7-day TTL")
    void store() {
      store.storeUserCognome(USER_ID, "DEL MUTO");
      verify(valueOps).set("omu:cognome:" + USER_ID, "DEL MUTO", Duration.ofDays(7));
    }

    /**
     * Verifies that getUserCognome returns value when present.
     */
    @Test
    @DisplayName("get → returns Optional.of when present")
    void getPresent() {
      when(valueOps.get("omu:cognome:" + USER_ID)).thenReturn("DEL MUTO");
      assertThat(store.getUserCognome(USER_ID)).contains("DEL MUTO");
    }
  }

  /**
   * Verifies storeUserId and getUserId.
   */
  @Nested
  @DisplayName("User ID")
  class UserId {

    /**
     * Verifies that storeUserId writes to Redis with 7-day TTL.
     */
    @Test
    @DisplayName("store → set with correct key and 7-day TTL")
    void store() {
      store.storeUserId(USER_ID, "a.delmuto");
      verify(valueOps).set("omu:userid:" + USER_ID, "a.delmuto", Duration.ofDays(7));
    }

    /**
     * Verifies that getUserId returns value when present.
     */
    @Test
    @DisplayName("get → returns Optional.of when present")
    void getPresent() {
      when(valueOps.get("omu:userid:" + USER_ID)).thenReturn("a.delmuto");
      assertThat(store.getUserId(USER_ID)).contains("a.delmuto");
    }

    /**
     * Verifies that getUserId returns empty when absent.
     */
    @Test
    @DisplayName("get → returns Optional.empty when absent")
    void getAbsent() {
      when(valueOps.get("omu:userid:" + USER_ID)).thenReturn(null);
      assertThat(store.getUserId(USER_ID)).isEmpty();
    }
  }

  /**
   * Verifies storeStuId and getStuId.
   */
  @Nested
  @DisplayName("StuId")
  class StuId {

    /**
     * Verifies that storeStuId writes correct key with 90-min TTL.
     */
    @Test
    @DisplayName("store → set with correct key and 90-min TTL")
    void store() {
      store.storeStuId(USER_ID, UNI_ID, 89486L);
      verify(valueOps).set(
          "cineca:stuid:" + USER_ID + ":" + UNI_ID,
          "89486",
          Duration.ofMinutes(90));
    }

    /**
     * Verifies that getStuId parses string back to Long.
     */
    @Test
    @DisplayName("get → parses string back to Long")
    void getPresent() {
      when(valueOps.get("cineca:stuid:" + USER_ID + ":" + UNI_ID)).thenReturn("89486");
      assertThat(store.getStuId(USER_ID, UNI_ID)).contains(89486L);
    }

    /**
     * Verifies that getStuId returns empty when absent.
     */
    @Test
    @DisplayName("get → returns Optional.empty when absent")
    void getAbsent() {
      when(valueOps.get("cineca:stuid:" + USER_ID + ":" + UNI_ID)).thenReturn(null);
      assertThat(store.getStuId(USER_ID, UNI_ID)).isEmpty();
    }
  }

  /**
   * Verifies storeMatId and getMatId.
   */
  @Nested
  @DisplayName("MatId")
  class MatId {

    /**
     * Verifies that storeMatId writes correct key with 90-min TTL.
     */
    @Test
    @DisplayName("store → set with correct key and 90-min TTL")
    void store() {
      store.storeMatId(USER_ID, UNI_ID, 106279L);
      verify(valueOps).set(
          "cineca:matid:" + USER_ID + ":" + UNI_ID,
          "106279",
          Duration.ofMinutes(90));
    }

    /**
     * Verifies that getMatId parses string back to Long.
     */
    @Test
    @DisplayName("get → parses string back to Long")
    void getPresent() {
      when(valueOps.get("cineca:matid:" + USER_ID + ":" + UNI_ID)).thenReturn("106279");
      assertThat(store.getMatId(USER_ID, UNI_ID)).contains(106279L);
    }
  }

  /**
   * Verifies storeMatricola and getMatricola — including null guard and sanitization.
   */
  @Nested
  @DisplayName("Matricola")
  class Matricola {

    /**
     * Verifies that storeMatricola writes sanitized value with 90-min TTL.
     */
    @Test
    @DisplayName("store → writes sanitized matricola with 90-min TTL")
    void store() {
      store.storeMatricola(USER_ID, UNI_ID, "178026");
      verify(valueOps).set(
          "cineca:matricola:" + USER_ID + ":" + UNI_ID,
          "178026",
          Duration.ofMinutes(90));
    }

    /**
     * Verifies that storeMatricola does nothing when value is null.
     */
    @Test
    @DisplayName("store → does nothing when matricola is null")
    void storeNullIsNoOp() {
      store.storeMatricola(USER_ID, UNI_ID, null);
      verify(valueOps, never()).set(
          org.mockito.ArgumentMatchers.any(),
          org.mockito.ArgumentMatchers.any(),
          org.mockito.ArgumentMatchers.any());
    }

    /**
     * Verifies that newlines in matricola are replaced with underscore.
     */
    @Test
    @DisplayName("store → sanitizes newline characters")
    void sanitizesNewlines() {
      store.storeMatricola(USER_ID, UNI_ID, "178\n026");
      verify(valueOps).set(
          "cineca:matricola:" + USER_ID + ":" + UNI_ID,
          "178_026",
          Duration.ofMinutes(90));
    }

    /**
     * Verifies that getMatricola returns value when present.
     */
    @Test
    @DisplayName("get → returns Optional.of when present")
    void getPresent() {
      when(valueOps.get("cineca:matricola:" + USER_ID + ":" + UNI_ID)).thenReturn("178026");
      assertThat(store.getMatricola(USER_ID, UNI_ID)).contains("178026");
    }

    /**
     * Verifies that getMatricola returns empty when absent.
     */
    @Test
    @DisplayName("get → returns Optional.empty when absent")
    void getAbsent() {
      when(valueOps.get("cineca:matricola:" + USER_ID + ":" + UNI_ID)).thenReturn(null);
      assertThat(store.getMatricola(USER_ID, UNI_ID)).isEmpty();
    }
  }
}