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
 * No JPA context — entity instantiated directly.
 * @PrePersist lifecycle hook invoked via package-private access.
 */
class OmuUserTest {

  private OmuUser user;

  @BeforeEach
  void setUp() {
    user = new OmuUser();
  }

  // ============================================================
  // Getters / Setters
  // ============================================================

  @Nested
  @DisplayName("Getters and setters")
  class GettersSetters {

    @Test
    @DisplayName("setCodiceFiscale / getCodiceFiscale round-trip")
    void codiceFiscale() {
      user.setCodiceFiscale("TSTXXX00A00X000X");

      assertThat(user.getCodiceFiscale()).isEqualTo("TSTXXX00A00X000X");
    }

    @Test
    @DisplayName("setEmailPrimaria / getEmailPrimaria round-trip")
    void emailPrimaria() {
      user.setEmailPrimaria("test@ohmyuniversity.it");

      assertThat(user.getEmailPrimaria()).isEqualTo("test@ohmyuniversity.it");
    }

    @Test
    @DisplayName("setLastLoginAt / getLastLoginAt round-trip")
    void lastLoginAt() {
      Instant now = Instant.now();
      user.setLastLoginAt(now);

      assertThat(user.getLastLoginAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("getId() returns null before persistence")
    void idNullBeforePersist() {
      assertThat(user.getId()).isNull();
    }

    @Test
    @DisplayName("getCreatedAt() returns null before @PrePersist")
    void createdAtNullBeforePrePersist() {
      assertThat(user.getCreatedAt()).isNull();
    }
  }

  // ============================================================
  // @PrePersist — onCreate()
  // ============================================================

  @Nested
  @DisplayName("@PrePersist lifecycle")
  class PrePersist {

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

  // ============================================================
  // universityConnections default state
  // ============================================================

  @Nested
  @DisplayName("universityConnections")
  class UniversityConnections {

    @Test
    @DisplayName("default list is empty and not null")
    void defaultEmpty() {
      assertThat(user.getUniversityConnections())
          .isNotNull()
          .isEmpty();
    }
  }
}