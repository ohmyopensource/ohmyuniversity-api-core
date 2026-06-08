package org.ohmyopensource.ohmyuniversity.core.dto;

/**
 * Response DTO for GET /api/v1/university/external-services.
 *
 * <p>Contains the URLs of external university services that the client
 * renders as embedded WebViews — Moodle for course materials and the
 * university library portal.
 *
 * <p>Both URLs are university-specific and are sourced from
 * {@code omu.universities} configuration in application.yaml.
 */
public class UniversityConfigResponse {

  /** Short university identifier (e.g. "UNIMOL"). */
  private String universityId;

  /** Human-readable university name. */
  private String name;

  /**
   * URL of the university Moodle instance.
   * The client opens this in an embedded WebView — no external browser required.
   */
  private String moodleUrl;

  /**
   * URL of the university library portal.
   * The client opens this in an embedded WebView — no external browser required.
   */
  private String libraryUrl;

  public String getUniversityId() { return universityId; }
  public void setUniversityId(String universityId) { this.universityId = universityId; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getMoodleUrl() { return moodleUrl; }
  public void setMoodleUrl(String moodleUrl) { this.moodleUrl = moodleUrl; }

  public String getLibraryUrl() { return libraryUrl; }
  public void setLibraryUrl(String libraryUrl) { this.libraryUrl = libraryUrl; }
}