package org.ohmyopensource.ohmyuniversity.core.config;

import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Registry of universities supported by OhMyUniversity.
 *
 * Each university has a short identifier (e.g. "UNIMOL"), a display name,
 * and the base URL of its ESSE3 REST API instance.
 */
@Component
@ConfigurationProperties(prefix = "omu")
public class UniversityRegistry {

  private Map<String, UniversityConfig> universities = Map.of();

  public void setUniversities(Map<String, UniversityConfig> universities) {
    this.universities = universities;
  }

  /**
   * Resolves a university configuration by its short identifier.
   *
   * @param universityId case-insensitive short university ID, e.g. "UNIMOL"
   * @return the university config if found, empty otherwise
   */
  public Optional<UniversityConfig> resolve(String universityId) {
    if (universityId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(universities.get(universityId.toUpperCase()));
  }

  /**
   * Immutable configuration record for a single university.
   */
  public record UniversityConfig(String name, String baseUrl) {
  }
}