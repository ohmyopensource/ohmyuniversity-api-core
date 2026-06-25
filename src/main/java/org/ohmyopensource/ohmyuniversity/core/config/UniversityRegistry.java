package org.ohmyopensource.ohmyuniversity.core.config;

import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Central registry of universities supported by OhMyUniversity.
 *
 * <p>This component is populated via Spring Boot configuration properties
 * (prefix: "omu.universities") and provides a runtime lookup mechanism for university-specific
 * ESSE3 configurations.
 *
 * <p>Each university is identified by a short code (e.g. "UNIMOL") and maps to:
 * - human-readable name
 * - Cineca ESSE3 base API URL
 * - Moodle URL
 * - Library portal URL
 * - ESSE3 student portal URL
 *
 * <p>This registry acts as the single source of truth for resolving external university endpoints.
 */
@Component
@ConfigurationProperties(prefix = "omu")
public class UniversityRegistry {

  private Map<String, UniversityConfig> universities = Map.of();

  public void setUniversities(Map<String, UniversityConfig> universities) {
    this.universities = universities;
  }

  // ============ Class Methods ============

  /**
   * Resolves a university configuration by its identifier.
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
   * @param name            human-readable university name
   * @param baseUrl         Cineca ESSE3 REST API base URL
   * @param moodleUrl       URL of the university Moodle instance
   * @param libraryUrl      URL of the university library portal
   * @param esse3PortalUrl  URL of the ESSE3 student portal
   */
  public record UniversityConfig(
      String name,
      String baseUrl,
      String moodleUrl,
      String libraryUrl,
      String esse3PortalUrl) {
  }
}