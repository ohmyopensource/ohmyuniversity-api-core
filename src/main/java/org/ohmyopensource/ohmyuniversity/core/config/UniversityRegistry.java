package org.ohmyopensource.ohmyuniversity.core.config;

import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Central registry of universities supported by OhMyUniversity.
 *
 * This component is populated via Spring Boot configuration properties
 * (prefix: "omu.universities") and provides a runtime lookup mechanism
 * for university-specific ESSE3 configurations.
 *
 * Each university is identified by a short code (e.g. "UNIMOL") and maps to:
 * - human-readable name
 * - Cineca ESSE3 base API URL
 *
 * This registry acts as the single source of truth for resolving
 * external university endpoints.
 */
@Component
@ConfigurationProperties(prefix = "omu")
public class UniversityRegistry {

  /**
   * Internal map of configured universities indexed by uppercase identifier.
   */
  private Map<String, UniversityConfig> universities = Map.of();

  /**
   * Injects university configuration map from application properties.
   *
   * @param universities map of university configurations
   */
  public void setUniversities(Map<String, UniversityConfig> universities) {
    this.universities = universities;
  }

  // ============ Class Methods ============

  /**
   * Resolves a university configuration by its identifier.
   *
   * Lookup is case-insensitive and returns an empty result if:
   * - universityId is null
   * - university is not registered in configuration
   *
   * @param universityId university short code (e.g. UNIMOL)
   * @return optional university configuration
   */
  public Optional<UniversityConfig> resolve(String universityId) {
    if (universityId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(universities.get(universityId.toUpperCase()));
  }

  /**
   * Immutable configuration model for a single university.
   *
   * @param name human-readable university name
   * @param baseUrl ESSE3 REST API base URL
   */
  public record UniversityConfig(String name, String baseUrl) {
  }
}