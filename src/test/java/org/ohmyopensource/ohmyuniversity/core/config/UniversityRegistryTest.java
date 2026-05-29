package org.ohmyopensource.ohmyuniversity.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UniversityRegistry}.
 *
 * No Spring context — registry is instantiated directly and populated via setUniversities().
 */
class UniversityRegistryTest {

  private UniversityRegistry registry;

  private static final UniversityRegistry.UniversityConfig UNIMOL_CONFIG =
      new UniversityRegistry.UniversityConfig(
          "Università degli Studi del Molise",
          "https://unimol.esse3.cineca.it/e3rest/api");

  @BeforeEach
  void setUp() {
    registry = new UniversityRegistry();
    registry.setUniversities(Map.of("UNIMOL", UNIMOL_CONFIG));
  }

  // ============================================================
  // resolve()
  // ============================================================

  @Nested
  @DisplayName("resolve()")
  class Resolve {

    @Test
    @DisplayName("known universityId → returns config")
    void knownId() {
      Optional<UniversityRegistry.UniversityConfig> result = registry.resolve("UNIMOL");

      assertThat(result).isPresent();
      assertThat(result.get().name())
          .isEqualTo("Università degli Studi del Molise");
      assertThat(result.get().baseUrl())
          .isEqualTo("https://unimol.esse3.cineca.it/e3rest/api");
    }

    @Test
    @DisplayName("unknown universityId → empty Optional")
    void unknownId() {
      assertThat(registry.resolve("UNICAM")).isEmpty();
    }

    @Test
    @DisplayName("null universityId → empty Optional, no NullPointerException")
    void nullId() {
      assertThat(registry.resolve(null)).isEmpty();
    }

    @Test
    @DisplayName("lowercase universityId → resolved case-insensitively")
    void caseInsensitive() {
      assertThat(registry.resolve("unimol")).isPresent();
      assertThat(registry.resolve("Unimol")).isPresent();
    }
  }

  // ============================================================
  // UniversityConfig record
  // ============================================================

  @Nested
  @DisplayName("UniversityConfig record")
  class UniversityConfigTests {

    @Test
    @DisplayName("name() and baseUrl() return correct values")
    void recordAccessors() {
      UniversityRegistry.UniversityConfig config =
          new UniversityRegistry.UniversityConfig("Test Uni", "https://test.esse3.it/api");

      assertThat(config.name()).isEqualTo("Test Uni");
      assertThat(config.baseUrl()).isEqualTo("https://test.esse3.it/api");
    }

    @Test
    @DisplayName("two configs with same values are equal")
    void recordEquality() {
      UniversityRegistry.UniversityConfig a =
          new UniversityRegistry.UniversityConfig("Uni", "https://url.it");
      UniversityRegistry.UniversityConfig b =
          new UniversityRegistry.UniversityConfig("Uni", "https://url.it");

      assertThat(a).isEqualTo(b);
    }
  }
}