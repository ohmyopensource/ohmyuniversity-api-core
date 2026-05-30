package org.ohmyopensource.ohmyuniversity.core.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OmuUser} entity.
 *
 * <p>No JPA context — the entity is instantiated directly. The {@code @PrePersist}
 * lifecycle hook is invoked via its package-private accessor to simulate
 * the behaviour triggered by the JPA provider at persist time.
 */
class OmuUserTest {

  private OmuUser user;

  /**
   * Creates a fresh {@link OmuUser} instance before each test
   * to guarantee test isolation.
   */
  @BeforeEach
  void setUp() {
    user = new OmuUser();
  }

  /**
   * Verifies that each field exposed through getters and setters
   * correctly stores and returns its assigned value.
   */
  @Nested
  @DisplayName("Getters and setters")
  class GettersSetters {

    /**
     * Verifies that {@code codiceFiscale} survives a set/get round-trip
     * without mutation.
     */
    @Test
    @DisplayName("setCodiceFiscale / getCodiceFiscale round-trip")
    void codiceFiscale() {
      user.setCodiceFiscale("TSTXXX00A00X000X");

      assertThat(user.getCodiceFiscale()).isEqualTo("TSTXXX00A00X000X");
    }

    /**
     * Verifies that {@code emailPrimaria} survives a set/get round-trip
     * without mutation.
     */
    @Test
    @DisplayName("setEmailPrimaria / getEmailPrimaria round-trip")
    void emailPrimaria() {
      user.setEmailPrimaria("test@ohmyuniversity.it");

      assertThat(user.getEmailPrimaria()).isEqualTo("test@ohmyuniversity.it");
    }

    /**
     * Verifies that {@code lastLoginAt} survives a set/get round-trip
     * preserving the exact {@link Instant} reference.
     */
    @Test
    @DisplayName("setLastLoginAt / getLastLoginAt round-trip")
    void lastLoginAt() {
      Instant now = Instant.now();
      user.setLastLoginAt(now);

      assertThat(user.getLastLoginAt()).isEqualTo(now);
    }

    /**
     * Verifies that {@code id} is {@code null} on a transient entity
     * that has not yet been persisted.
     */
    @Test
    @DisplayName("getId() returns null before persistence")
    void idNullBeforePersist() {
      assertThat(user.getId()).isNull();
    }

    /**
     * Verifies that {@code createdAt} is {@code null} before the
     * {@code @PrePersist} lifecycle hook has been invoked.
     */
    @Test
    @DisplayName("getCreatedAt() returns null before @PrePersist")
    void createdAtNullBeforePrePersist() {
      assertThat(user.getCreatedAt()).isNull();
    }
  }

  /**
   * Verifies the behaviour of the {@code @PrePersist} lifecycle hook
   * exposed via {@link OmuUser#onCreate()}.
   */
  @Nested
  @DisplayName("@PrePersist lifecycle")
  class PrePersist {

    /**
     * Verifies that {@link OmuUser#onCreate()} sets {@code createdAt}
     * to a timestamp falling within the window delimited by instants
     * captured immediately before and after the invocation.
     */
    @Test
    @DisplayName("onCreate() sets createdAt to a recent timestamp")
    void setsCreatedAt() {
      Instant before = Instant.now();
      user.onCreate();
      Instant after = Instant.now();

      assertThat(user.getCreatedAt())
          .isNotNull()
          .isAfterOrEqualTo(before)
          .isBeforeOrEqualTo(after);
    }

    /**
     * Verifies that invoking {@link OmuUser#onCreate()} a second time
     * overwrites the previously stored {@code createdAt} timestamp.
     */
    @Test
    @DisplayName("onCreate() called twice → createdAt is overwritten")
    void calledTwiceOverwrites() throws Exception {
      user.onCreate();
      Instant first = user.getCreatedAt();

      Thread.sleep(5);
      user.onCreate();

      assertThat(user.getCreatedAt()).isAfter(first);
    }
  }

  /**
   * Verifies the default state of the {@code universityConnections} collection
   * on a newly instantiated {@link OmuUser}.
   */
  @Nested
  @DisplayName("universityConnections")
  class UniversityConnections {

    /**
     * Verifies that {@code universityConnections} is initialised as an
     * empty, non-null collection on entity construction.
     */
    @Test
    @DisplayName("default list is empty and not null")
    void defaultEmpty() {
      assertThat(user.getUniversityConnections())
          .isNotNull()
          .isEmpty();
    }
  }
}