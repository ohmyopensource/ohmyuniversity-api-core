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
 * <p>No Spring context — the registry is instantiated directly and populated
 * via {@link UniversityRegistry#setUniversities(Map)} to keep tests fast
 * and independent of application configuration.
 */
class UniversityRegistryTest {

  private UniversityRegistry registry;

  /** Shared UNIMOL configuration reused across multiple test cases. */
  private static final UniversityRegistry.UniversityConfig UNIMOL_CONFIG =
      new UniversityRegistry.UniversityConfig(
          "Università degli Studi del Molise",
          "https://unimol.esse3.cineca.it/e3rest/api",
          "https://moodle.unimol.it",
          "https://biblioteche.unimol.it");

  /**
   * Initialises a fresh {@link UniversityRegistry} populated with a single
   * known entry before each test to guarantee isolation.
   */
  @BeforeEach
  void setUp() {
    registry = new UniversityRegistry();
    registry.setUniversities(Map.of("UNIMOL", UNIMOL_CONFIG));
  }

  /**
   * Verifies the lookup behaviour of {@link UniversityRegistry#resolve(String)}
   * across known, unknown, null, and mixed-case identifiers.
   */
  @Nested
  @DisplayName("resolve()")
  class Resolve {

    /**
     * Verifies that resolving a registered university identifier returns
     * a non-empty {@link Optional} containing the correct name and base URL.
     */
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

    /**
     * Verifies that resolving an unregistered university identifier returns
     * an empty {@link Optional}.
     */
    @Test
    @DisplayName("unknown universityId → empty Optional")
    void unknownId() {
      assertThat(registry.resolve("UNICAM")).isEmpty();
    }

    /**
     * Verifies that passing {@code null} as the university identifier returns
     * an empty {@link Optional} without throwing a {@link NullPointerException}.
     */
    @Test
    @DisplayName("null universityId → empty Optional, no NullPointerException")
    void nullId() {
      assertThat(registry.resolve(null)).isEmpty();
    }

    /**
     * Verifies that university identifiers are resolved case-insensitively,
     * accepting both fully lowercase and mixed-case variants.
     */
    @Test
    @DisplayName("lowercase universityId → resolved case-insensitively")
    void caseInsensitive() {
      assertThat(registry.resolve("unimol")).isPresent();
      assertThat(registry.resolve("Unimol")).isPresent();
    }
  }

  /**
   * Verifies the structural contract of the {@link UniversityRegistry.UniversityConfig} record,
   * including accessor correctness and value-based equality.
   */
  @Nested
  @DisplayName("UniversityConfig record")
  class UniversityConfigTests {

    /**
     * Verifies that {@link UniversityRegistry.UniversityConfig#name()} and
     * {@link UniversityRegistry.UniversityConfig#baseUrl()} return the values
     * supplied at construction time.
     */
    @Test
    @DisplayName("name() and baseUrl() return correct values")
    void recordAccessors() {
      UniversityRegistry.UniversityConfig config =
          new UniversityRegistry.UniversityConfig(
              "Test Uni",
              "https://test.esse3.it/api",
              null,
              null);

      assertThat(config.name()).isEqualTo("Test Uni");
      assertThat(config.baseUrl()).isEqualTo("https://test.esse3.it/api");
    }

    /**
     * Verifies that two {@link UniversityRegistry.UniversityConfig} instances
     * constructed with identical values are considered equal, as expected
     * from a Java record.
     */
    @Test
    @DisplayName("two configs with same values are equal")
    void recordEquality() {
      UniversityRegistry.UniversityConfig a =
          new UniversityRegistry.UniversityConfig("Uni",
              "https://url.it",
              null,
              null);
      UniversityRegistry.UniversityConfig b =
          new UniversityRegistry.UniversityConfig("Uni",
              "https://url.it",
              null,
              null);

      assertThat(a).isEqualTo(b);
    }
  }
}