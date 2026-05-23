package org.ohmyopensource.ohmyuniversity.core.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for POST /api/auth/login.
 *
 * The student provides their Cineca credentials for a specific university.
 * We proxy the authentication to the university's ESSE3 instance.
 * Credentials are never stored — only the resulting session is cached in Redis.
 */
public class LoginRequest {

  /**
   * Short university identifier, e.g. "UNIMOL".
   * Must match a registered university in our system.
   */
  @NotBlank
  private String universityId;

  /** Cineca username, e.g. "a.delmuto". */
  @NotBlank
  private String username;

  /** Cineca password. Transmitted over HTTPS only, never logged or stored. */
  @NotBlank
  private String password;

  public String getUniversityId() { return universityId; }
  public void setUniversityId(String universityId) { this.universityId = universityId; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
}